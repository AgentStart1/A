/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.getOrThrow
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.openpdf.text.Document
import org.openpdf.text.Font
import org.openpdf.text.Paragraph
import org.openpdf.text.pdf.BaseFont
import org.openpdf.text.pdf.PdfWriter
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Cff2VariationTest {
    @Test
    fun `multiple axes normalize omitted defaults`() {
        val source = fontBytes("cff2-multiaxis.otf")
        val axes = SfntFont(source, 0).axes
        assertTrue(axes.keys.containsAll(listOf("wght", "opsz")))
        assertEquals(20f, axes.getValue("opsz").default)
        val repository = FontInstanceRepository()
        val implicit = repository.getInstance(source, axes = mapOf("wght" to 700f)).getOrThrow()
        val explicit = axes.mapValues { (tag, axis) -> if (tag == "wght") 700f else axis.default }
        assertSame(
            implicit,
            repository.getInstance(source, axes = explicit.entries.reversed().associate { it.toPair() }).getOrThrow(),
        )
        assertStaticCff(implicit)
    }

    @Test
    fun `multiple nondefault axes change optical metrics`() {
        val source = fontBytes("cff2-multiaxis.otf")
        val optical = SfntFont(source, 0).axes.getValue("opsz")
        val repository = FontInstanceRepository()
        val instances =
            listOf(optical.minimum, optical.maximum).map { size ->
                repository.getInstance(source, axes = mapOf("wght" to 700f, "opsz" to size)).getOrThrow()
            }
        instances.forEach(::assertStaticCff)
        val widths = instances.map { baseFont(it).getWidth("Hamburgefontsiv Wi012345") }
        assertTrue(widths.all { it > 0 })
        assertNotEquals(widths[0], widths[1])
    }

    @Test
    fun `fixed optical size applies requested weights`() {
        val source = fontBytes("cff2-multiaxis.otf")
        val repository = FontInstanceRepository()
        val weights = listOf(400, 700)
        val instances =
            weights.map { weight ->
                repository.getInstance(source, axes = mapOf("opsz" to 8f, "wght" to weight.toFloat())).getOrThrow()
            }
        instances.forEach(::assertStaticCff)
        val file = output("cff2-fixed-opsz-weights.pdf")
        file.outputStream().buffered().use { stream ->
            Document().use { document ->
                PdfWriter.getInstance(document, stream)
                document.open()
                instances.forEachIndexed { index, instance ->
                    if (index != 0) document.newPage()
                    // Identical text and NORMAL PDF style isolate the actual instantiated outlines.
                    document.add(Paragraph("Hamburgefontsiv Wi012345", Font(baseFont(instance), 12f, Font.NORMAL)))
                }
            }
        }
        Loader.loadPDF(file).use { document ->
            assertEquals(2, document.numberOfPages)
            val renderer = PDFRenderer(document)
            val darkness =
                weights.indices.map { index ->
                    val resources = document.getPage(index).resources
                    resources.fontNames.forEach { name -> assertTrue(resources.getFont(name).isEmbedded) }
                    val image = renderer.renderImageWithDPI(index, 144f)
                    ImageIO.write(image, "png", output("cff2-fixed-opsz-${weights[index]}.png"))
                    (0 until image.height).sumOf { y ->
                        (0 until image.width).sumOf { x -> 255L - (image.getRGB(x, y) and 0xff) }
                    }
                }
            assertTrue(darkness[1] > darkness[0] * 1.15, "wght 700 must have heavier outlines at opsz 8: $darkness")
        }
        instances.forEachIndexed { index, instance ->
            assertEquals(weights[index], readFontWeight(instance.copyBytes()))
        }
    }

    @Test
    fun `CFF2 collection faces keep independent identities`() {
        val sources = listOf(fontBytes("cff2-multiaxis.otf"), fontBytes("cff2-collection.otf"))
        val collection = createFontCollection(sources)
        // Equal normalized axes make faceIndex the only difference in the collection cache keys.
        assertEquals(SfntFont(collection, 0).axes, SfntFont(collection, 1).axes)
        val repository = FontInstanceRepository()
        val instances =
            sources.indices.map {
                repository.getInstance(
                    collection,
                    it,
                    mapOf("wght" to 700f),
                ).getOrThrow()
            }
        assertNotSame(instances[0], instances[1])
        assertNotEquals(baseFont(instances[0]).postscriptFontName, baseFont(instances[1]).postscriptFontName)
        instances.forEachIndexed { index, instance ->
            assertStaticCff(instance)
            assertSame(instance, repository.getInstance(collection, index, mapOf("wght" to 700f)).getOrThrow())
            val standalone = repository.getInstance(sources[index], axes = mapOf("wght" to 700f)).getOrThrow()
            assertEquals(baseFont(standalone).postscriptFontName, baseFont(instance).postscriptFontName)
            assertEquals(
                baseFont(standalone).getWidth("Hamburgefontsiv"),
                baseFont(instance).getWidth("Hamburgefontsiv"),
            )
        }
    }

    @Test
    fun `CFF2 collection files preserve selected face`() {
        val root = Files.createDirectories(Path.of("build/tmp/cff2-collection")).toRealPath()
        val file = Files.createTempFile(root, "fonts-", ".otc").toFile()
        try {
            val italic = fontBytes("cff2-collection.otf")
            file.writeBytes(createFontCollection(listOf(fontBytes("cff2-multiaxis.otf"), italic)))
            val faces = InstalledFontMetadata.read(file)
            assertEquals(listOf(0, 1), faces.map { it.source.faceIndex })
            assertTrue(faces.all { it.variable })
            assertFalse(faces[0].italic)
            assertTrue(faces[1].italic)
            val family = PdfFontFamily.fromFiles(null, null, faces[1].source)
            val repository = FontInstanceRepository()
            val selected = family.getInstance(repository, 700).getOrThrow()
            val standalone = repository.getInstance(italic, axes = mapOf("wght" to 700f)).getOrThrow()
            assertEquals(baseFont(standalone).postscriptFontName, baseFont(selected).postscriptFontName)
            assertStaticCff(selected)
        } finally {
            check(file.toPath().toRealPath().startsWith(root))
            check(file.delete())
        }
    }

    private fun output(name: String): File = File("build/test-output/fonts", name).apply { parentFile.mkdirs() }

    private fun baseFont(instance: FontInstance): BaseFont =
        BaseFont.createFont("instance.otf", BaseFont.IDENTITY_H, true, false, instance.copyBytes(), null)

    private fun assertStaticCff(instance: FontInstance) {
        val metadata = SfntFont(instance.copyBytes(), 0)
        assertTrue(metadata.hasTable("CFF "))
        listOf("CFF2", "fvar", "gvar", "avar", "HVAR", "MVAR").forEach { assertFalse(metadata.hasTable(it), it) }
    }
}
