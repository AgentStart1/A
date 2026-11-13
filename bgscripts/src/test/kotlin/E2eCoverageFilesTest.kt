/*
 * This is a private project. All rights reserved.
 */

import com.storyteller_f.alphaheart.coverage.CoverageRuntime
import kotlinx.kover.features.jvm.ClassFilters
import kotlinx.kover.features.jvm.KoverLegacyFeatures
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import java.io.File
import java.net.URLClassLoader
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.tools.ToolProvider

class E2eCoverageFilesTest {
    @Test
    fun sourceChangesInvalidateIdenticalBytecode() {
        val root = File(System.getProperty("coverage.test.directory"), "source-identity")
        resetCoverageDirectory(root)
        val source = root.resolve("Fixture.kt").apply { writeText("class Fixture") }
        val originals = root.resolve("originals").apply { mkdirs() }
        originals.resolve("Fixture.class").writeBytes(ClassWriter(0).apply {
            visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "Fixture", null, "java/lang/Object", null)
            visitEnd()
        }.toByteArray())
        fun identity(): String = Properties().apply {
            createCoverageDescriptor(originals, ":fixture", setOf(source)).inputStream().buffered().use(::load)
        }.getProperty("buildId")
        val before = identity()
        source.appendText("\n// A source-only edit must not reuse an earlier snapshot identity.\n")
        assertNotEquals(before, identity())
    }

    @Test
    fun retainsExternalJarSignaturesAndResources() {
        val root = File(System.getProperty("coverage.test.directory"), "external-jar")
        resetCoverageDirectory(root)
        val input = root.resolve("external.jar")
        val bytes = ClassWriter(0).apply {
            visit(Opcodes.V21, Opcodes.ACC_PUBLIC, "external/Foreign", null, "java/lang/Object", null)
            visitEnd()
        }.toByteArray()
        ZipOutputStream(input.outputStream().buffered()).use { jar ->
            mapOf("META-INF/provider.SF" to "signature metadata".toByteArray(),
                "external/Foreign.class" to bytes).forEach { (name, content) ->
                jar.putNextEntry(ZipEntry(name))
                jar.write(content)
                jar.closeEntry()
            }
        }
        val output = root.resolve("copied.jar")
        E2eCoverageFiles(emptySet(), root.resolve("originals"), root.resolve("instrumented")).copy(input, output)
        assertArrayEquals(input.readBytes(), output.readBytes())
    }

    @Test
    fun recordsBranchesDuringConcurrentSnapshots() {
        val directory = File(System.getProperty("coverage.test.directory"), "branches").apply { resetCoverageDirectory(this) }
        val source = directory.resolve("Work.java")
        source.writeText("""
            package coveragefixture;
            public class Work {
                public static int evaluate(boolean condition) {
                    if (condition) {
                        return 73;
                    } else {
                        return 19;
                    }
                }
                public static int unvisited() {
                    return -1;
                }
            }
        """.trimIndent())
        val input = directory.resolve("input").apply { mkdirs() }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "-g", "-d", input.path, source.path))
        val originals = directory.resolve("originals").apply { mkdirs() }
        val output = directory.resolve("output")
        val files = E2eCoverageFiles(setOf(source), originals, directory.resolve("instrumented"))
        files.prepare(listOf(input))
        files.copy(input, output)
        assertArrayEquals(input.resolve("coveragefixture/Work.class").readBytes(),
            originals.resolve("coveragefixture/Work.class").readBytes())
        assertFalse(input.resolve("coveragefixture/Work.class").readBytes()
            .contentEquals(output.resolve("coveragefixture/Work.class").readBytes()))
        val binary = directory.resolve("report.ic")
        URLClassLoader(arrayOf(output.toURI().toURL()), javaClass.classLoader).use { loader ->
            val evaluate = loader.loadClass("coveragefixture.Work").getMethod("evaluate", Boolean::class.javaPrimitiveType)
            assertEquals(73, evaluate.invoke(null, true))
            val executor = Executors.newSingleThreadExecutor()
            try {
                val writer = executor.submit { repeat(10_000) { evaluate.invoke(null, true) } }
                repeat(5) { CoverageRuntime.saveReport(binary) }
                writer.get(10, TimeUnit.SECONDS)
            } finally {
                executor.shutdownNow()
            }
            CoverageRuntime.saveReport(binary)
            val xml = directory.resolve("coverage.xml")
            KoverLegacyFeatures.generateXmlReport(xml, listOf(binary), listOf(originals), listOf(directory), "fixture",
                ClassFilters(emptySet(), emptySet(), emptySet(), emptySet(), emptySet(), emptySet()))
            val report = xml.readText()
            assertTrue(report.contains("name=\"coveragefixture/Work\""))
            val document = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml)
            val lines = document.getElementsByTagName("line")
            val hits = (0 until lines.length).associate { index ->
                val line = lines.item(index) as org.w3c.dom.Element
                line.getAttribute("nr").toInt() to line.getAttribute("ci").toInt()
            }
            assertTrue(checkNotNull(hits[5]) > 0)
            assertEquals(0, hits[7])
            assertEquals(0, hits[11])
            assertFalse(binary.length() == 0L)
        }
    }

    @Test
    fun retainsAbstractInterfaceContract() {
        val directory = File(System.getProperty("coverage.test.directory"), "filtered").apply { resetCoverageDirectory(this) }
        val source = directory.resolve("Contract.java")
        source.writeText("package coveragefixture; public interface Contract { int value(); }")
        val input = directory.resolve("input").apply { mkdirs() }
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null,
            "-g", "-d", input.path, source.path))
        val output = directory.resolve("output")
        val originals = directory.resolve("originals")
        val files = E2eCoverageFiles(setOf(source), originals, directory.resolve("instrumented"))
        files.prepare(listOf(input))
        files.copy(input, output)
        assertArrayEquals(input.resolve("coveragefixture/Contract.class").readBytes(),
            originals.resolve("coveragefixture/Contract.class").readBytes())
        URLClassLoader(arrayOf(output.toURI().toURL()), javaClass.classLoader).use { loader ->
            val contract = loader.loadClass("coveragefixture.Contract")
            assertTrue(contract.isInterface)
            assertEquals(Int::class.javaPrimitiveType, contract.getMethod("value").returnType)
        }
    }

}
