/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestArtifactReportsTest {
    @Test
    fun renderFailuresWithoutMutatingRawResults() =
        runBlocking {
            withWorkspace { root ->
                val raw = root.resolve("raw")
                val directory = createTestArtifactDirectory(raw, "ExampleTest", "failed case")
                val original = IllegalStateException("synthetic <failure>")
                assertSame(
                    original,
                    assertFailsWith<IllegalStateException> {
                        recordTestArtifacts(directory, "ExampleTest", "failed case") {
                            directory.resolve("server.log").writeText("startup <record>")
                            throw original
                        }
                    },
                )
                assertFalse(directory.resolve("index.html").exists())
                val output = root.resolve("report")
                output.mkdirs()
                output.resolve("stale.html").writeText("old report")
                generateTestArtifactReports(raw, output)
                val html = output.resolve("ExampleTest/failed case/index.html").readText()
                assertTrue(html.contains("FAILED"))
                assertTrue(html.contains("synthetic &lt;failure&gt;"))
                assertFalse(output.resolve("stale.html").exists())
                assertFalse(directory.resolve("artifact-views").exists())
                assertEquals("startup <record>", directory.resolve("server.log").readText())
            }
        }

    @Test
    fun retainCancellationWhenMetadataWriteFails() =
        runBlocking {
            withWorkspace { root ->
                val directory = createTestArtifactDirectory(root.resolve("raw"), "cancelled")
                directory.resolve("result.properties").mkdir()
                val cancellation = CancellationException("synthetic cancellation")
                val actual =
                    assertFailsWith<CancellationException> {
                        recordTestArtifacts(directory, "ExampleTest", "cancelled") { throw cancellation }
                    }
                assertSame(cancellation, actual)
                assertEquals(1, actual.suppressed.size)
            }
        }

    @Test
    fun rejectOverlappingDirectoriesBeforeCleanup() {
        withWorkspace { root ->
            val raw = createTestArtifactDirectory(root, "raw")
            raw.resolve("keep.log").writeText("keep")
            assertFailsWith<IllegalArgumentException> { generateTestArtifactReports(raw, root) }
            assertFailsWith<IllegalArgumentException> { generateTestArtifactReports(raw, raw.resolve("html")) }
            assertEquals("keep", raw.resolve("keep.log").readText())
        }
    }

    private inline fun withWorkspace(block: (File) -> Unit) {
        val parent = File("build/test/artifact-report-generator").also { it.mkdirs() }
        val root = Files.createTempDirectory(parent.toPath(), "case-").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
