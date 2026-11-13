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
import org.openqa.selenium.WebDriver
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration

/** Fixture cleanup also runs if any client fails to start. */
class WasmClientsSetup<T>(
    val data: T,
    val clients: Map<String, InjectedSession?>,
    private val cleanup: () -> Unit = {},
) : AutoCloseable {
    override fun close() = cleanup()
}

private data class WasmClientEnvironment(
    val report: AppiumTestReport,
    val server: WasmDistributionServer,
    val ports: AppiumPorts,
    val multiple: Boolean,
    val syntheticMedia: Boolean,
)

class WasmAppiumHelper(
    private val distribution: File,
    private val startupLocator: By,
    private val browserType: WasmBrowser,
    private val windowSize: Dimension = Dimension(1280, 900),
) : PlatformAppiumHelper {
    override val capturesExternalAsciidocPreview = false

    override suspend fun <T> runTest(
        testName: String,
        target: TargetAppiumHelper,
        captureBrowserOpen: Boolean,
        setup: suspend (AppiumPorts) -> AppiumTestSetup<T>,
        block: suspend (AppiumTestScope, T) -> Unit,
    ) = runClients(testName, target, setup = { ports ->
        val prepared = setup(ports)
        WasmClientsSetup(prepared.data, mapOf("primary" to prepared.injectedSession))
    }) { clients, data -> block(clients.getValue("primary"), data) }

    /** Each role owns a browser profile; all roles connect to the same isolated real backend. */
    suspend fun <T> runClients(
        testName: String,
        target: TargetAppiumHelper,
        syntheticMedia: Boolean = false,
        setup: suspend (AppiumPorts) -> WasmClientsSetup<T>,
        block: suspend (Map<String, AppiumTestScope>, T) -> Unit,
    ) {
        val report =
            AppiumTestReport(
                target.suiteName,
                "wasm-${browserType.name.lowercase()}",
                testName.removeSuffix("[${browserType.name}]"),
            )
        report.run {
            WasmDistributionServer(
                distribution,
                coverageDirectory = report.directory.resolve("coverage/wasm"),
            ).use { server ->
                runAppiumTestEnvironment(report, allowedOrigins = server.url.removeSuffix("/")) { ports ->
                    server.setBackendUrl("http://127.0.0.1:${ports.server}")
                    setup(ports).use { prepared ->
                        require(prepared.clients.isNotEmpty())
                        val environment =
                            WasmClientEnvironment(
                                report,
                                server,
                                ports,
                                prepared.clients.size > 1,
                                syntheticMedia,
                            )
                        withClients(prepared.clients.toList(), environment, emptyMap()) { clients ->
                            block(clients, prepared.data)
                        }
                    }
                }
            }
        }
    }

    private suspend fun withClients(
        remaining: List<Pair<String, InjectedSession?>>,
        environment: WasmClientEnvironment,
        clients: Map<String, AppiumTestScope>,
        block: suspend (Map<String, AppiumTestScope>) -> Unit,
    ) {
        val next = remaining.firstOrNull()
        if (next == null) {
            block(clients)
        } else {
            withClient(next.first, next.second, environment) { client ->
                withClients(remaining.drop(1), environment, clients + (next.first to client), block)
            }
        }
    }

    private suspend fun withClient(
        role: String,
        session: InjectedSession?,
        environment: WasmClientEnvironment,
        block: suspend (AppiumTestScope) -> Unit,
    ) {
        require(role.matches(Regex("[a-z-]+")))
        val report = environment.report
        val server = environment.server
        val ports = environment.ports
        val artifacts =
            (
                if (environment.multiple) {
                    report.directory.resolve(
                        role,
                    )
                } else {
                    report.directory
                }
                ).also(File::mkdirs)
        browserType.withProfile(directory = report.temporaryPath("browser-$role")) { profile ->
            var browser: WebDriver = browserType.createDriver(profile, windowSize, environment.syntheticMedia)
            try {
                initializeBrowserState(browser, server, session)
                browser.get(
                    "${server.url}?appium=true&appiumHttpUrl=${server.url.removeSuffix(
                        "/",
                    )}&appiumWsUrl=ws://127.0.0.1:${ports.ws}",
                )
                awaitStartup(browser, server)
                if (session != null) {
                    WebDriverWait(browser, Duration.ofSeconds(30)).until {
                        (browser as JavascriptExecutor).executeScript(
                            "return window.localStorage.getItem('appium.session_ready') === 'true';",
                        ) == true
                    }
                }
                block(
                    WasmAppiumTestScope({ browser }, artifacts, profile.resolve("downloads")) {
                        val url = requireNotNull(browser.currentUrl)
                        (browser as JavascriptExecutor).executeScript(
                            "window.localStorage.removeItem('appium.injected_session');" +
                                "window.localStorage.setItem('appium.session_ready', 'false');",
                        )
                        server.collectCoverage(browser)
                        browser.quit()
                        browser = browserType.createDriver(profile, windowSize, environment.syntheticMedia)
                        browser.get(url)
                        awaitStartup(browser, server)
                    },
                )
            } finally {
                report.captureDriver(browser, artifacts)
                collectBrowserConsole(report, browser, artifacts, role)
                report.collect("$role Wasm coverage") { server.collectCoverage(browser) }
                report.collect("$role browser shutdown") { browser.quit() }
            }
        }
    }

    private fun collectBrowserConsole(report: AppiumTestReport, browser: WebDriver, artifacts: File, role: String) {
        if (browserType != WasmBrowser.FIREFOX) {
            report.collect("$role browser console") {
                val entries = browser.manage().logs().get(LogType.BROWSER)
                val output = artifacts.resolve("logs/browser.log")
                output.parentFile.mkdirs()
                output.writeText(entries.all.joinToString("\n") { "${it.level}: ${it.message}" })
            }
        }
    }

    private fun awaitStartup(browser: WebDriver, server: WasmDistributionServer) {
        captureFailure(ArtifactCollectionError::CollectionFailed) {
            WebDriverWait(browser, Duration.ofSeconds(30)).until(
                ExpectedConditions.presenceOfElementLocated(startupLocator),
            )
        }.getOrElse { cause ->
            val body = browser.findElement(By.tagName("body")).getAttribute("innerHTML")
            val errors = (browser as JavascriptExecutor).executeScript("return window.appiumErrors || [];")
            throw AssertionError(
                "Wasm Appium semantics did not mount; requested=${server.requestedPaths}; body=${body ?: "<none>"}; errors=${errors ?: "<none>"}",
                cause.cause,
            )
        }
    }

    private fun initializeBrowserState(browser: WebDriver, server: WasmDistributionServer, session: InjectedSession?) {
        browser.get(server.bootstrapUrl)
        session?.cookies?.forEach { cookie ->
            browser.manage().addCookie(
                org.openqa.selenium.Cookie.Builder(cookie.name, cookie.value)
                    .path(cookie.path ?: "/").isHttpOnly(cookie.httpOnly).isSecure(cookie.secure).build(),
            )
        }
        (browser as JavascriptExecutor).executeScript(
            """
            window.localStorage.clear();
            if (arguments[0] !== null) {
              window.localStorage.setItem('appium.injected_session', arguments[0]);
            }
            """.trimIndent(),
            session?.let(::buildInjectedSessionJson),
        )
        preferDownloadedFonts(browser)
    }
}

private class WasmAppiumTestScope(
    private val getBrowser: () -> WebDriver,
    private val artifactDirectory: File,
    private val downloadDirectory: File,
    private val restartBrowser: () -> Unit,
) : AppiumTestScope {
    override val driver get() = WasmAppTestDriver(getBrowser(), artifactDirectory, downloadDirectory)

    override suspend fun assertAsciidocPreviewOpened(source: String) {
        val browser = getBrowser()
        val title =
            source.lineSequence().firstOrNull { it.startsWith("= ") }?.removePrefix("= ")
                ?: error("AsciiDoc source must include a document title")
        WebDriverWait(browser, Duration.ofSeconds(15)).until(
            ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[aria-label='asciidoc-preview'][data-appium-text='${title.cssAttributeValue()}']"),
            ),
        )
    }

    override suspend fun restart() = restartBrowser()

    private fun String.cssAttributeValue(): String = replace("\\", "\\\\").replace("'", "\\'")
}
