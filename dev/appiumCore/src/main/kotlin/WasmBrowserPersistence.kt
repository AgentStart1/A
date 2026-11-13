/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.Cookie
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import java.io.File
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals

/** Exercise the real persistent profile used for browser restart acceptance with synthetic data only. */
suspend fun testWasmProfilePersistence(browserType: WasmBrowser, distribution: File) {
    WasmDistributionServer(distribution, measureCoverage = false).use { server ->
        browserType.withProfile { profile ->
            val first = browserType.createDriver(profile, Dimension(500, 800))
            try {
                assertEdgeBlankStartup(browserType, first)
                first.get(server.bootstrapUrl)
                first.manage().addCookie(
                    Cookie.Builder("profile-test", "synthetic").path("/").isHttpOnly(true)
                        .expiresOn(Date.from(Instant.now().plusSeconds(3600))).build(),
                )
                (first as JavascriptExecutor).executeScript("localStorage.setItem('profile-test', 'synthetic');")
                assertEquals("synthetic", accessProfileDatabase(first, write = true))
            } finally {
                first.quit()
            }
            val restarted = browserType.createDriver(profile, Dimension(500, 800))
            try {
                assertEdgeBlankStartup(browserType, restarted)
                restarted.get(server.bootstrapUrl)
                val cookie = restarted.manage().getCookieNamed("profile-test")
                assertEquals("synthetic", cookie?.value)
                assertEquals(true, cookie?.isHttpOnly)
                assertEquals(
                    "synthetic",
                    (restarted as JavascriptExecutor).executeScript("return localStorage.getItem('profile-test');"),
                )
                assertEquals("synthetic", accessProfileDatabase(restarted, write = false))
            } finally {
                restarted.quit()
            }
        }
    }
}

private fun assertEdgeBlankStartup(browserType: WasmBrowser, browser: WebDriver) {
    if (browserType == WasmBrowser.EDGE) {
        assertEquals("about:blank", browser.currentUrl)
        assertEquals(1, browser.windowHandles.size)
    }
}

private fun accessProfileDatabase(browser: WebDriver, write: Boolean): Any? =
    (browser as JavascriptExecutor).executeAsyncScript(
        """
        const write = arguments[0], done = arguments[arguments.length - 1];
        const request = indexedDB.open('profile-test', 1);
        request.onupgradeneeded = () => request.result.createObjectStore('state');
        request.onerror = () => done('open-failed');
        request.onsuccess = () => {
          const database = request.result;
          const tx = database.transaction('state', write ? 'readwrite' : 'readonly');
          const store = tx.objectStore('state');
          const operation = write ? store.put('synthetic', 'value') : store.get('value');
          let value;
          operation.onsuccess = () => { value = write ? 'synthetic' : operation.result; };
          tx.oncomplete = () => { database.close(); done(value); };
          tx.onabort = () => { database.close(); done('transaction-failed'); };
        };
        """.trimIndent(),
        write,
    )
