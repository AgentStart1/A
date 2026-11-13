/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.generateTestArtifactReports
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import java.io.File
import java.lang.reflect.Proxy
import java.net.URLDecoder
import java.nio.file.Files
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AppiumTestReportTest {
    @Test
    fun snapshotsRemainWithTheirOwningCase() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val png =
                    checkNotNull(javaClass.getResourceAsStream("/media/sample.png")).buffered().use {
                        it.readBytes()
                    }
                var source = "<html>before</html>"
                val driver =
                    Proxy.newProxyInstance(
                        javaClass.classLoader,
                        arrayOf(WebDriver::class.java, TakesScreenshot::class.java),
                    ) { _, method, _ ->
                        when (method.name) {
                            "getPageSource" -> source
                            "getScreenshotAs" -> png
                            else -> error("Unexpected driver call")
                        }
                    } as WebDriver
                val first = AppiumTestReport("suite", "desktop", "first", root)
                val second = AppiumTestReport("suite", "desktop", "second", root)
                first.run {
                    first.captureSnapshot(driver, "before")
                    source = "<html>after</html>"
                    first.captureSnapshot(driver, "after")
                }
                source = "<html>second</html>"
                second.run { second.captureSnapshot(driver, "before") }
                assertEquals(
                    "<html>before</html>",
                    first.directory.resolve("snapshots/before/page-source.xml").readText(),
                )
                assertEquals(
                    "<html>after</html>",
                    first.directory.resolve("snapshots/after/page-source.xml").readText(),
                )
                assertEquals(
                    "<html>second</html>",
                    second.directory.resolve("snapshots/before/page-source.xml").readText(),
                )
                assertTrue(png.contentEquals(first.directory.resolve("snapshots/before/screenshot.png").readBytes()))
                assertTrue(png.contentEquals(second.directory.resolve("snapshots/before/screenshot.png").readBytes()))
            }
        }

    @Test
    fun isolateUnsafeNames() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val names = listOf("../test", "..\\test", "CON", "", "<test> & \"quoted\"", "测试")
                val reports = names.map { name -> AppiumTestReport("suite", "android", name, root) }
                assertEquals(reports.size, reports.map { it.directory.canonicalPath }.distinct().size)
                reports.forEach { report ->
                    assertTrue(report.directory.toPath().startsWith(root.toPath()))
                    report.run { }
                    assertTrue(report.directory.resolve("result.properties").isFile)
                    assertFalse(report.directory.resolve("index.html").exists())
                }
                assertTrue(
                    generatedRoot(root).resolve("index.html").readText().contains(
                        "&lt;test&gt; &amp; &quot;quoted&quot;",
                    ),
                )
                Regex("""href="([^"]+)"""").findAll(
                    generatedRoot(root).resolve("index.html").readText(),
                ).forEach { link ->
                    val relative = URLDecoder.decode(link.groupValues[1], Charsets.UTF_8)
                    assertTrue(generatedRoot(root).resolve(relative).isFile)
                }
            }
        }

    @Test
    fun deferHtmlUntilAggregation() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val report = AppiumTestReport("suite", "android", "case", root)
                report.run { report.directory.resolve("latest.log").writeText("latest") }
                assertFalse(root.resolve("index.html").exists())
                assertEquals(setOf("result.properties", "latest.log"), requireNotNull(report.directory.list()).toSet())
                val generated = generatedCase(root, report)
                assertTrue(generated.resolve("index.html").isFile)
                assertTrue(generated.resolve("artifact-views/latest.log.html").isFile)
                assertFalse(report.directory.resolve("artifact-views").exists())
            }
        }

    @Test
    fun preserveFailureAndExposeCollectionErrors() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val report = AppiumTestReport("suite", "desktop", "failed case", root)
                val original = AssertionError("original <failure>")
                val actual =
                    assertFailsWith<AssertionError> {
                        report.run {
                            report.collect("Recording") { error("recording unavailable") }
                            report.collect("Application log") {
                                report.directory.resolve("application.log").writeText("log")
                            }
                            report.directory.resolve("server.error.txt").writeText("service export failed")
                            throw original
                        }
                    }
                assertSame(original, actual)
                val html = generatedCase(root, report).resolve("index.html").readText()
                assertTrue(html.contains("FAILED"))
                assertTrue(html.contains("original &lt;failure&gt;"))
                assertTrue(html.contains("recording unavailable"))
                assertTrue(html.contains("service export failed"))
                assertTrue(html.contains("href=\"artifact-views/application.log.html\""))
                assertTrue(report.directory.resolve("failure.txt").readText().contains("original <failure>"))
            }
        }

    @Test
    fun reportCancelledAndSuccessfulRuns() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val cancelled = AppiumTestReport("suite", "wasm-firefox", "cancelled", root)
                val cancellation = CancellationException("cancelled")
                assertSame(
                    cancellation,
                    assertFailsWith<CancellationException> {
                        cancelled.run { throw cancellation }
                    },
                )
                assertTrue(generatedCase(root, cancelled).resolve("index.html").readText().contains("CANCELLED"))
                val success = AppiumTestReport("suite", "wasm-chrome", "success", root)
                success.run { }
                assertTrue(generatedCase(root, success).resolve("index.html").readText().contains("PASSED"))
                assertFalse(success.directory.resolve("failure.txt").exists())
            }
        }

    @Test
    fun isolateConcurrentCases() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val reports =
                    coroutineScope {
                        (1..12).map {
                            async(Dispatchers.IO) {
                                AppiumTestReport("suite", "android", "case $it", root).also { report -> report.run { } }
                            }
                        }.awaitAll()
                    }
                assertEquals(12, reports.map { it.directory }.distinct().size)
                assertFalse(root.resolve("index.html").exists())
                val generated = generatedRoot(root)
                val index = generated.resolve("index.html").readText()
                reports.forEach {
                    assertTrue(
                        generated.resolve(it.directory.relativeTo(root)).resolve("index.html").isFile,
                    )
                }
                assertTrue(index.contains("PASSED"))
                assertTrue(
                    AppiumTestReport("other suite", "android", "case 1", root).directory !=
                        reports.first().directory,
                )
            }
        }

    @Test
    fun cleanPrivateStateAfterSetupFailure() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val report = AppiumTestReport("suite", "desktop", "setup failure", root)
                val privateSession = report.temporaryPath("session.json")
                val runtime = report.temporaryPath("runtime").also { it.mkdirs() }
                privateSession.writeText("synthetic private state")
                runtime.resolve("storage").writeText("synthetic storage")
                assertFalse(privateSession.toPath().startsWith(root.toPath()))
                assertFailsWith<IllegalStateException> { report.run { error("setup failed") } }
                assertFalse(privateSession.exists())
                assertFalse(runtime.exists())
                val html = generatedCase(root, report).resolve("index.html").readText()
                assertFalse(html.contains("synthetic private state"))
                assertTrue(html.contains("setup failed"))
            }
        }

    @Test
    fun preserveFailureWhenReportCannotBeWritten() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val report = AppiumTestReport("suite", "desktop", "broken report", root)
                report.directory.resolve("result.properties").mkdir()
                val original = AssertionError("test failure")
                val actual = assertFailsWith<AssertionError> { report.run { throw original } }
                assertSame(original, actual)
                assertEquals(1, actual.suppressed.size)
            }
        }

    @Test
    fun finishReportAfterJobCancellation() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val report = AppiumTestReport("suite", "android", "cancelled job", root)
                coroutineScope {
                    val started = CompletableDeferred<Unit>()
                    val job =
                        launch {
                            report.run {
                                started.complete(Unit)
                                awaitCancellation()
                            }
                        }
                    started.await()
                    job.cancel()
                    job.join()
                }
                assertTrue(generatedCase(root, report).resolve("index.html").readText().contains("CANCELLED"))
            }
        }

    @Test
    fun retainOtherArtifactsWhenDriverCaptureFails() =
        runAppiumBlockingTest {
            withReportRoot { root ->
                val report = AppiumTestReport("suite", "wasm-edge", "capture", root)
                val driver =
                    Proxy.newProxyInstance(
                        WebDriver::class.java.classLoader,
                        arrayOf(WebDriver::class.java, TakesScreenshot::class.java),
                    ) { _, method, _ ->
                        when (method.name) {
                            "getPageSource" -> "<html>page</html>"
                            "getScreenshotAs" -> error("screenshot unavailable")
                            else -> error("Unexpected driver call")
                        }
                    } as WebDriver
                report.run {
                    report.captureDriver(driver)
                    report.directory.resolve("recording # &.mp4").writeBytes(byteArrayOf(1))
                }
                assertEquals("<html>page</html>", report.directory.resolve("page-source.xml").readText())
                val html = generatedCase(root, report).resolve("index.html").readText()
                assertTrue(html.contains("screenshot unavailable"))
                assertTrue(html.contains("recording%20%23%20%26.mp4.html"))
                assertTrue(html.contains("PASSED"))
            }
        }

    private fun generatedRoot(raw: File): File =
        raw.parentFile.resolve("${raw.name}-html").also {
            generateTestArtifactReports(raw, it)
        }

    private fun generatedCase(raw: File, report: AppiumTestReport): File =
        generatedRoot(raw).resolve(report.directory.relativeTo(raw).path)

    private suspend fun withReportRoot(block: suspend (File) -> Unit) {
        val parent = File("build/test/appium-report-tests").also { it.mkdirs() }
        val root = Files.createTempDirectory(parent.toPath(), "report-").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
            root.parentFile.resolve("${root.name}-html").deleteRecursively()
        }
    }
}
