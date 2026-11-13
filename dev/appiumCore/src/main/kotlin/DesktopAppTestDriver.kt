/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import io.appium.java_client.AppiumBy
import io.appium.java_client.AppiumDriver
import io.appium.java_client.remote.AppiumWebElement
import org.openqa.selenium.By
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class DesktopAppTestDriver internal constructor(
    private val driver: AppiumDriver,
    private val report: AppiumTestReport,
    windowsProcessId: Long? = null,
) : AppTestDriver {
    private val platform = DesktopTestPlatform.current()
    private val windows =
        if (platform == DesktopTestPlatform.Windows) {
            DesktopWindowsWindowTracker(driver, requireNotNull(windowsProcessId))
        } else {
            null
        }
    internal val media = DesktopMediaControls(driver) { windows?.synchronizeWindow() }
    private val nameAttribute =
        when (platform) {
            DesktopTestPlatform.Windows -> "Name"
            DesktopTestPlatform.Mac -> "label"
            DesktopTestPlatform.Linux -> "name"
        }
    private val valueAttribute = if (platform == DesktopTestPlatform.Windows) "Value" else "value"
    suspend fun cancelNativeSaveDialog() {
        if (platform == DesktopTestPlatform.Mac) {
            findMacSaveControl("OKButton")
            findMacSaveControl("CancelButton")
            report.captureSnapshot(driver, "native-save-dialog")
            clickElement { findMacSaveControl("CancelButton") }
            WebDriverWait(driver, Duration.ofSeconds(1)).until {
                driver.findElements(AppiumBy.accessibilityId("save-panel")).isEmpty()
            }
            return
        }
        assertVisibleByText("Save")
        assertVisibleByText("Cancel")
        report.captureSnapshot(driver, "native-save-dialog")
        clickByText("Cancel")
        assertNotVisibleByText("Save", timeoutSeconds = 1)
    }

    private fun findMacSaveControl(identifier: String): org.openqa.selenium.WebElement =
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS)).until {
            // AppKit also exposes off-screen Touch Bar duplicates; use the actual save panel.
            driver.findElements(AppiumBy.accessibilityId("save-panel")).singleOrNull()
                ?.findElements(AppiumBy.accessibilityId(identifier))?.singleOrNull()
                ?.takeIf { it.isDisplayed && it.isEnabled }
        }

    suspend fun assertAsciidocPreviewOpened() {
        assertVisibleByDescription("close preview")
    }

    override suspend fun clickByDescription(description: String) {
        clickElement { findDescriptionElement(description) }
    }

    override suspend fun clickByText(text: String) {
        clickElement(textTarget = true) { findTextElement(text, false) }
    }

    override suspend fun clickByTextContaining(text: String) {
        clickElement(textTarget = true) { findTextElement(text, true) }
    }

    private fun findDescriptionElement(description: String): org.openqa.selenium.WebElement =
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS)).until {
            windows?.synchronizeWindow()
            if (platform == DesktopTestPlatform.Mac) {
                driver.findElements(AppiumBy.iOSNsPredicateString("label == ${macPredicateLiteral(description)}"))
                    .firstOrNull()
                    ?: driver.findElements(AppiumBy.iOSNsPredicateString(macTextPredicate(description, false)))
                        .lastOrNull()
            } else {
                val exact =
                    if (platform == DesktopTestPlatform.Windows) {
                        driver.findElements(By.name(description)).firstOrNull()
                    } else {
                        driver.findElements(By.xpath("//*[@$nameAttribute='$description']")).firstOrNull()
                    }
                exact
                    // Incoming content follows the outgoing scene during Compose navigation transitions.
                    ?: driver.findElements(By.xpath("//*[${namePredicate(description)}]")).lastOrNull()
            }
        }

    private fun clickElement(
        textTarget: Boolean = false,
        findElement: () -> org.openqa.selenium.WebElement,
    ): org.openqa.selenium.WebElement {
        var previousBounds: org.openqa.selenium.Rectangle? = null
        return WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
            .ignoring(StaleElementReferenceException::class.java)
            .until {
                val element = findElement()
                // Semantics can appear before a navigation animation settles.
                val bounds = element.rect
                val ready =
                    bounds.width > 0 && bounds.height > 0 &&
                        bounds == previousBounds && element.isDisplayed && element.isEnabled
                previousBounds = bounds
                if (!ready) return@until null
                performElementClick(element, textTarget)
                element
            }
    }

    private fun performElementClick(element: org.openqa.selenium.WebElement, textTarget: Boolean) {
        if (platform == DesktopTestPlatform.Windows) {
            val elementId = (element as AppiumWebElement).id
            val focusable = element.getAttribute("IsKeyboardFocusable").equals("true", ignoreCase = true)
            if (focusable && element.tagName == "TabItem") {
                driver.executeScript("windows: setFocus", mapOf("elementId" to elementId))
                driver.executeScript("windows: keys", mapOf("actions" to listOf(mapOf("virtualKeyCode" to 0x20))))
            } else if (element.getAttribute("IsInvokePatternAvailable").equals("true", ignoreCase = true)) {
                if (focusable) driver.executeScript("windows: setFocus", mapOf("elementId" to elementId))
                driver.executeScript("windows: invoke", mapOf("elementId" to elementId))
            } else if (element.getAttribute("IsSelectionItemPatternAvailable").equals("true", ignoreCase = true)) {
                driver.executeScript("windows: select", mapOf("elementId" to elementId))
            } else {
                // Let Tao process cursor movement before pressing at the target's position.
                driver.executeScript("windows: hover", mapOf("endElementId" to elementId, "durationMs" to 200))
                driver.executeScript("windows: click", mapOf("elementId" to elementId, "durationMs" to 100))
            }
        } else if (platform == DesktopTestPlatform.Mac) {
            // An element click can succeed without focusing a Compose editor. Use a native pointer click.
            // A text container's center can hit an embedded control instead of its own label.
            val point =
                if (textTarget) {
                    desktopTextClickPoint(
                        element.rect,
                        element.findElements(AppiumBy.iOSNsPredicateString("TRUEPREDICATE")).map { it.rect },
                    )
                } else {
                    val bounds = element.rect
                    org.openqa.selenium.Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2)
                }
            driver.executeScript("macos: click", mapOf("x" to point.x, "y" to point.y))
        } else {
            element.click()
        }
    }

    override suspend fun inputText(text: String) {
        if (platform == DesktopTestPlatform.Windows) {
            inputWindowsText(text)
            return
        }
        if (platform == DesktopTestPlatform.Mac) {
            inputMacText(text)
            return
        }
        // A navigation transition can retire an editor after lookup; re-locate its current identity.
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
            .ignoring(StaleElementReferenceException::class.java)
            .until {
                val element = driver.findElements(By.xpath("//text-field")).lastOrNull() ?: return@until false
                // Linux setValue replaces contents, so a stale editor never causes appended duplicate input.
                element.sendKeys(text)
                true
            }
    }

    private fun inputWindowsText(text: String) {
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS)).until {
            windows?.synchronizeWindow()
            val element = driver.findElements(By.xpath("//Edit")).lastOrNull() ?: return@until false
            driver.executeScript(
                "windows: setFocus",
                mapOf("elementId" to (element as AppiumWebElement).id),
            )
            element.getAttribute("HasKeyboardFocus").toBoolean()
        }
        driver.executeScript("windows: keys", mapOf("actions" to buildWindowsTextActions(text), "forceUnicode" to true))
    }

    private fun inputMacText(text: String) {
        val editor =
            clickElement {
                listOf("XCUIElementTypeTextField", "XCUIElementTypeSecureTextField", "XCUIElementTypeTextView")
                    .flatMap { driver.findElements(By.className(it)) }.lastOrNull()
                    ?: throw org.openqa.selenium.NoSuchElementException("No macOS text editor is available")
            }
        // Mac2 sends this string through XCTest typeText, preserving native keyboard input.
        editor.sendKeys(text.replace("\r\n", "\n"))
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS)).until {
            editor.getAttribute("value")?.replace("\r\n", "\n") == text.replace("\r\n", "\n")
        }
    }

    override suspend fun assertVisibleByDescription(description: String) {
        findDescriptionElement(description)
    }

    override suspend fun assertNotVisibleByDescription(description: String, timeoutSeconds: Long) {
        val locator =
            if (platform == DesktopTestPlatform.Mac) {
                AppiumBy.iOSNsPredicateString(macTextPredicate(description, false))
            } else {
                By.xpath("//*[${namePredicate(description)}]")
            }
        assertNotVisible(locator, timeoutSeconds)
    }

    override suspend fun assertVisibleByText(text: String) {
        findTextElement(text, false)
    }

    override suspend fun assertVisibleByTextContaining(text: String) {
        findTextElement(text, true)
    }

    private fun findTextElement(text: String, contains: Boolean): org.openqa.selenium.WebElement =
        WebDriverWait(driver, Duration.ofSeconds(UI_WAIT_SECONDS))
            .ignoring(StaleElementReferenceException::class.java)
            .until {
                windows?.synchronizeWindow()
                if (platform == DesktopTestPlatform.Mac) {
                    driver.findElements(AppiumBy.iOSNsPredicateString("label == ${macPredicateLiteral(text)}"))
                        .lastOrNull()
                        ?: driver.findElements(AppiumBy.iOSNsPredicateString(macTextPredicate(text, contains)))
                            .lastOrNull()
                        ?: if (contains) findMacValueContaining(text) else null
                } else {
                    val predicate = if (contains) "contains(@$nameAttribute,'$text')" else namePredicate(text)
                    val exact =
                        if (platform == DesktopTestPlatform.Windows) {
                            driver.findElements(By.name(text)).lastOrNull()
                        } else {
                            driver.findElements(By.xpath("//*[@$nameAttribute='$text']")).lastOrNull()
                        }
                    exact ?: driver.findElements(By.xpath("//*[$predicate]")).lastOrNull()
                        ?: if (platform == DesktopTestPlatform.Windows) {
                            driver.findElements(By.xpath("//Text | //Edit | //Unknown")).lastOrNull { element ->
                                val value = element.text
                                if (contains) value.contains(text) else value == text
                            }
                        } else {
                            val named =
                                if (contains) {
                                    null
                                } else {
                                    driver.findElements(By.xpath("//*[contains(@name,'$text')]"))
                                        .lastOrNull { matchesDesktopLabel(it.getAttribute("name").orEmpty(), text) }
                                }
                            val valuePredicate = if (contains) "contains(@value,'$text')" else "@value='$text'"
                            named ?: driver.findElements(By.xpath("//*[$valuePredicate]")).lastOrNull()
                        }
                }
            }

    private fun findMacValueContaining(text: String): org.openqa.selenium.WebElement? =
        listOf(
            "XCUIElementTypeStaticText",
            "XCUIElementTypeTextField",
            "XCUIElementTypeSecureTextField",
            "XCUIElementTypeTextView",
        )
            .flatMap { driver.findElements(By.className(it)) }
            .lastOrNull { it.getAttribute("value").orEmpty().contains(text) }

    private fun namePredicate(value: String): String =
        if (platform == DesktopTestPlatform.Windows) {
            "contains(concat(', ', @Name, ', '), ', $value, ')"
        } else if (platform == DesktopTestPlatform.Mac) {
            "@title='$value' or contains(concat(', ', @label, ', '), ', $value, ') or @value='$value'"
        } else {
            "@name='$value'"
        }

    override suspend fun assertNotVisibleByText(text: String, timeoutSeconds: Long) {
        val locator =
            if (platform == DesktopTestPlatform.Mac) {
                AppiumBy.iOSNsPredicateString(macTextPredicate(text, false))
            } else {
                By.xpath("//*[${namePredicate(text)} or @$valueAttribute='$text']")
            }
        assertNotVisible(locator, timeoutSeconds)
    }

    private fun assertNotVisible(locator: By, timeoutSeconds: Long) {
        WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds)).until {
            windows?.synchronizeWindow()
            driver.findElements(locator).isEmpty()
        }
    }

    override suspend fun dismissDialog() {
        if (platform == DesktopTestPlatform.Windows) {
            windows?.synchronizeWindow()
            driver.executeScript("windows: keys", mapOf("actions" to listOf(mapOf("virtualKeyCode" to 0x1B))))
        } else if (platform == DesktopTestPlatform.Mac) {
            driver.executeScript("macos: keys", mapOf("keys" to listOf("XCUIKeyboardKeyEscape")))
        } else {
            driver.executeScript("linux: dismissDialog")
        }
    }

    override suspend fun navigateBack() {
        if (platform == DesktopTestPlatform.Windows) {
            // UIA Invoke can activate a background back button through a modal scene layer.
            dismissDialog()
        } else {
            clickByDescription("back")
        }
    }

    fun resizeWindow(width: Int, height: Int) {
        windows?.synchronizeWindow()
        if (windows != null) {
            windows.resizeWindow(width, height)
        } else if (platform == DesktopTestPlatform.Mac) {
            driver.executeScript("macos: adjustWindow", mapOf("width" to width, "height" to height))
        } else {
            driver.manage().window().size = org.openqa.selenium.Dimension(width, height)
        }
    }

    override suspend fun saveSnapshot(name: String) {
        report.captureSnapshot(driver, name)
    }

    companion object {
        private const val UI_WAIT_SECONDS = 15L
    }
}

