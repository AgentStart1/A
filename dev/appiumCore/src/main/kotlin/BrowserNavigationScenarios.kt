/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration
import kotlin.test.assertEquals

enum class BrowserNavigationCase {
    ROUND_TRIP,
    RELOAD,
    MODAL_DISMISSAL,
}

/** Exercises real app navigation without requiring a signed-in account or backend containers. */
fun testBrowserNavigation(
    browserType: WasmBrowser,
    distribution: File,
    panel: Boolean,
    scenario: BrowserNavigationCase,
) = runAppiumBlockingTest {
    val scope = if (panel) "panel" else "app"
    val report = AppiumTestReport("${scope}BrowserNavigation", "wasm-${browserType.name.lowercase()}", scenario.name)
    report.run { runNavigationBrowser(report, browserType, distribution, panel, scenario) }
}

private suspend fun runNavigationBrowser(
    report: AppiumTestReport,
    browserType: WasmBrowser,
    distribution: File,
    panel: Boolean,
    scenario: BrowserNavigationCase,
) {
    WasmDistributionServer(distribution, coverageDirectory = report.directory.resolve("coverage/wasm")).use { server ->
        browserType.withProfile(report.temporaryPath("browser-profile")) { profile ->
            val browser = browserType.createDriver(profile, Dimension(1000, 900))
            AutoCloseable {
                report.captureDriver(browser)
                report.collect("Wasm coverage") { server.collectCoverage(browser) }
                browser.quit()
            }.use {
                val driver = WasmAppTestDriver(browser, report.directory, profile.resolve("downloads"))
                browser.get(server.bootstrapUrl)
                preferDownloadedFonts(browser)
                browser.get("${server.url}?appium=true")
                openLoginInput(browser, driver, panel)
                when (scenario) {
                    BrowserNavigationCase.ROUND_TRIP -> verifyNavigationRoundTrip(browser, driver, server, panel)
                    BrowserNavigationCase.RELOAD -> verifyNavigationReload(browser, driver, panel)
                    BrowserNavigationCase.MODAL_DISMISSAL -> verifyModalDismissal(browser, driver, panel)
                }
            }
        }
    }
}

private suspend fun openLoginInput(browser: WebDriver, driver: WasmAppTestDriver, panel: Boolean) {
    val scope = if (panel) "panel" else "app"
    val select = if (panel) "Input" else "Private Key"
    waitNavigationDepth(browser, scope, 1)
    if (!panel) driver.clickByDescription("avatar")
    driver.clickByText("Sign in")
    driver.assertVisibleByText(select)
    waitNavigationDepth(browser, scope, 2)
    driver.clickByText(select)
    driver.assertVisibleByDescription("Edit Private Key")
    waitNavigationDepth(browser, "$scope/${if (panel) "login" else "signin"}", 2)
}

private suspend fun verifyNavigationRoundTrip(
    browser: WebDriver,
    driver: WasmAppTestDriver,
    server: WasmDistributionServer,
    panel: Boolean,
) {
    val length = historyLength(browser)
    verifyBrowserRoundTrip(browser, driver, panel)
    // The app toolbar must traverse existing history, preserving browser Forward.
    if (panel) driver.pressEscape() else driver.clickByDescription("back to pre page")
    val scope = if (panel) "panel" else "app"
    val child = "$scope/${if (panel) "login" else "signin"}"
    waitNavigationDepth(browser, child, 1)
    assertEquals(length, historyLength(browser))
    if (panel) driver.pressEscape() else driver.clickByDescription("back to pre page")
    waitNavigationDepth(browser, scope, 1)
    assertEquals(length, historyLength(browser))
    browser.navigate().forward()
    driver.assertVisibleByText(if (panel) "Input" else "Private Key")
    browser.navigate().forward()
    driver.assertVisibleByDescription("Edit Private Key")
    waitNavigationDepth(browser, child, 2)
    browser.navigate().back()
    waitNavigationDepth(browser, child, 1)
    browser.navigate().back()
    waitNavigationDepth(browser, scope, 1)
    browser.navigate().back()
    WebDriverWait(browser, Duration.ofSeconds(30)).until { browser.currentUrl == server.bootstrapUrl }
    browser.navigate().forward()
    waitNavigationDepth(browser, scope, 1)
    if (panel) driver.assertVisibleByText("Sign in") else driver.assertVisibleByDescription("avatar")
}

private suspend fun verifyNavigationReload(browser: WebDriver, driver: WasmAppTestDriver, panel: Boolean) {
    val length = historyLength(browser)
    browser.navigate().refresh()
    driver.assertVisibleByDescription("Edit Private Key")
    val scope = if (panel) "panel" else "app"
    waitNavigationDepth(browser, "$scope/${if (panel) "login" else "signin"}", 2)
    assertEquals(length, historyLength(browser))
    verifyBrowserRoundTrip(browser, driver, panel)
}

private suspend fun verifyBrowserRoundTrip(browser: WebDriver, driver: WasmAppTestDriver, panel: Boolean) {
    val scope = if (panel) "panel" else "app"
    val child = "$scope/${if (panel) "login" else "signin"}"
    val select = if (panel) "Input" else "Private Key"
    browser.navigate().back()
    driver.assertVisibleByText(select)
    waitNavigationDepth(browser, child, 1)
    browser.navigate().back()
    waitNavigationDepth(browser, scope, 1)
    if (panel) driver.assertVisibleByText("Sign in") else driver.assertVisibleByDescription("avatar")
    browser.navigate().forward()
    driver.assertVisibleByText(select)
    browser.navigate().forward()
    driver.assertVisibleByDescription("Edit Private Key")
    waitNavigationDepth(browser, child, 2)
}

private suspend fun verifyModalDismissal(browser: WebDriver, driver: WasmAppTestDriver, panel: Boolean) {
    val length = historyLength(browser)
    driver.clickByDescription("Edit Private Key")
    driver.assertVisibleByText("Confirm")
    driver.dismissDialog()
    driver.assertNotVisibleByText("Confirm")
    assertEquals(length, historyLength(browser))
    verifyBrowserRoundTrip(browser, driver, panel)
}

private fun historyLength(browser: WebDriver): Any? =
    (browser as JavascriptExecutor).executeScript("return history.length")

private fun waitNavigationDepth(browser: WebDriver, id: String, expected: Int) {
    WebDriverWait(browser, Duration.ofSeconds(30)).until {
        (browser as JavascriptExecutor).executeScript(
            """
            const saved = window.history.state?.aNavigation;
            return saved ? JSON.parse(saved).stacks[arguments[0]]?.keys.length === arguments[1] : false;
            """.trimIndent(),
            id,
            expected,
        ) == true
    }
}
