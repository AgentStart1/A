/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Holds a real response before application consumption; never supplies replacement bytes. */
internal class WasmDownloadProbe(private val browser: WebDriver, private val downloads: File) {
    private val script get() = browser as JavascriptExecutor
    private val wait get() = WebDriverWait(browser, Duration.ofSeconds(30))

    fun holdResponse(url: String) {
        script.executeScript(
            """
            const expected = new URL(arguments[0], location.href).href;
            const fetch = window.fetch;
            const state = {held: false, aborted: false, released: false, handoffs: 0};
            window.appiumDownloadProbe = state;
            const gate = new window.Promise(resolve => {window.appiumReleaseDownload = resolve;});
            window.fetch = (...args) => {
              const input = args[0];
              const address = typeof input === 'string' ? input : input.url || String(input);
              const result = fetch.apply(window, args);
              if (new URL(address, location.href).href !== expected) return result;
              const signal = args[1]?.signal || input?.signal;
              if (!signal) throw new Error('Resource fetch has no cancellation signal');
              signal.addEventListener('abort', () => {state.aborted = true;}, {once: true});
              return result.then(response => {
                state.held = true;
                return gate.then(() => {state.released = true; return response;});
              });
            };
            const click = window.HTMLAnchorElement.prototype.click;
            window.HTMLAnchorElement.prototype.click = function (...args) {
              if (this.hasAttribute('download')) state.handoffs++;
              return click.apply(this, args);
            };
            """.trimIndent(),
            url,
        )
    }

    fun awaitHeld() {
        wait.until { script.executeScript("return window.appiumDownloadProbe.held;") == true }
        assertNoHandoff()
    }

    fun awaitAborted() {
        wait.until { script.executeScript("return window.appiumDownloadProbe.aborted;") == true }
    }

    fun release() {
        script.executeScript("window.appiumReleaseDownload?.();")
    }

    fun assertLateResponseDiscarded() {
        wait.until { script.executeScript("return window.appiumDownloadProbe.released;") == true }
        script.executeAsyncScript(
            "const done = arguments[arguments.length - 1]; requestAnimationFrame(() => requestAnimationFrame(done));",
        )
        assertNoHandoff()
    }

    private fun assertNoHandoff() {
        assertEquals(0L, script.executeScript("return window.appiumDownloadProbe.handoffs;"))
        val files = checkNotNull(downloads.listFiles()) { "Browser download directory is unavailable" }
        assertTrue(files.isEmpty(), "Revoked owner must not create browser download files")
    }
}
