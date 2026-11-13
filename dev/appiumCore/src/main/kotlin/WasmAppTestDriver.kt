/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.ArtifactCollectionError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrElse
import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.interactions.WheelInput.ScrollOrigin
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration

private const val EDITOR_INPUT_SELECTOR = "[data-appium-input='true']:not([aria-label='search query'])"

/** Drives Compose/Wasm through its transparent Appium-only HTML semantics overlay. */
class WasmAppTestDriver(
    private val browser: WebDriver,
    private val artifactDirectory: File,
    private val downloadDirectory: File,
) : AppTestDriver {
    private val wait get() = WebDriverWait(browser, Duration.ofSeconds(UI_WAIT_SECONDS))

    internal fun clickExternalLink(text: String) {
        clickWithSnapshot(waitAfterClick = false) { findOnScreen(textLocator(text)) }
    }

    internal fun observeExternalUris() {
        (browser as JavascriptExecutor).executeScript(
            """
            window.appiumExternalUris = [];
            const open = window.open;
            window.open = function(url, ...args) {
              window.appiumExternalUris.push(String(url));
              return open.call(this, url, ...args);
            };
            """.trimIndent(),
        )
    }

    internal fun assertExternalUri(uri: String) {
        wait.until {
            (browser as JavascriptExecutor).executeScript(
                "return window.appiumExternalUris.includes(arguments[0]);",
                uri,
            ) == true
        }
    }

    internal fun assertDownloadedBytes(name: String, expected: ByteArray) {
        val file = downloadDirectory.resolve(name)
        try {
            wait.until { file.isFile && file.length() == expected.size.toLong() }
        } catch (failure: org.openqa.selenium.TimeoutException) {
            val state =
                downloadDirectory.listFiles().orEmpty().sortedBy(File::getName)
                    .joinToString("\n") { "${it.name}\t${it.length()}" }
            artifactDirectory.resolve("download-state.txt").writeText(state.ifEmpty { "No downloaded files" })
            throw failure
        }
        kotlin.test.assertContentEquals(expected, file.readBytes())
        file.copyTo(artifactDirectory.resolve("downloaded-$name"))
    }

    internal fun assertPlaybackProgress() {
        wait.until {
            browser.findElements(By.cssSelector("[data-appium-text]")).any { element ->
                Regex("^(\\d+) / (\\d+) s$").matchEntire(element.getAttribute("data-appium-text").orEmpty())
                    ?.let { it.groupValues[1].toInt() > 0 && it.groupValues[2].toInt() > 0 } == true
            }
        }
    }

    internal fun assertPdfPreview(expected: ByteArray) {
        val frame =
            wait.until {
                browser.findElements(By.cssSelector("iframe[title='PDF document'][data-pdf-loaded='true']"))
                    .firstOrNull { it.isDisplayed && it.rect.width > 0 && it.rect.height > 0 }
            }
        val javascript = browser as JavascriptExecutor
        kotlin.test.assertEquals(true, javascript.executeScript("return navigator.pdfViewerEnabled;"))
        val source = requireNotNull(frame.getAttribute("src"))
        kotlin.test.assertEquals(
            true,
            javascript.executeScript("return arguments[0].startsWith('blob:' + location.origin + '/');", source),
        )
        val document =
            javascript.executeAsyncScript(
                """
                const done = arguments[arguments.length - 1];
                fetch(arguments[0]).then(response => {
                  if (!response.ok) throw new Error('PDF document is unavailable');
                  return response.arrayBuffer();
                }).then(buffer => done({bytes: Array.from(new Uint8Array(buffer))}))
                  .catch(error => done({error: String(error)}));
                """.trimIndent(),
                source,
            )
        val response = kotlin.test.assertIs<Map<*, *>>(document)
        kotlin.test.assertNull(response["error"])
        val values = kotlin.test.assertIs<List<*>>(response["bytes"])
        val bytes = values.map { kotlin.test.assertIs<Number>(it).toByte() }.toByteArray()
        kotlin.test.assertContentEquals(expected, bytes)
        val fillsPage =
            javascript.executeScript(
                """
                const rect = arguments[0].getBoundingClientRect();
                return rect.width >= innerWidth * 0.8 && rect.height >= innerHeight * 0.65;
                """.trimIndent(),
                frame,
            )
        kotlin.test.assertEquals(true, fillsPage, "PDF must occupy the full-page preview")
    }

    internal fun awaitDescriptionRemoved(description: String) {
        wait.until { browser.findElements(descriptionLocator(description)).isEmpty() }
    }

    internal fun awaitAuthenticatedSession() {
        WebDriverWait(browser, Duration.ofSeconds(30)).until {
            (browser as JavascriptExecutor).executeScript(
                "return window.localStorage.getItem('appium.session_ready') === 'true';",
            ) == true
        }
    }

    internal fun rtcProbe() = WasmRtcProbe(browser)
    internal fun searchProbe() = WasmSearchProbe(browser, artifactDirectory)
    internal fun uploadGate() = WasmUploadGate(browser)

    internal fun openContextMenuByText(text: String) {
        Actions(browser).contextClick(findOnScreen(textLocator(text))).perform()
    }

    internal fun assertNotVisibleByTextContaining(text: String) =
        assertNotVisible(By.cssSelector("[data-appium-text*='${text.cssAttributeValue()}']"), 5)

    internal fun prepareUploadPicker() {
        (browser as JavascriptExecutor).executeScript(
            """
            const click = HTMLInputElement.prototype.click;
            HTMLInputElement.prototype.click = function() {
                if (this.type !== 'file') return click.call(this);
                HTMLInputElement.prototype.click = click;
                // WebDriver supplies the selected file to FileKit's real input and change handler.
            };
            """.trimIndent(),
        )
    }

    internal fun selectUploadFile(name: String, bytes: ByteArray) {
        require(name.matches(Regex("[a-zA-Z0-9._-]+")))
        val file = artifactDirectory.resolve("input").also(File::mkdirs).resolve(name)
        file.writeBytes(bytes)
        val picker = wait.until { browser.findElements(By.cssSelector("input[type='file']")).singleOrNull() }
        picker.sendKeys(file.canonicalPath)
    }

    internal fun scrollToDescription(description: String, upward: Boolean = false) =
        scrollToLocator(descriptionLocator(description), upward)

    internal fun scrollToText(text: String) = scrollToLocator(textLocator(text), upward = false)

    internal fun assertInputByDescription(description: String, expected: String) {
        wait.until { findOnScreen(descriptionLocator(description)).getDomProperty("value") == expected }
    }

    private fun scrollToLocator(locator: By, upward: Boolean) {
        val viewport =
            requireNotNull(
                (browser as JavascriptExecutor).executeScript(
                    "return [window.innerWidth, window.innerHeight];",
                ),
            ) as List<*>
        val x = (requireNotNull(viewport[0]) as Number).toInt() / 2
        val y = (requireNotNull(viewport[1]) as Number).toInt() / 2
        repeat(12) {
            if (browser.findElements(locator).any(::isOnScreen)) return
            val script = browser as JavascriptExecutor
            val before =
                script.executeScript(
                    """
                    if (!window.appiumWheelProbe) {
                      window.appiumWheelProbe = {count: 0, events: []};
                      document.addEventListener('wheel', event => {
                        if (!event.isTrusted) return;
                        const probe = window.appiumWheelProbe;
                        probe.count++;
                        probe.events.push({target: event.target.nodeName, deltaY: event.deltaY,
                          x: event.clientX, y: event.clientY});
                        if (probe.events.length > 100) probe.events.shift();
                      }, {capture: true, passive: true});
                    }
                    document.getElementById('appium-html-semantics').style.visibility = 'hidden';
                    return window.appiumWheelProbe.count;
                    """.trimIndent(),
                )
            try {
                // Commit hit-test changes before Firefox's asynchronous native wheel dispatch.
                waitForUiCommit()
                Actions(browser)
                    .scrollFromOrigin(ScrollOrigin.fromViewport(x, y), 0, if (upward) -400 else 400).perform()
                wait.until {
                    script.executeScript("return window.appiumWheelProbe.count > arguments[0];", before) == true
                }
                waitForUiCommit()
            } finally {
                script.executeScript("document.getElementById('appium-html-semantics').style.visibility = '';")
            }
            waitForUiCommit()
        }
        check(browser.findElements(locator).any(::isOnScreen)) {
            saveFailureSnapshot("scroll")
            "Control was not reachable by scrolling: $locator"
        }
    }

    fun resizeViewport(width: Int, height: Int) {
        // Set width before measuring responsive browser chrome for the exact content viewport.
        browser.manage().window().size = Dimension(width, height)
        waitForUiCommit()
        val frame =
            requireNotNull(
                (browser as JavascriptExecutor).executeScript(
                    "return [window.outerWidth - window.innerWidth, window.outerHeight - window.innerHeight];",
                ),
            ) as List<*>
        browser.manage().window().size =
            Dimension(
                width + (requireNotNull(frame[0]) as Number).toInt(),
                height + (requireNotNull(frame[1]) as Number).toInt(),
            )
        waitForUiCommit()
        val actual =
            browser.executeScript(
                "return [window.innerWidth, window.innerHeight, window.outerWidth, window.outerHeight];",
            )
        check((requireNotNull(actual) as List<*>).take(2) == listOf(width.toLong(), height.toLong())) {
            saveFailureSnapshot("resize")
            "Expected viewport ${width}x$height; actual=$actual; initial frame=$frame"
        }
    }

    fun assertInputText(expected: String) {
        wait.until {
            (browser as JavascriptExecutor).executeScript(
                "const inputs = document.querySelectorAll(arguments[1]); " +
                    "return inputs.length === 1 && inputs[0].value === arguments[0];",
                expected,
                EDITOR_INPUT_SELECTOR,
            ) == true && inputLengthAttributeMatchesValue("data-appium-input-compose-length")
        }
    }

    fun assertUnreadRoomsIndicator(visible: Boolean) {
        val locator = descriptionLocator("Rooms, Unread messages")
        if (visible) assertVisible(locator) else assertNotVisible(locator, 5)
    }

    internal fun socketProbe() = WasmSocketProbe(browser)

    override suspend fun clickByDescription(description: String) = click(descriptionLocator(description))

    override suspend fun clickByText(text: String) = clickText(textLocator(text))

    override suspend fun clickByTextContaining(text: String) =
        clickText(
            By.cssSelector("[data-appium-text*='${text.cssAttributeValue()}']"),
        )

    override suspend fun inputText(text: String) = inputTextAt(By.cssSelector(EDITOR_INPUT_SELECTOR), text)

    internal fun inputTextByDescription(description: String, text: String) =
        inputTextAt(
            By.cssSelector("[data-appium-input='true'][aria-label='${description.cssAttributeValue()}']"),
            text,
        )

    internal fun submitInputByDescription(description: String) {
        findOnScreen(By.cssSelector("[data-appium-input='true'][aria-label='${description.cssAttributeValue()}']"))
            .sendKeys(Keys.ENTER)
    }

    private fun inputTextAt(locator: By, text: String) {
        try {
            val expectedValue = text.replace("\r\n", "\n").replace('\r', '\n')
            val input = wait.until { browser.findElements(locator).filter(::isOnScreen).singleOrNull() }
            (browser as JavascriptExecutor).executeScript(
                """
                arguments[0].value = arguments[1];
                arguments[0].dispatchEvent(new Event('input', { bubbles: true }));
                """.trimIndent(),
                input,
                expectedValue,
            )
            wait.until { inputLengthAttributeMatchesValue("data-appium-input-delivered-length", input) }
            wait.until { inputLengthAttributeMatchesValue("data-appium-input-compose-length", input) }
            val actualValue =
                (browser as JavascriptExecutor).executeScript(
                    "return arguments[0].value;",
                    input,
                )?.toString().orEmpty()
            check(actualValue == expectedValue) { "Wasm HTML input changed the supplied text" }
        } catch (throwable: Throwable) {
            captureFailure(ArtifactCollectionError::CollectionFailed) { saveFailureSnapshot("input") }
            throw throwable
        }
    }

    override suspend fun assertVisibleByDescription(description: String) =
        assertVisible(
            descriptionLocator(description),
        )

    override suspend fun assertNotVisibleByDescription(description: String, timeoutSeconds: Long) =
        assertNotVisible(descriptionLocator(description), timeoutSeconds)

    override suspend fun assertVisibleByText(text: String) = assertVisible(textLocator(text))

    override suspend fun assertVisibleByTextContaining(text: String) {
        assertVisible(
            By.cssSelector("[data-appium-text*='${text.cssAttributeValue()}']"),
        )
    }

    override suspend fun assertNotVisibleByText(text: String, timeoutSeconds: Long) =
        assertNotVisible(textLocator(text), timeoutSeconds)

    override suspend fun dismissDialog() {
        pressEscape()
        wait.until { browser.findElements(descriptionLocator("dialog")).none(::isOnScreen) }
    }

    /** Sends the browser client's ordinary Escape navigation action to the focused renderer. */
    fun pressEscape() {
        focusComposeCanvas()
        Actions(browser).sendKeys(Keys.ESCAPE).perform()
        waitForUiCommit()
    }

    private fun focusComposeCanvas() {
        // Bridge callbacks do not focus the renderer as a physical canvas click would.
        check(
            (browser as JavascriptExecutor).executeScript(
                """
                const canvases = [];
                const collect = root => {
                  for (const element of root.querySelectorAll('*')) {
                    if (element.tagName === 'CANVAS') canvases.push(element);
                    if (element.shadowRoot) collect(element.shadowRoot);
                  }
                };
                collect(document);
                const canvas = canvases.reverse().find(element => {
                  const rect = element.getBoundingClientRect();
                  return rect.width > 0 && rect.height > 0 && rect.right > 0 && rect.bottom > 0 &&
                    rect.left < window.innerWidth && rect.top < window.innerHeight;
                });
                if (!canvas) return false;
                canvas.focus({preventScroll: true});
                return canvas.getRootNode().activeElement === canvas;
                """.trimIndent(),
            ) == true,
        ) { "Compose canvas did not receive keyboard focus" }
    }

    override suspend fun navigateBack() {
        if (browser.findElements(descriptionLocator("dialog")).any(::isOnScreen)) {
            dismissDialog()
        } else {
            click(descriptionLocator("back"))
        }
    }

    override suspend fun saveSnapshot(name: String) {
        val outputDir = artifactDirectory.also(File::mkdirs)
        val safeName = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        File(
            outputDir,
            "$safeName.html",
        ).writeText(requireNotNull(browser.pageSource) { "Browser page source is unavailable" })
        saveBrowserDiagnostics(File(outputDir, "$safeName.json"))
        (browser as? TakesScreenshot)?.getScreenshotAs(OutputType.FILE)
            ?.copyTo(File(outputDir, "$safeName.png"), overwrite = true)
    }

    private fun inputLengthAttributeMatchesValue(attribute: String, input: WebElement? = null): Boolean =
        (browser as JavascriptExecutor).executeScript(
            """
            const inputs = document.querySelectorAll(arguments[2]);
            const input = arguments[1] || (inputs.length === 1 ? inputs[0] : null);
            return input !== null && input.getAttribute(arguments[0]) === String(input.value.length);
            """.trimIndent(),
            attribute,
            input,
            EDITOR_INPUT_SELECTOR,
        ) == true

    private fun click(locator: By) {
        clickWithSnapshot {
            findOnScreen(locator)
        }
    }

    private fun clickText(locator: By) {
        clickWithSnapshot(textTarget = true) { findOnScreen(locator) }
    }

    private fun textClickPoint(element: WebElement): org.openqa.selenium.Point {
        val data =
            (browser as JavascriptExecutor).executeScript(
                """
                const target = arguments[0];
                const parent = target.getBoundingClientRect();
                const children = Array.from(document.querySelectorAll('#appium-html-semantics > [data-appium-semantics]'))
                  .filter(node => node !== target).map(node => node.getBoundingClientRect())
                  .filter(rect => rect.width > 0 && rect.height > 0 && rect.left >= parent.left && rect.top >= parent.top &&
                    rect.right <= parent.right && rect.bottom <= parent.bottom)
                  .map(rect => [rect.left, rect.top, rect.width, rect.height].map(Math.round));
                return [[parent.left, parent.top, parent.width, parent.height].map(Math.round), ...children];
                """.trimIndent(),
                element,
            )
        val rectangles =
            kotlin.test.assertIs<List<*>>(data).map { item ->
                val values = kotlin.test.assertIs<List<*>>(item).map { kotlin.test.assertIs<Number>(it).toInt() }
                org.openqa.selenium.Rectangle(values[0], values[1], values[3], values[2])
            }
        return desktopTextClickPoint(rectangles.first(), rectangles.drop(1))
    }

    private fun clickWithSnapshot(
        textTarget: Boolean = false,
        waitAfterClick: Boolean = true,
        findElement: () -> WebElement,
    ) {
        try {
            waitForUiCommit()
            val element = findElement()
            // A trusted pointer event preserves browser user activation for Compose effects.
            if (textTarget) {
                val point = textClickPoint(element)
                Actions(browser).moveToLocation(point.x, point.y).click().perform()
            } else {
                Actions(browser).moveToElement(element).click().perform()
            }
            if (waitAfterClick) waitForUiCommit()
        } catch (throwable: Throwable) {
            captureFailure(ArtifactCollectionError::CollectionFailed) { saveFailureSnapshot("click") }
            throw throwable
        }
    }

    private fun waitForUiCommit() {
        (browser as JavascriptExecutor).executeAsyncScript(
            """
            const done = arguments[arguments.length - 1];
            const startedAt = performance.now();
            let previousSignature = '';
            let stableFrames = 0;
            const check = () => {
              const signature = Array.from(document.querySelectorAll('#appium-html-semantics > [data-appium-semantics]'))
                .map(element => [element.getAttribute('data-appium-semantics'), element.getAttribute('data-appium-text'),
                  element.getAttribute('aria-label'), Math.round(element.getBoundingClientRect().left),
                  Math.round(element.getBoundingClientRect().top), Math.round(element.getBoundingClientRect().width),
                  Math.round(element.getBoundingClientRect().height)].join(':')).join('|');
              stableFrames = signature === previousSignature ? stableFrames + 1 : 0;
              previousSignature = signature;
              if ((stableFrames >= 4 && performance.now() - startedAt >= 100) || performance.now() - startedAt >= 1500) done();
              else window.requestAnimationFrame(check);
            };
            window.requestAnimationFrame(check);
            """.trimIndent(),
        )
    }

    private fun assertVisible(locator: By) {
        try {
            findOnScreen(locator)
        } catch (throwable: Throwable) {
            captureFailure(ArtifactCollectionError::CollectionFailed) { saveFailureSnapshot("assert-visible") }
            throw throwable
        }
    }

    private fun assertNotVisible(locator: By, timeoutSeconds: Long) {
        try {
            WebDriverWait(browser, Duration.ofSeconds(timeoutSeconds)).until {
                browser.findElements(locator).none(::isOnScreen)
            }
        } catch (throwable: Throwable) {
            captureFailure(ArtifactCollectionError::CollectionFailed) { saveFailureSnapshot("assert-not-visible") }
            throw throwable
        }
    }

    private fun findOnScreen(locator: By): WebElement =
        wait.until {
            // Overlay nodes share a z-index; later dialog nodes are painted above page controls.
            browser.findElements(locator).lastOrNull(::isOnScreen)
        }

    private fun isOnScreen(element: WebElement): Boolean =
        captureFailure(ArtifactCollectionError::CollectionFailed) {
            (browser as JavascriptExecutor).executeScript(
                """
                const rect = arguments[0].getBoundingClientRect();
                const centerX = rect.left + rect.width / 2;
                const centerY = rect.top + rect.height / 2;
                const visible = rect.width > 0 && rect.height > 0 && centerX >= 0 && centerX <= window.innerWidth &&
                  centerY >= 0 && centerY <= window.innerHeight;
                if (!visible) return false;
                const node = arguments[0];
                if (!node.matches("[data-appium-action='true'],[data-appium-input='true']")) return true;
                const hit = document.elementFromPoint(centerX, centerY);
                return hit === node || node.contains(hit);
                """.trimIndent(),
                element,
            ) == true
        }.getOrElse { false }

    private fun descriptionLocator(description: String) =
        By.cssSelector(
            "[aria-label='${description.cssAttributeValue()}']",
        )

    private fun textLocator(text: String) = By.cssSelector("[data-appium-text='${text.cssAttributeValue()}']")

    private fun saveFailureSnapshot(prefix: String) {
        val outputDir = artifactDirectory.also(File::mkdirs)
        val name = prefix
        captureFailure(ArtifactCollectionError::CollectionFailed) {
            File(
                outputDir,
                "$name.html",
            ).writeText(requireNotNull(browser.pageSource) { "Browser page source is unavailable" })
            saveBrowserDiagnostics(File(outputDir, "$name.json"))

            (browser as? TakesScreenshot)?.getScreenshotAs(OutputType.FILE)
                ?.copyTo(File(outputDir, "$name.png"), overwrite = true)
        }
    }

    private fun saveBrowserDiagnostics(file: File) {
        val diagnostics =
            (browser as JavascriptExecutor).executeScript(
                """
                return JSON.stringify({http: window.appiumHttpProbe, workers: window.appiumWorkerProbe,
                  wheel: window.appiumWheelProbe, errors: window.appiumErrors || []});
                """.trimIndent(),
            )
        file.writeText(diagnostics?.toString().orEmpty())
    }

    private fun String.cssAttributeValue(): String = replace("\\", "\\\\").replace("'", "\\'")

    private companion object {
        const val UI_WAIT_SECONDS = 15L
    }
}
