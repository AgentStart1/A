/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import java.io.File
import java.time.Instant
import java.util.Properties
import kotlin.coroutines.cancellation.CancellationException

/** Raw artifact root supplied by the owning Gradle Test task. */
fun testArtifactRoot(): File = File(System.getProperty("test.artifacts.dir", "build/test-results/test/artifacts"))

enum class TestArtifactStatus { PASSED, FAILED, CANCELLED }

data class TestArtifactResult(
    val suite: String,
    val testName: String,
    val platform: String,
    val status: TestArtifactStatus,
    val started: Instant,
    val finished: Instant = Instant.now(),
)

/** Stores raw metadata only; HTML rendering belongs to the Gradle report finalizer. */
fun writeTestArtifactResult(directory: File, result: TestArtifactResult) {
    val properties =
        Properties().apply {
            setProperty("suite", result.suite)
            setProperty("testName", result.testName)
            setProperty("platform", result.platform)
            setProperty("status", result.status.name)
            setProperty("started", result.started.toString())
            setProperty("finished", result.finished.toString())
        }
    directory.resolve("result.properties").outputStream().buffered().use { properties.store(it, null) }
}

/** Records success, failure or cancellation after fixture cleanup, retaining the original exception. */
suspend fun <T> recordTestArtifacts(directory: File, suite: String, testName: String, block: suspend () -> T): T =
    TestArtifactRecording(directory, suite, testName).use { recording ->
        try {
            block()
        } catch (cancellation: CancellationException) {
            recording.recordFailure(cancellation)
            throw cancellation
        } catch (cause: Throwable) {
            recording.recordFailure(cause)
            throw cause
        }
    }

private class TestArtifactRecording(
    private val directory: File,
    private val suite: String,
    private val testName: String,
) : AutoCloseable {
    private val started = Instant.now()
    private var failure: Throwable? = null

    fun recordFailure(cause: Throwable) {
        failure = cause
    }

    override fun close() {
        val status =
            when (failure) {
                null -> TestArtifactStatus.PASSED
                is CancellationException -> TestArtifactStatus.CANCELLED
                else -> TestArtifactStatus.FAILED
            }
        failure?.let { directory.resolve("failure.txt").writeText(it.stackTraceToString()) }
        writeTestArtifactResult(directory, TestArtifactResult(suite, testName, "", status, started))
    }
}
