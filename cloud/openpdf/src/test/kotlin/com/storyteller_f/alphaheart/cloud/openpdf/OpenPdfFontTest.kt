/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.alphaheart.cloud.pdf.PdfGenerationSpec
import com.storyteller_f.alphaheart.cloud.pdf.SnapshotGeneration
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.UserInfo
import kotlinx.datetime.LocalDateTime
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.apache.pdfbox.text.PDFTextStripper
import org.openpdf.text.Document
import org.openpdf.text.Paragraph
import org.openpdf.text.pdf.PdfWriter
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class OpenPdfFontTest {
    @Test
    fun `all font formats embed and extract bilingual text`() {
        for (kind in listOf("static", "variable", "cff2")) {
            val bundle = createBundle(kind)
            val file = output("$kind-bilingual.pdf")
            file.outputStream().buffered().use { stream ->
                Document().use { document ->
                    PdfWriter.getInstance(document, stream)
                    document.open()
                    document.add(Paragraph("Regular 中文测试 English", bundle.plainFont()))
                    document.add(Paragraph("Bold 中文测试 English", bundle.boldFont()))
                    document.add(Paragraph("Mono Regular Wi 012345", bundle.monoFont()))
                    document.add(Paragraph("Mono Bold Wi 012345", bundle.monoBoldFont()))
                }
            }
            Loader.loadPDF(file).use { document ->
                val text = PDFTextStripper().getText(document)
                assertTrue(text.contains("Regular 中文测试 English"), text)
                assertTrue(text.contains("Bold 中文测试 English"), text)
                assertTrue(text.contains("Mono Regular Wi 012345"), text)
                val resources = document.getPage(0).resources
                val fonts = resources.fontNames.map(resources::getFont)
                assertEquals(4, fonts.size)
                fonts.forEach { font ->
                    assertTrue(font.isEmbedded, font.name)
                    val program = font.fontDescriptor.fontFile2 ?: font.fontDescriptor.fontFile3
                    assertTrue(program != null, font.name)
                }
                val image = PDFRenderer(document).renderImageWithDPI(0, 144f)
                ImageIO.write(image, "png", output(file.nameWithoutExtension + ".png"))
            }
        }
    }

    @Test
    fun `real bold outlines render darker than regular`() {
        for (kind in listOf("static", "variable", "cff2")) {
            val bundle = createBundle(kind)
            val file = output("$kind-weights.pdf")
            file.outputStream().buffered().use { stream ->
                Document().use { document ->
                    PdfWriter.getInstance(document, stream)
                    document.open()
                    document.add(Paragraph("中文字体 Regular Bold English", bundle.plainFont()))
                    document.newPage()
                    document.add(Paragraph("中文字体 Regular Bold English", bundle.boldFont()))
                }
            }
            Loader.loadPDF(file).use { document ->
                val renderer = PDFRenderer(document)
                val darkness =
                    (0..1).map { page ->
                        val image = renderer.renderImageWithDPI(page, 144f)
                        ImageIO.write(image, "png", output(file.nameWithoutExtension + "-$page.png"))
                        (0 until image.height).sumOf { y ->
                            (0 until image.width).sumOf { x -> 255L - (image.getRGB(x, y) and 0xff) }
                        }
                    }
                assertTrue(darkness[1] > darkness[0] * 1.15, "Bold must use heavier outlines: $darkness")
            }
        }
    }

    @Test
    fun `mono advances and document ownership are correct`() {
        for (kind in listOf("static", "variable", "cff2")) {
            val first = createBundle(kind)
            val second = createBundle(kind)
            assertNotSame(first.plainFont(), first.plainFont())
            assertNotSame(first.plainFont().baseFont, second.plainFont().baseFont)
            assertFalse(first.plainFont().baseFont.isFontSpecific)
            for (font in listOf(first.monoFont(), first.monoBoldFont())) {
                assertEquals(font.baseFont.getWidth('i'.code), font.baseFont.getWidth('W'.code))
                assertEquals(font.baseFont.getWidth('i'.code), font.baseFont.getWidth('0'.code))
            }
        }
    }

    @Test
    fun `snapshot embeds installed Chinese fonts`() {
        val file = output("snapshot.pdf")
        val timestamp = LocalDateTime.parse("2026-01-01T00:00:00")
        OpenPdf().generateSignedSnapshot(
            UserInfo.EMPTY,
            UserInfo.EMPTY,
            "# 中文标题 English\n\n普通正文 中文测试 English **加粗文字 Bold** `代码 code`",
            emptyMap(),
            SnapshotGeneration.SimpleGeneration(file),
            PdfGenerationSpec(timestamp, timestamp),
        ).getOrThrow()
        Loader.loadPDF(file).use { document ->
            val text = PDFTextStripper().getText(document)
            for (expected in listOf("中文标题 English", "普通正文 中文测试 English", "加粗文字 Bold", "代码 code")) {
                assertTrue(text.contains(expected), text)
            }
            for (page in document.pages) {
                page.resources.fontNames.forEach { assertTrue(page.resources.getFont(it).isEmbedded) }
            }
            ImageIO.write(PDFRenderer(document).renderImageWithDPI(0, 144f), "png", output("snapshot.png"))
        }
    }

    private fun createBundle(kind: String): OpenPdfFontBundle =
        OpenPdfFontProvider.createBundle(
            if (kind != "static") {
                val source = if (kind == "cff2") "cff2-variable.otf" else "variable.ttf"
                PdfFontFamily(null, null, fontBytes(source))
            } else {
                PdfFontFamily(
                    fontBytes("serif-regular.ttf"),
                    fontBytes("serif-bold.ttf"),
                    null,
                )
            },
            PdfFontFamily(fontBytes("mono-regular.ttf"), fontBytes("mono-bold.ttf"), null),
        ).getOrThrow()

    private fun output(name: String): File = File("build/test-output/fonts", name).apply { parentFile.mkdirs() }
}
