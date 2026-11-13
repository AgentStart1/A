/*
 * This is a private project. All rights reserved.
 */

import com.storyteller_f.alphaheart.coverage.CoverageRuntime
import kotlinx.kover.offline.runtime.api.KoverRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import javax.tools.ToolProvider

class E2eCoverageControlTest {
    @Test
    fun snapshotsLiveProcessTwiceAndExportsBeforeShutdown() {
        val root = File(System.getProperty("coverage.test.directory"), "process-control").apply { resetCoverageDirectory(this) }
        val control = root.resolve("control")
        resetCoverageDirectory(control)
        val source = root.resolve("ControlFixture.java")
        source.writeText("""
            package coveragefixture;
            public class ControlFixture {
                public static void main(String[] args) throws Exception {
                    while (!java.nio.file.Files.exists(java.nio.file.Path.of(args[0]))) Thread.sleep(10);
                }
            }
        """.trimIndent())
        val input = root.resolve("input").apply { mkdirs() }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "-g", "-d", input.path, source.path))
        val originals = root.resolve("originals").apply { mkdirs() }
        val files = E2eCoverageFiles(setOf(source), originals, root.resolve("instrumented"))
        files.prepare(listOf(input))
        val output = root.resolve("output")
        files.copy(input, output)
        output.resolve("e2e-coverage.properties").writeBytes(createCoverageDescriptor(originals, ":fixture", setOf(source)))
        val classpath = listOf(output, File(CoverageRuntime::class.java.protectionDomain.codeSource.location.toURI()),
            File(KoverRuntime::class.java.protectionDomain.codeSource.location.toURI())).joinToString(File.pathSeparator)
        val stop = root.resolve("stop")
        stop.delete()
        val executable = File(System.getProperty("java.home"), "bin/java${if (File.separatorChar == '\\') ".exe" else ""}")
        val process = ProcessBuilder(executable.path, "-De2e.coverage.directory=${control.absolutePath}",
            "-cp", classpath, "coveragefixture.ControlFixture", stop.absolutePath)
            .redirectErrorStream(true).redirectOutput(root.resolve("process.log")).start()
        try {
            val directory = control.resolve(process.pid().toString())
            await { directory.resolve("ready").isFile }
            repeat(2) {
                directory.resolve("complete").delete()
                val request = directory.resolve("request.pending")
                request.writeText("snapshot")
                Files.move(request.toPath(), directory.resolve("request").toPath(), StandardCopyOption.ATOMIC_MOVE)
                await { directory.resolve("complete").isFile || directory.resolve("error").isFile }
                assertFalse(root.resolve("process.log").readText(), directory.resolve("error").exists())
                assertTrue(directory.resolve("report.ic").length() > 0)
            }
            stop.writeText("stop")
            assertTrue(process.waitFor(10, TimeUnit.SECONDS))
            assertEquals(root.resolve("process.log").readText(), 0, process.exitValue())
            assertTrue(directory.resolve("complete").isFile)
            assertFalse(directory.resolve("running").exists())
            assertFalse(directory.resolve("error").exists())
        } finally {
            if (process.isAlive) process.destroyForcibly().waitFor()
        }
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (!condition()) {
            check(System.nanoTime() < deadline) { "Coverage control did not acknowledge the request" }
            Thread.sleep(10)
        }
    }
}
