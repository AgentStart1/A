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
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SnapshotLayoutTest {
    @Test
    fun `snapshot separates provenance from topic content`() {
        val file =
            generate(
                "reading-sample",
                """
                # 一份可以长期阅读的主题

                中文正文与 English text 混排，保留 **关键结论 Bold** 与 `inline code`。

                ## 阅读与复现

                这份快照保留发布者与捕获者信息，正文按标题、段落与代码组织。

                ```kotlin
                val message = "你好，PDF"
                ```

                > A short quotation for context.

                ---

                归档结束 Archive complete.
                """.trimIndent(),
            )
        Loader.loadPDF(file).use { pdf ->
            val text = PDFTextStripper().getText(pdf)
            for (expected in listOf(
                "主题快照", "Published by", "author-example", "Captured by", "reader-example",
                "2026-01-01 10:00:00 UTC+00:00", "2026-01-02 12:30:45 UTC+00:00", "一份可以长期阅读的主题", "Archive complete.",
            )) {
                assertTrue(text.contains(expected), text)
            }
            assertFalse(text.contains(".123456789"), text)
            assertEquals("Topic snapshot", pdf.documentInformation.title)
            assertEquals(1, pdf.numberOfPages)
            render(pdf, file)
        }
    }

    @Test
    fun `long snapshots retain content and page numbers`() {
        val content =
            (1..65).joinToString("\n\n") { index ->
                "段落 $index. A topic snapshot keeps the full reading order across page breaks. 中文内容完整保留。"
            } + "\n\nEND OF TOPIC"
        val file = generate("multipage-reading", content)
        Loader.loadPDF(file).use { pdf ->
            assertTrue(pdf.numberOfPages > 1)
            for (page in 1..pdf.numberOfPages) {
                val text =
                    PDFTextStripper().apply {
                        startPage = page
                        endPage = page
                    }.getText(pdf)
                assertTrue(text.contains("Content page $page"), text)
                pdf.getPage(page - 1).resources.fontNames.forEach {
                    assertTrue(pdf.getPage(page - 1).resources.getFont(it).isEmbedded)
                }
            }
            val text = PDFTextStripper().getText(pdf)
            for (index in 1..65) assertTrue(text.contains("段落 $index."), "Missing paragraph $index")
            assertTrue(text.contains("END OF TOPIC"))
            render(pdf, file)
        }
    }

    @Test
    fun `code fences preserve highlighted and trailing text`() {
        val cases =
            listOf(
                "kotlin" to "val answer = 42\n// FINAL COMMENT",
                "mermaid" to "graph TD\nA-->B",
            )
        for ((language, code) in cases) {
            val file = generate("code-$language", "```$language\n$code\n```")
            Loader.loadPDF(file).use { pdf ->
                val text = PDFTextStripper().getText(pdf).replace("\r\n", "\n")
                for (line in code.lines()) assertTrue(text.contains(line), text)
                assertFalse(text.contains("=="), text)
            }
        }
    }

    @Test
    fun `signature page preserves content page numbering`() {
        val signed = generateSignedSnapshot("reading-signed", generationSpec())
        Loader.loadPDF(signed).use { pdf ->
            assertEquals(2, pdf.numberOfPages)
            assertEquals(1, pdf.signatureDictionaries.size)
            val signatureText = readSignatureAppearance(pdf)
            assertTrue(signatureText.contains("2026-01-01 10:00:00 UTC+00:00"), signatureText)
            assertFalse(signatureText.contains("2026-01-01T"), signatureText)
            assertFalse(signatureText.contains(".987654321"), signatureText)
            val text = PDFTextStripper().apply { startPage = 2 }.getText(pdf)
            assertTrue(text.contains("Signed content remains readable."), text)
            assertTrue(text.contains("Content page 1"), text)
            render(pdf, signed)
        }
    }

    private fun generate(name: String, content: String): File {
        val file = File("build/test-output/layout/$name.pdf").apply { parentFile.mkdirs() }
        OpenPdf().generateSignedSnapshot(
            UserInfo.EMPTY.copy(aid = "reader-example"),
            UserInfo.EMPTY.copy(aid = "author-example"),
            content,
            emptyMap(),
            SnapshotGeneration.SimpleGeneration(file),
            generationSpec(),
        ).getOrThrow()
        return file
    }

    private fun generationSpec(): PdfGenerationSpec =
        PdfGenerationSpec(
            LocalDateTime.parse("2026-01-01T10:00:00.987654321"),
            LocalDateTime.parse("2026-01-02T12:30:45.123456789"),
        )

    private fun render(pdf: org.apache.pdfbox.pdmodel.PDDocument, file: File) {
        val renderer = PDFRenderer(pdf)
        for (page in 0 until pdf.numberOfPages) {
            ImageIO.write(
                renderer.renderImageWithDPI(page, 110f),
                "png",
                File(file.parentFile, "${file.nameWithoutExtension}-$page.png"),
            )
        }
    }
}
