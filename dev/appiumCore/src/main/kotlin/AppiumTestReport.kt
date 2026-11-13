/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.test.TestArtifactResult
import com.storyteller_f.alphaheart.test.TestArtifactStatus
import com.storyteller_f.alphaheart.test.createTestArtifactDirectory
import com.storyteller_f.alphaheart.test.testArtifactRoot
import com.storyteller_f.alphaheart.test.writeTestArtifactResult
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.openqa.selenium.OutputType
import org.openqa.selenium.TakesScreenshot
import org.openqa.selenium.WebDriver
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/** Keeps only the latest artifacts for each suite, platform and test name. */
internal class AppiumTestReport(
    private val suite: String,
    private val platform: String,
    private val testName: String,
    root: File = testArtifactRoot(),
) {
    val directory: File = createTestArtifactDirectory(root, suite, testName, platform)
    private val started = Instant.now()
    private val collectionErrors = mutableListOf<String>()
    private val temporaryPaths = mutableListOf<File>()
    private val privateDirectory by lazy {
        val parent = File("build/test/appium/private").also { it.mkdirs() }
        Files.createTempDirectory(parent.toPath(), "runtime-").toFile().also { temporaryPaths.add(it) }
    }

    /** Private runtime state lives outside the report and is removed even when setup fails. */
    fun temporaryPath(name: String): File = privateDirectory.resolve(name)

    /** Collection must never replace a test failure or prevent subsequent collectors. */
    fun collect(label: String, block: () -> Unit) {
        try {
            block()
        } catch (failure: Exception) {
            collectionErrors.add("$label: ${failure.stackTraceToString()}")
        }
    }

    fun captureSnapshot(driver: WebDriver, name: String) {
        collect("Snapshot $name") {
            val output = createTestArtifactDirectory(directory.resolve("snapshots"), name)
            captureDriver(driver, output, labelPrefix = "Snapshot $name")
        }
    }

    fun captureDriver(driver: WebDriver, outputDirectory: File = directory, labelPrefix: String = "") {
        require(outputDirectory.toPath().normalize().startsWith(directory.toPath().normalize()))
        val prefix = labelPrefix.takeIf(String::isNotBlank)?.let { "$it " }.orEmpty()
        collect("${prefix}Page source") {
            val source = requireNotNull(driver.pageSource) { "Appium returned no page source" }
            outputDirectory.resolve("page-source.xml").writeText(source)
        }
        collect("${prefix}Screenshot") {
            val screenshot = driver as TakesScreenshot
            outputDirectory.resolve("screenshot.png").writeBytes(screenshot.getScreenshotAs(OutputType.BYTES))
        }
    }

    /** Coverage IO must complete before process teardown, including when the test was cancelled. */
    suspend fun collectSuspending(label: String, block: suspend () -> Unit) {
        try {
            withContext(NonCancellable) { block() }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            collectionErrors.add("$label: ${failure.stackTraceToString()}")
        }
    }

    suspend fun run(block: suspend () -> Unit) {
        var outcome: AppiumOutcome = AppiumOutcome.Running
        try {
            block()
            outcome = AppiumOutcome.Passed
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (cause: Throwable) {
            outcome = AppiumOutcome.Failed(cause)
            throw cause
        } finally {
            withContext(NonCancellable) { finish(outcome) }
        }
    }

    private fun finish(outcome: AppiumOutcome) {
        try {
            temporaryPaths.forEach { path ->
                collect("Temporary state cleanup") {
                    check(!path.exists() || path.deleteRecursively()) { "Unable to remove temporary state" }
                }
            }
            writeResult(outcome)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (reportFailure: Exception) {
            when (outcome) {
                is AppiumOutcome.Failed -> outcome.cause.addSuppressed(reportFailure)

                AppiumOutcome.Passed -> throw reportFailure

                AppiumOutcome.Running ->
                    org.slf4j.LoggerFactory.getLogger("appium-report")
                        .warn("Failed to write cancelled test report", reportFailure)
            }
        }
    }

    private fun writeResult(outcome: AppiumOutcome) {
        val failure = (outcome as? AppiumOutcome.Failed)?.cause
        if (failure != null) directory.resolve("failure.txt").writeText(failure.stackTraceToString())
        if (collectionErrors.isNotEmpty()) {
            directory.resolve("collection-errors.txt").writeText(collectionErrors.joinToString("\n"))
        }
        val status =
            when (outcome) {
                AppiumOutcome.Running -> TestArtifactStatus.CANCELLED
                AppiumOutcome.Passed -> TestArtifactStatus.PASSED
                is AppiumOutcome.Failed -> TestArtifactStatus.FAILED
            }
        writeTestArtifactResult(directory, TestArtifactResult(suite, testName, platform, status, started))
    }
}

private sealed interface AppiumOutcome {
    data object Running : AppiumOutcome
    data object Passed : AppiumOutcome
    data class Failed(val cause: Throwable) : AppiumOutcome
}
