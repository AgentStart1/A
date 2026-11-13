/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class E2eCoverageTest {
    @Test
    fun restartCollectsCurrentProcessAndRetainsOldSnapshot(): Unit =
        runBlocking {
            withCoverageEnabled {
                val root = createProcessFixture()
                val control = root.resolve("control")
                val output = root.resolve("output")
                val old = createCompletedProcess(control, 101)
                E2eCoverage.collectJvm(control, output, ":app:desktopApp", setOf(101))
                // A forced driver shutdown cannot run the application's shutdown hook.
                old.resolve("running").writeText("force-stopped")
                createCompletedProcess(control, 102)

                E2eCoverage.collectJvm(control, output, ":app:desktopApp", setOf(102))

                assertEquals("snapshot-101", output.resolve("101/report.ic").readText())
                assertEquals("snapshot-102", output.resolve("102/report.ic").readText())
                assertEquals("complete", output.resolve("status").readText())
                assertTrue(old.resolve("complete").isFile)
                assertFalse(old.resolve("request").exists())
            }
        }

    @Test
    fun missingCurrentProcessCannotReuseOldSnapshot(): Unit =
        runBlocking {
            withCoverageEnabled {
                val root = createProcessFixture()
                val control = root.resolve("control")
                val output = root.resolve("output")
                createCompletedProcess(control, 101)

                assertFailsWith<IllegalStateException> {
                    E2eCoverage.collectJvm(control, output, ":app:desktopApp", emptySet())
                }

                assertEquals("incomplete", output.resolve("status").readText())
                assertTrue(output.resolve("collection-error.txt").isFile)
                assertFalse(output.resolve("101").exists())
            }
        }

    @Test
    fun missingSnapshotPreservesFailure() {
        val directory = File("build/coverage-tests/missing").apply { mkdirs() }
        val failure =
            assertFailsWith<IllegalStateException> {
                E2eCoverage.collect(directory) {
                    E2eCoverage.copyReport(directory.resolve("missing.ic"), directory.resolve("report.ic"))
                }
            }
        assertEquals("Missing or empty coverage binary", failure.message)
        assertEquals("incomplete", directory.resolve("status").readText())
        assertTrue(directory.resolve("collection-error.txt").readText().contains(failure.message.orEmpty()))
    }

    @Test
    fun rejectsSnapshotFromAnotherApplicationVariant() {
        val directory = File("build/coverage-tests/variant").apply { mkdirs() }
        val descriptor = directory.resolve("input.properties")
        descriptor.writeText("classSet=:panel:cliApp\nbuildId=${"a".repeat(64)}\n")
        assertFailsWith<IllegalStateException> {
            E2eCoverage.copyDescriptor(descriptor, directory, ":app:cliApp")
        }
    }

    private fun createProcessFixture(): File =
        Files.createTempDirectory(File("build/coverage-tests").apply { mkdirs() }.toPath(), "processes-").toFile()

    private fun createCompletedProcess(root: File, processId: Long): File =
        root.resolve(processId.toString()).apply {
            mkdirs()
            resolve("ready").writeText("ready")
            resolve("complete").writeText("complete")
            resolve("report.ic").writeText("snapshot-$processId")
            resolve("classes.properties").writeText("classSet=:app:desktopApp\nbuildId=${"a".repeat(64)}\n")
        }

    private inline fun withCoverageEnabled(block: () -> Unit) {
        val previous = System.getProperty("e2e.coverage")
        System.setProperty("e2e.coverage", "true")
        try {
            block()
        } finally {
            previous?.let { System.setProperty("e2e.coverage", it) } ?: System.clearProperty("e2e.coverage")
        }
    }
}
