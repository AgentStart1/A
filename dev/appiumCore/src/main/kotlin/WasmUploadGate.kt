/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import kotlin.test.assertEquals

/** Holds the second real chunk request, preserving AbortSignal and the original fetch implementation. */
internal class WasmUploadGate(private val browser: WebDriver) {
    private val script get() = browser as JavascriptExecutor

    fun install() {
        script.executeScript(
            """
            const fetch = window.fetch.bind(window);
            window.appiumUploadGate = {held: true, blocked: false, release: null, record: null, sent: []};
            window.fetch = (...args) => {
              const url = new URL(typeof args[0] === 'string' ? args[0] : args[0].url, location.href);
              const match = url.pathname.match(/\/files\/chunk\/(\d+)\/(\d+)\/upload$/);
              if (!match) return fetch(...args);
              const gate = window.appiumUploadGate;
              const signal = args[1]?.signal || args[0]?.signal;
              gate.record = match[1];
              const transmit = () => {
                if (signal?.aborted) {
                  return window.Promise.reject(new window.DOMException('Upload cancelled', 'AbortError'));
                }
                gate.sent.push(Number(match[2]));
                return fetch(...args);
              };
              if (match[2] === '1' && gate.held) {
                gate.blocked = true;
                // Use the page constructor rather than a WebDriver sandbox Promise.
                return new window.Promise((resolve, reject) => {
                  const abort = () => reject(new window.DOMException('Upload cancelled', 'AbortError'));
                  if (signal?.aborted) { abort(); return; }
                  signal?.addEventListener('abort', abort, {once: true});
                  gate.release = () => { signal?.removeEventListener('abort', abort); resolve(); };
                }).then(transmit);
              }
              return transmit();
            };
            """.trimIndent(),
        )
    }

    fun awaitSecondChunk(): Long {
        WebDriverWait(browser, Duration.ofSeconds(30)).until {
            script.executeScript("return window.appiumUploadGate.blocked;") == true
        }
        return (requireNotNull(script.executeScript("return window.appiumUploadGate.record;")) as String).toLong()
    }

    fun release() {
        script.executeScript("window.appiumUploadGate.held = false; window.appiumUploadGate.release?.();")
    }

    fun assertEachChunkSentOnce() {
        assertEquals(listOf(0L, 1L), script.executeScript("return window.appiumUploadGate.sent;"))
    }
}
