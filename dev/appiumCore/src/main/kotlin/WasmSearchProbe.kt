/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.ArtifactCollectionError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.onFailure
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration
import kotlin.test.assertEquals

/** Observes real search responses without changing their data or server paging tokens. */
internal class WasmSearchProbe(private val browser: WebDriver, private val artifacts: File) {
    private val script get() = browser as JavascriptExecutor
    private val wait get() = WebDriverWait(browser, Duration.ofSeconds(30))

    fun observe() {
        script.executeScript(
            """
            const fetch = window.fetch.bind(window);
            window.appiumSearchPages = [];
            // Preserve the page Promise realm; WebDriver's async functions can belong to its sandbox.
            window.fetch = (...args) => fetch(...args).then(response => {
              const url = new URL(typeof args[0] === 'string' ? args[0] : args[0].url, location.href);
              if (url.pathname.endsWith('/users/joined-rooms/search') && response.ok) {
                return response.clone().json().then(page => {
                  window.appiumSearchPages.push({query: url.searchParams.get('word'),
                    input: url.searchParams.get('nextPageToken'), next: page.pagination?.nextPageToken,
                    names: page.data.map(room => room.name)});
                  return response;
                });
              }
              return response;
            });
            """.trimIndent(),
        )
    }

    fun awaitContinuation(query: String) {
        wait.until {
            script.executeScript(
                """
                const pages = window.appiumSearchPages.filter(page => page.query === arguments[0]);
                return pages.some((page, i) => page.input && pages.slice(0, i)
                  .some(previous => previous.next === page.input));
                """.trimIndent(),
                query,
            ) == true
        }
    }

    fun assertQueryResult(query: String, expected: List<String>) {
        val names =
            wait.until {
                script.executeScript(
                    "return window.appiumSearchPages.find(page => page.query === arguments[0])?.names || null;",
                    query,
                ) as? List<*>
            }
        assertEquals(expected, names)
    }

    fun pageCount(): Long =
        (requireNotNull(script.executeScript("return window.appiumSearchPages.length;")) as Number).toLong()

    fun awaitAdditionalPage(before: Long) {
        wait.until { pageCount() > before }
    }

    fun assertVisibleOrder(expected: List<String>) {
        try {
            wait.until {
                val visible = visibleRows()
                visible.isNotEmpty() &&
                    visible.all(
                        expected::contains,
                    ) && visible == visible.sortedBy(expected::indexOf)
            }
        } catch (failure: TimeoutException) {
            captureFailure(ArtifactCollectionError::CollectionFailed) {
                val snapshot =
                    requireNotNull(
                        script.executeScript(
                            "return JSON.stringify({expected: arguments[0], pages: window.appiumSearchPages});",
                            expected,
                        ) as? String,
                    ) { "Search probe snapshot is unavailable" }
                artifacts.mkdirs()
                File(artifacts, "search-order.json").writeText(snapshot)
            }.onFailure { failure.addSuppressed(it.cause) }
            throw failure
        }
        val visible = visibleRows()
        assertEquals(visible.sortedBy(expected::indexOf), visible, "UI must preserve the server result order")
    }

    fun firstVisibleName(): String = visibleRows().first()

    fun rowTop(name: String): Double {
        val top =
            requireNotNull(
                script.executeScript(
                    """
                    const row = [...document.querySelectorAll('[data-appium-text]')]
                      .filter(node => node.getAttribute('data-appium-text') === arguments[0]).at(-1);
                    return row?.getBoundingClientRect().top;
                    """.trimIndent(),
                    name,
                ),
            ) { "Search room is unavailable: $name" }
        return (top as Number).toDouble()
    }

    private fun visibleRows(): List<String> {
        val rows =
            requireNotNull(
                script.executeScript(
                    """
                    return [...document.querySelectorAll('[data-appium-text]')]
                      .filter(node => node.getAttribute('data-appium-text').startsWith('voyage '))
                      .filter(node => { const r = node.getBoundingClientRect();
                        const hit = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2);
                        return r.height > 0 && r.height + 0.5 >= Number(node.getAttribute('data-appium-layout-height')) &&
                          r.top >= 0 && r.bottom <= innerHeight && (hit === node || node.contains(hit)); })
                      .sort((a,b) => a.getBoundingClientRect().top - b.getBoundingClientRect().top)
                      .map(node => node.getAttribute('data-appium-text'));
                    """.trimIndent(),
                ),
            ) as List<*>
        return rows.map { requireNotNull(it) as String }
    }
}
