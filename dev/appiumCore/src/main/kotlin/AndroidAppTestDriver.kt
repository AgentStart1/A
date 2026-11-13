/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.ArtifactCollectionError
import com.storyteller_f.result.captureFailure
import io.appium.java_client.AppiumBy
import io.appium.java_client.Setting
import io.appium.java_client.android.AndroidDriver
import kotlinx.coroutines.delay
import org.openqa.selenium.By
import org.openqa.selenium.OutputType
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

internal fun androidFileNamePattern(name: String): String =
    "(?s)^${java.util.regex.Pattern.quote(
        name,
    )}(?:(?:, |\n).*)?$"

internal fun androidFileNameSelector(name: String): String {
    // UiAutomator2 unescapes quotes only; JSON escaping doubles regex backslashes.
    val pattern = androidFileNamePattern(name).replace("\"", "\\\"")
    return """new UiSelector().descriptionMatches("$pattern")"""
}

class AndroidAppTestDriver internal constructor(
    private val driver: AndroidDriver,
    private val report: AppiumTestReport,
    private val runAdbCommand: (List<String>) -> String,
) : AppTestDriver {
    /** Requires a compact API 34+ device configured for gesture navigation. */
    fun assertPredictiveBackCancellation(fromRight: Boolean) {
        verifyAndroidPredictiveBackCancellation(
            fromRight = fromRight,
            adb = runAdbCommand,
            screenshot = { driver.getScreenshotAs(OutputType.BYTES) },
            output = report.directory,
        )
    }

    suspend fun cancelNativeSaveDialog(appPackageName: String) {
        val pickerPackage =
            WebDriverWait(driver, Duration.ofSeconds(15)).until {
                driver.currentPackage?.takeIf { it.endsWith(".documentsui") }
            }
        val dump = runAdbCommand(listOf("shell", "dumpsys", "activity", "top"))
        assertTrue("ACTIVITY $pickerPackage/" in dump, "Expected the foreground document picker")
        val topActivity =
            dump.substringAfter("ACTIVITY $pickerPackage/")
                .substringBefore("\nACTIVITY ")
        assertTrue("android.intent.action.CREATE_DOCUMENT" in topActivity, "Expected a native save-document request")
        report.captureSnapshot(driver, "native-save-dialog")
        dismissKeyboard()
        navigateBack()
        WebDriverWait(driver, Duration.ofSeconds(15)).until { driver.currentPackage == appPackageName }
    }

    suspend fun assertAsciidocPreviewOpened(appPackageName: String, expectedSource: String) {
        assertTrue(expectedSource.isNotBlank(), "AsciiDoc source must not be blank")
        val expectedTitle =
            expectedSource.lineSequence()
                .firstOrNull { it.startsWith("= ") }
                ?.removePrefix("= ")
                ?: error("AsciiDoc source must include a document title")
        var resumedActivity = ""
        var previewPollCount = 0
        while (previewPollCount < 30) {
            resumedActivity =
                runAdbCommand(
                    listOf("shell", "dumpsys", "activity", "activities"),
                ).lineSequence()
                    .firstOrNull { "topResumedActivity=" in it }
                    .orEmpty()
            if (resumedActivity.contains(appPackageName)) {
                break
            }
            previewPollCount += 1
            delay(500.milliseconds)
        }
        assertTrue(
            resumedActivity.contains(appPackageName),
            "Expected the app to remain foreground for the AsciiDoc preview, " +
                "but the resumed activity is $resumedActivity",
        )
        assertElementVisible("""new UiSelector().text("AsciiDoc preview")""")
        assertElementVisible("""new UiSelector().text("$expectedTitle")""")
    }

    suspend fun withWideViewport(block: suspend () -> Unit) {
        val size = runAdbCommand(listOf("shell", "wm", "size"))
        val density = runAdbCommand(listOf("shell", "wm", "density"))
        val originalSize =
            size.lineSequence().firstOrNull { it.startsWith("Override size:") }
                ?.substringAfter(":")?.trim() ?: "reset"
        val originalDensity =
            density.lineSequence().firstOrNull { it.startsWith("Override density:") }
                ?.substringAfter(":")?.trim() ?: "reset"
        try {
            runAdbCommand(listOf("shell", "wm", "size", "1920x1080"))
            runAdbCommand(listOf("shell", "wm", "density", "160"))
            block()
        } finally {
            runAdbCommand(listOf("shell", "wm", "size", originalSize))
            runAdbCommand(listOf("shell", "wm", "density", originalDensity))
        }
    }

    fun assertUnreadRoomsIndicator(visible: Boolean) {
        val selector = """new UiSelector().descriptionContains("Unread messages")"""
        if (visible) assertElementVisible(selector) else assertElementNotVisible(selector, 5)
    }

    internal suspend fun assertVideoPlaying(name: String) {
        assertVisibleByDescription("video playback $name: playing")
        val packageName = requireNotNull(driver.currentPackage) { "Video playback has no foreground application" }
        assertVideoFrame(packageName, name)
    }

    internal suspend fun withAnimatedMediaControls(block: suspend () -> Unit) {
        val setting = Setting.WAIT_FOR_IDLE_TIMEOUT
        val originalWait = driver.settings.getValue(setting.toString())
        val restoreWait = AutoCloseable { driver.setSetting(setting, originalWait) }
        // Media3 progress updates prevent idle while native controls and video frames remain active.
        driver.setSetting(setting, 0)
        restoreWait.use { block() }
    }

    internal fun playbackPositionSeconds(): Int {
        val progress =
            driver.findElement(
                AppiumBy.androidUIAutomator("""new UiSelector().textMatches("[0-9]+:[0-9]{2} / [0-9]+:[0-9]{2}")"""),
            ).text
        val parts = progress.substringBefore(" / ").split(':').map(String::toInt)
        return parts[0] * 60 + parts[1]
    }

    internal fun assertPlaybackAdvancesAfter(position: Int) {
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
            .ignoring(StaleElementReferenceException::class.java).until {
                playbackPositionSeconds() > position
            }
    }

    internal fun assertExpandedVideoFrame() {
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS)).until {
            val size = driver.manage().window().size
            hasVideoTestPattern(
                driver.getScreenshotAs(OutputType.BYTES),
                org.openqa.selenium.Rectangle(org.openqa.selenium.Point(0, 0), size),
            )
        }
    }

    internal fun seekPlaybackNearEnd() {
        val bounds = driver.findElement(AppiumBy.accessibilityId("Playback position")).rect
        driver.executeScript(
            "mobile: dragGesture",
            mapOf(
                "startX" to bounds.x + bounds.width / 25,
                "startY" to bounds.y + bounds.height / 2,
                "endX" to bounds.x + bounds.width * 85 / 100,
                "endY" to bounds.y + bounds.height / 2,
                "speed" to 1000,
            ),
        )
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
            .ignoring(StaleElementReferenceException::class.java).until { playbackPositionSeconds() in 48..55 }
    }

    internal fun assertPlaybackRestarted() {
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
            .ignoring(StaleElementReferenceException::class.java).until { playbackPositionSeconds() in 1..10 }
    }

    internal fun lowerPlaybackVolume(): Float {
        var previousBounds: org.openqa.selenium.Rectangle? = null
        val bounds =
            WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
                .ignoring(StaleElementReferenceException::class.java).until {
                    val slider =
                        driver.findElements(By.className("android.widget.SeekBar")).lastOrNull()
                            ?: return@until null
                    val current = slider.rect
                    val stable = slider.isDisplayed && current.width > 0 && current == previousBounds
                    previousBounds = current
                    current.takeIf { stable }
                }
        driver.executeScript(
            "mobile: dragGesture",
            mapOf(
                "startX" to bounds.x + bounds.width * 9 / 10,
                "startY" to bounds.y + bounds.height / 2,
                "endX" to bounds.x + bounds.width / 4,
                "endY" to bounds.y + bounds.height / 2,
                "speed" to 1000,
            ),
        )
        return WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
            .ignoring(StaleElementReferenceException::class.java)
            .until { playbackVolume()?.takeIf { it in 0.1f..0.4f } }
    }

    internal fun assertPlaybackVolume(expected: Float) {
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
            .ignoring(StaleElementReferenceException::class.java).until {
                playbackVolume()?.let { kotlin.math.abs(it - expected) < 0.01f } == true
            }
    }

    private fun playbackVolume(): Float? =
        driver.findElements(By.className("android.widget.SeekBar")).lastOrNull()?.text?.toFloatOrNull()

    internal suspend fun withFailedMediaResponse(bytes: ByteArray, block: suspend (() -> Unit) -> Unit) {
        val previous =
            runAdbCommand(listOf("reverse", "--list")).lineSequence()
                .map { it.trim().split(Regex("\\s+")) }
                .single { it.size == 3 && it[1] == "tcp:8822" }[2]
        val ready = kotlinx.coroutines.CompletableDeferred<Unit>()
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        val restore =
            AutoCloseable {
                try {
                    runAdbCommand(listOf("reverse", "tcp:8822", previous))
                } finally {
                    server.stop(0)
                }
            }
        restore.use {
            server.createContext("/") { exchange ->
                exchange.use {
                    if (ready.isCompleted) {
                        it.sendResponseHeaders(200, bytes.size.toLong())
                        it.responseBody.buffered().use { output -> output.write(bytes) }
                    } else {
                        it.sendResponseHeaders(503, -1)
                    }
                }
            }
            server.start()
            runAdbCommand(listOf("reverse", "tcp:8822", "tcp:${server.address.port}"))
            block { ready.complete(Unit) }
        }
    }

    internal suspend fun pauseVideo(name: String) {
        withAnimatedMediaControls {
            driver.findElements(AppiumBy.accessibilityId("Show player controls")).singleOrNull()?.click()
            clickByDescription("Pause")
        }
        assertVisibleByDescription("video playback $name: paused")
    }

    internal fun pressHomeAndAssertPip(packageName: String, expected: Boolean) {
        driver.pressKey(
            io.appium.java_client.android.nativekey.KeyEvent(io.appium.java_client.android.nativekey.AndroidKey.HOME),
        )
        WebDriverWait(driver, Duration.ofSeconds(15)).until { driver.currentPackage != packageName }
        assertPipState(packageName, expected)
    }

    internal fun assertVideoFrame(packageName: String, name: String) {
        val originalWindows = driver.settings.getValue("enableMultiWindows")
        val restoreWindows = AutoCloseable { driver.setSetting("enableMultiWindows", originalWindows) }
        driver.setSetting("enableMultiWindows", true)
        restoreWindows.use {
            WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
                .ignoring(StaleElementReferenceException::class.java)
                .until {
                    val player =
                        driver.findElements(By.xpath("//*[starts-with(@content-desc,'video playback ')]"))
                            .singleOrNull { element ->
                                element.getAttribute("package") == packageName &&
                                    element.getAttribute("content-desc") == "video playback $name: playing"
                            } ?: return@until false
                    hasVideoTestPattern(driver.getScreenshotAs(OutputType.BYTES), player.rect)
                }
        }
    }

    internal fun restoreFromPip(packageName: String) {
        driver.activateApp(packageName)
        assertPipState(packageName, false)
    }

    private fun assertPipState(packageName: String, expected: Boolean) {
        WebDriverWait(driver, Duration.ofSeconds(15)).until {
            val activities = runAdbCommand(listOf("shell", "dumpsys", "activity", "activities"))
            val stacks = activities.split(Regex("(?m)(?=^ * (?:RootTask|Stack) #)"))
            val pinned =
                stacks.any { stack ->
                    ("mode=pinned" in stack || "windowingMode=2" in stack) && packageName in stack
                }
            pinned == expected
        }
    }

    override suspend fun clickByDescription(description: String) {
        clickElement("""new UiSelector().description("$description")""")
    }

    fun clickFileByName(name: String) {
        clickElement(androidFileNameSelector(name))
    }

    fun assertFileNameVisible(name: String) {
        assertElementVisible(androidFileNameSelector(name))
    }

    override suspend fun clickByText(text: String) {
        clickElement("""new UiSelector().text("$text")""")
    }

    override suspend fun clickByTextContaining(text: String) {
        clickElement("""new UiSelector().textContains("$text")""")
    }

    override suspend fun inputText(text: String) {
        inputLastElement("""new UiSelector().className("android.widget.EditText")""", text)
    }

    override suspend fun assertVisibleByDescription(description: String) {
        assertElementVisible("""new UiSelector().description("$description")""")
    }

    override suspend fun assertNotVisibleByDescription(description: String, timeoutSeconds: Long) {
        assertElementNotVisible("""new UiSelector().description("$description")""", timeoutSeconds)
    }

    override suspend fun assertVisibleByText(text: String) {
        assertElementVisible("""new UiSelector().text("$text")""")
    }

    override suspend fun assertVisibleByTextContaining(text: String) {
        assertElementVisible("""new UiSelector().textContains("$text")""")
    }

    override suspend fun assertNotVisibleByText(text: String, timeoutSeconds: Long) {
        assertElementNotVisible("""new UiSelector().text("$text")""", timeoutSeconds)
    }

    override suspend fun refreshList() {
        val viewport = driver.manage().window().size
        driver.executeScript(
            "mobile: swipeGesture",
            mapOf(
                "left" to viewport.width / 4,
                "top" to viewport.height / 3,
                "width" to viewport.width / 2,
                "height" to viewport.height / 3,
                "direction" to "down",
                "percent" to 0.8,
            ),
        )
    }

    override suspend fun dismissKeyboard() {
        if (driver.isKeyboardShown) driver.hideKeyboard()
    }

    override suspend fun dismissDialog() = navigateBack()

    override suspend fun navigateBack() {
        driver.navigate().back()
    }

    override suspend fun saveSnapshot(name: String) {
        saveDebugSnapshot(name)
    }

    private fun assertElementVisible(selector: String) {
        try {
            val element =
                WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
                    .until(ExpectedConditions.presenceOfElementLocated(AppiumBy.androidUIAutomator(selector)))
            assertTrue(element.isDisplayed)
        } catch (throwable: Throwable) {
            captureFailure(ArtifactCollectionError::CollectionFailed) {
                saveDebugSnapshot("assert-visible-failed")
            }
            throw throwable
        }
    }

    private fun assertElementNotVisible(selector: String, seconds: Long) {
        try {
            val locator = AppiumBy.androidUIAutomator(selector)
            WebDriverWait(driver, Duration.ofSeconds(seconds)).until {
                driver.findElements(locator).isEmpty()
            }
        } catch (throwable: Throwable) {
            captureFailure(ArtifactCollectionError::CollectionFailed) {
                saveDebugSnapshot("assert-not-visible-failed")
            }
            throw throwable
        }
    }

    private fun clickElement(selector: String, seconds: Long = UI_WAIT_SECONDS) {
        try {
            val locator = AppiumBy.androidUIAutomator(selector)
            val element =
                if (seconds > 0) {
                    var previousBounds: org.openqa.selenium.Rectangle? = null
                    WebDriverWait(driver, Duration.ofSeconds(seconds))
                        .ignoring(StaleElementReferenceException::class.java)
                        .until {
                            val target = driver.findElement(locator)
                            val bounds = target.rect
                            val ready =
                                target.isDisplayed && target.isEnabled && bounds.width > 0 && bounds.height > 0 &&
                                    bounds == previousBounds
                            previousBounds = bounds
                            target.takeIf { ready }
                        }
                } else {
                    driver.findElement(locator)
                }
            element.click()
        } catch (throwable: Throwable) {
            captureFailure(ArtifactCollectionError::CollectionFailed) {
                saveDebugSnapshot("click-failed")
            }
            throw throwable
        }
    }

    private fun inputLastElement(selector: String, input: String, seconds: Long = UI_WAIT_SECONDS) {
        try {
            val locator = AppiumBy.androidUIAutomator(selector)
            if (seconds > 0) {
                WebDriverWait(driver, Duration.ofSeconds(seconds))
                    .ignoring(StaleElementReferenceException::class.java)
                    .until {
                        val element = driver.findElements(locator).lastOrNull() ?: return@until false
                        element.sendKeys(input)
                        true
                    }
            } else {
                driver.findElements(locator).last().sendKeys(input)
            }
        } catch (throwable: Throwable) {
            captureFailure(ArtifactCollectionError::CollectionFailed) {
                saveDebugSnapshot("input-failed")
            }
            throw throwable
        }
    }

    private fun saveDebugSnapshot(name: String) {
        report.captureSnapshot(driver, name)
    }

    private companion object {
        const val UI_WAIT_SECONDS = 15L
    }
}