internal fun buildWindowsTextActions(text: String): List<Map<String, Any>> =
    buildList {
        text.replace("\r\n", "\n").split("\n").forEachIndexed { index, line ->
            if (index > 0) add(mapOf("virtualKeyCode" to 0x0D))
            if (line.isNotEmpty()) add(mapOf("text" to line))
        }
    }

/** AT-SPI may append comma-separated suffix labels, including an empty badge suffix. */
internal fun matchesDesktopLabel(name: String, label: String): Boolean =
    name == label || name.split(", ").any { it == label }

internal fun macPredicateLiteral(value: String): String {
    val escaped =
        value.replace("""\""", """\\""").replace("'", """\'""")
            .replace("\n", """\n""").replace("\r", """\r""")
    return "'$escaped'"
}

internal fun macTextPredicate(value: String, contains: Boolean): String {
    val literal = macPredicateLiteral(value)
    // XCTest evaluates these predicates in the target process. CONTAINS on numeric values can abort it.
    if (contains) return "label CONTAINS $literal OR title CONTAINS $literal"
    return "label == $literal OR label BEGINSWITH ${macPredicateLiteral("$value, ")} " +
        "OR label ENDSWITH ${macPredicateLiteral(", $value")} " +
        "OR label CONTAINS ${macPredicateLiteral(", $value, ")} OR title == $literal OR value == $literal"
}
