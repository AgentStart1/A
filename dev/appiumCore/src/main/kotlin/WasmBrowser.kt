/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.edge.EdgeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.firefox.GeckoDriverService
import org.openqa.selenium.net.PortProber
import java.io.File
import kotlin.uuid.Uuid

/** Browser variants run serially with isolated profiles, including across process restarts. */
enum class WasmBrowser {
    CHROME,
    EDGE,
    FIREFOX,
    ;

    fun createDriver(profile: File, windowSize: Dimension, syntheticMedia: Boolean = false): WebDriver {
        val binary =
            System.getenv("WASM_E2E_${name}_BINARY")?.let(::File)?.also {
                require(it.isFile) { "WASM_E2E_${name}_BINARY must name an existing browser executable" }
            }
        val downloads = profile.resolve("downloads").also(File::mkdirs).canonicalPath
        // Leaving the binary unset lets Selenium Manager discover installed browsers before downloading.
        return when (this) {
            CHROME ->
                ChromeDriver(
                    ChromeOptions().addArguments(chromiumArguments(profile, windowSize, syntheticMedia)).also {
                        if (binary != null) it.setBinary(binary.canonicalPath)
                        it.setExperimentalOption(
                            "prefs",
                            mapOf(
                                "intl.accept_languages" to "en-US,en",
                                "download.default_directory" to downloads,
                                "download.prompt_for_download" to false,
                            ),
                        )
                    },
                )

            EDGE ->
                createBlankEdgeDriver(
                    EdgeOptions().addArguments(chromiumArguments(profile, windowSize, syntheticMedia)).also {
                        if (binary != null) it.setBinary(binary.canonicalPath)
                        it.setExperimentalOption(
                            "prefs",
                            mapOf(
                                "intl.accept_languages" to "en-US,en",
                                "download.default_directory" to downloads,
                                "download.prompt_for_download" to false,
                            ),
                        )
                    },
                )

            FIREFOX ->
                FirefoxDriver(
                    firefoxService(),
                    firefoxOptions(downloads, syntheticMedia)
                        .addArguments(
                            "-headless",
                            "--profile",
                            profile.canonicalPath,
                            "--width=${windowSize.width}",
                            "--height=${windowSize.height}",
                        ).also { options -> if (binary != null) options.setBinary(binary.canonicalPath) },
                )
        }
    }

    suspend fun <T> withBrowser(
        windowSize: Dimension = Dimension(1280, 900),
        beforeClose: (WebDriver) -> Unit = {},
        block: (WebDriver) -> T,
    ): T =
        withProfile { profile ->
            val browser = createDriver(profile, windowSize)
            useWasmBrowser(browser, beforeClose, block)
        }

    suspend fun <T> withProfile(directory: File? = null, block: suspend (File) -> T): T {
        val profile =
            (directory ?: File("build/test/appium/$name-profile-${Uuid.random().toHexString()}"))
                .also { check(it.mkdirs() || it.isDirectory) { "Cannot create browser profile" } }
        return try {
            block(profile)
        } finally {
            profile.deleteRecursively()
        }
    }

    private fun chromiumArguments(profile: File, windowSize: Dimension, syntheticMedia: Boolean) =
        listOf(
            "--headless=new",
            "--user-data-dir=${profile.canonicalPath}",
            "--lang=en-US",
            "--window-size=${windowSize.width},${windowSize.height}",
            "--no-first-run",
            "--no-default-browser-check",
        ) +
            if (syntheticMedia) {
                listOf("--use-fake-device-for-media-stream", "--use-fake-ui-for-media-stream")
            } else {
                emptyList()
            }

    private fun firefoxOptions(downloads: String, syntheticMedia: Boolean) =
        FirefoxOptions().addPreference("intl.accept_languages", "en-US,en")
            .addPreference("intl.locale.requested", "en-US")
            .addPreference("browser.download.folderList", 2)
            .addPreference("browser.download.dir", downloads)
            .addPreference("browser.download.useDownloadDir", true)
            .addPreference(
                "browser.helperApps.neverAsk.saveToDisk",
                "application/octet-stream,image/png,application/pdf,video/mp4,audio/wav",
            )
            .also {
                if (syntheticMedia) {
                    it.addPreference("media.navigator.streams.fake", true)
                    it.addPreference("media.navigator.permission.disabled", true)
                }
            }

    private fun firefoxService(): GeckoDriverService =
        object : GeckoDriverService.Builder() {
            // In-place profiles require an explicit port, including when reusing the profile after quit.
            // https://firefox-source-docs.mozilla.org/testing/geckodriver/Profiles.html
            override fun createArgs(): List<String> =
                super.createArgs() + listOf("--marionette-port", PortProber.findFreePort().toString())
        }.build()
}

/** Kotlin use retains the test failure and suppresses failures from snapshot/driver cleanup. */
internal fun <T> useWasmBrowser(browser: WebDriver, beforeClose: (WebDriver) -> Unit, block: (WebDriver) -> T): T =
    AutoCloseable {
        val failures = mutableListOf<Throwable>()
        try {
            beforeClose(browser)
        } catch (failure: Throwable) {
            failures.add(failure)
        }
        try {
            browser.quit()
        } catch (failure: Throwable) {
            failures.add(failure)
        }
        failures.firstOrNull()?.let { failure ->
            failures.drop(1).forEach(failure::addSuppressed)
            throw failure
        }
    }.use { block(browser) }
