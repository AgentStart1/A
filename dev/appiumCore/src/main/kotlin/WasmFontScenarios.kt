/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.By
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chromium.HasCdp
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val FONT_PREFERENCE_KEY = "a.preferSystemFonts"

/** Business scenarios choose the normal downloaded-font path on the same-origin bootstrap page. */
fun preferDownloadedFonts(browser: WebDriver) {
    (browser as JavascriptExecutor).executeScript("localStorage.setItem(arguments[0], 'false');", FONT_PREFERENCE_KEY)
}

enum class WasmFontCase {
    FIRST_CHOICE,
    PERSISTENCE,
    PENDING_PERMISSION,
    DENIED_PERMISSION,
    GRANTED_PERMISSION,
    REVOKED_PERMISSION,
}

fun testWasmFonts(browserType: WasmBrowser, distribution: File, panel: Boolean, scenario: WasmFontCase) =
    runAppiumBlockingTest {
        val scope = if (panel) "panel" else "app"
        val report = AppiumTestReport("${scope}FontGate", "wasm-${browserType.name.lowercase()}", scenario.name)
        report.run {
            WasmDistributionServer(
                distribution,
                coverageDirectory = report.directory.resolve("coverage/wasm"),
            ).use { server ->
                browserType.withProfile(report.temporaryPath("browser-profile")) { profile ->
                    var browser = browserType.createDriver(profile, Dimension(1000, 900))
                    try {
                        runFontScenario(browser, server, panel, scenario)
                        if (scenario == WasmFontCase.PERSISTENCE) {
                            report.captureDriver(browser, report.directory.resolve("before-restart"))
                            server.collectCoverage(browser)
                            browser.quit()
                            browser = browserType.createDriver(profile, Dimension(1000, 900))
                            browser.get("${server.url}?appium=true")
                            awaitFontContent(browser, panel)
                            assertFalse(isFontGateOpen(browser))
                        }
                    } finally {
                        report.captureDriver(browser)
                        report.collect("Wasm coverage") { server.collectCoverage(browser) }
                        report.collect("browser shutdown") { browser.quit() }
                    }
                }
            }
        }
    }

private fun runFontScenario(
    browser: WebDriver,
    server: WasmDistributionServer,
    panel: Boolean,
    scenario: WasmFontCase,
) {
    browser.get(server.bootstrapUrl)
    val js = browser as JavascriptExecutor
    val supported = js.executeScript("return isSecureContext && typeof queryLocalFonts === 'function';") == true
    if (supported) prepareFontAccess(browser, server, scenario)
    browser.get("${server.url}?appium=true")
    if (!supported) {
        awaitFontContent(browser, panel)
        assertFalse(isFontGateOpen(browser))
        assertEquals(null, js.executeScript("return localStorage.getItem(arguments[0]);", FONT_PREFERENCE_KEY))
        return
    }
    awaitFontGate(browser)
    assertFalse(hasFontContent(browser, panel), "Business page must not mount before the font choice")
    when (scenario) {
        WasmFontCase.FIRST_CHOICE, WasmFontCase.PERSISTENCE -> {
            browser.findElement(By.id("font-gate-default")).click()
            awaitFontContent(browser, panel)
            assertEquals("false", js.executeScript("return localStorage.getItem(arguments[0]);", FONT_PREFERENCE_KEY))
            if (scenario == WasmFontCase.PERSISTENCE) {
                browser.navigate().refresh()
                awaitFontContent(browser, panel)
                assertFalse(isFontGateOpen(browser))
            }
        }

        WasmFontCase.PENDING_PERMISSION -> {
            assertTrue(browser.findElement(By.id("font-gate-message")).text.contains("not ready"))
            browser.findElement(By.id("font-gate-default")).click()
            awaitFontContent(browser, panel)
        }

        WasmFontCase.DENIED_PERMISSION -> {
            browser.findElement(By.id("font-gate-local")).click()
            awaitFontMessage(browser, "not ready")
            assertFalse(hasFontContent(browser, panel))
            assertEquals("true", js.executeScript("return localStorage.getItem(arguments[0]);", FONT_PREFERENCE_KEY))
            browser.findElement(By.id("font-gate-default")).click()
            awaitFontContent(browser, panel)
        }

        WasmFontCase.GRANTED_PERMISSION, WasmFontCase.REVOKED_PERMISSION -> {
            browser.findElement(By.id("font-gate-local")).click()
            awaitFontContent(browser, panel)
            assertEquals("true", js.executeScript("return localStorage.getItem(arguments[0]);", FONT_PREFERENCE_KEY))
            assertFalse(isFontGateOpen(browser))
            if (scenario == WasmFontCase.REVOKED_PERMISSION) {
                setFontPermission(browser, server, "denied")
                awaitFontMessage(browser, "not ready")
                WebDriverWait(browser, Duration.ofSeconds(30)).until { !hasFontContent(browser, panel) }
                browser.findElement(By.id("font-gate-default")).click()
                awaitFontContent(browser, panel)
            }
        }
    }
}

private fun prepareFontAccess(browser: WebDriver, server: WasmDistributionServer, scenario: WasmFontCase) {
    if (scenario == WasmFontCase.PENDING_PERMISSION) {
        (browser as JavascriptExecutor).executeScript(
            "localStorage.setItem(arguments[0], 'true');",
            FONT_PREFERENCE_KEY,
        )
    }
    if (scenario == WasmFontCase.DENIED_PERMISSION) setFontPermission(browser, server, "denied")
    if (scenario in listOf(WasmFontCase.GRANTED_PERMISSION, WasmFontCase.REVOKED_PERMISSION)) {
        setFontPermission(browser, server, "granted")
    }
}

private fun setFontPermission(browser: WebDriver, server: WasmDistributionServer, setting: String) {
    (browser as HasCdp).executeCdpCommand(
        "Browser.setPermission",
        mapOf("permission" to mapOf("name" to "local-fonts"), "setting" to setting, "origin" to server.url),
    )
}

private fun awaitFontGate(browser: WebDriver) {
    WebDriverWait(browser, Duration.ofSeconds(30)).until { isFontGateOpen(browser) }
}

private fun awaitFontMessage(browser: WebDriver, text: String) {
    WebDriverWait(browser, Duration.ofSeconds(30)).until {
        isFontGateOpen(browser) && browser.findElement(By.id("font-gate-message")).text.contains(text)
    }
}

private fun isFontGateOpen(browser: WebDriver): Boolean =
    (browser as JavascriptExecutor).executeScript("return document.querySelector('#font-gate')?.open === true;") == true

private fun hasFontContent(browser: WebDriver, panel: Boolean): Boolean =
    browser.findElements(fontContentLocator(panel)).isNotEmpty()

private fun awaitFontContent(browser: WebDriver, panel: Boolean) {
    WebDriverWait(browser, Duration.ofSeconds(30)).until(
        ExpectedConditions.presenceOfElementLocated(fontContentLocator(panel)),
    )
}

private fun fontContentLocator(panel: Boolean): By =
    By.cssSelector(if (panel) "[data-appium-text='Sign in']" else "[aria-label='avatar']")
