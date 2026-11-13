/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.alphaheart.cloud.pdf.PdfGenerationSpec
import com.storyteller_f.shared.model.UserInfo
import kotlinx.datetime.LocalDateTime
import org.openpdf.text.Document
import org.openpdf.text.Element
import org.openpdf.text.PageSize
import org.openpdf.text.Paragraph
import org.openpdf.text.Phrase
import org.openpdf.text.Rectangle
import org.openpdf.text.pdf.ColumnText
import org.openpdf.text.pdf.PdfPCell
import org.openpdf.text.pdf.PdfPTable
import org.openpdf.text.pdf.PdfPageEventHelper
import org.openpdf.text.pdf.PdfWriter
import java.awt.Color

internal object SnapshotLayout {
    private val accent = Color(38, 73, 87)

    fun createDocument(): Document = Document(PageSize.A4, 48f, 48f, 60f, 54f)

    fun addSummary(
        document: Document,
        fonts: OpenPdfFontBundle,
        creator: UserInfo,
        author: UserInfo,
        spec: PdfGenerationSpec,
    ) {
        document.addTitle("Topic snapshot")
        document.add(
            Paragraph("主题快照", fonts.boldFont(14f).apply { color = accent }).apply {
                spacingAfter = 4f
            },
        )
        document.add(
            Paragraph(
                "TOPIC SNAPSHOT",
                fonts.plainFont().apply {
                    size = 10f
                    color = accent
                },
            ).apply {
                spacingAfter = 20f
            },
        )
        val metadata =
            PdfPTable(2).apply {
                widthPercentage = 100f
                setWidths(floatArrayOf(1f, 3f))
                setSpacingAfter(22f)
            }
        listOf(
            "Published by" to identity(author),
            "Published at" to formatTimestamp(spec.created),
            "Captured by" to identity(creator),
            "Captured at" to formatTimestamp(spec.captured),
        ).forEach { (label, value) ->
            metadata.addCell(summaryCell(label, fonts, label = true))
            metadata.addCell(summaryCell(value, fonts, label = false))
        }
        document.add(metadata)
    }

    fun formatTimestamp(value: LocalDateTime): String {
        val time = listOf(value.hour, value.minute, value.second).joinToString(":") { it.toString().padStart(2, '0') }
        return "${value.date} $time UTC+00:00"
    }

    private fun identity(user: UserInfo): String =
        user.aid?.takeIf { it.isNotBlank() } ?: user.address.takeIf { it.isNotBlank() } ?: "<none>"

    private fun summaryCell(text: String, fonts: OpenPdfFontBundle, label: Boolean): PdfPCell {
        val font =
            (if (label) fonts.boldFont() else fonts.plainFont()).apply {
                size = 10f
                color = if (label) accent else Color(45, 51, 56)
            }
        return PdfPCell(Phrase(text, font)).apply {
            border = Rectangle.NO_BORDER
            backgroundColor = Color(242, 246, 247)
            paddingLeft = 12f
            paddingRight = 12f
            paddingTop = 8f
            paddingBottom = 8f
        }
    }
}

/** Content page numbers remain valid when signing later inserts its separate signature page. */
internal class SnapshotPageEvent(private val fonts: OpenPdfFontBundle) : PdfPageEventHelper() {
    override fun onEndPage(writer: PdfWriter, document: Document) {
        val canvas = writer.directContent
        val font =
            fonts.plainFont().apply {
                size = 9f
                color = Color(100, 110, 115)
            }
        canvas.saveState()
        try {
            canvas.setColorStroke(Color(215, 223, 226))
            canvas.setLineWidth(0.5f)
            canvas.moveTo(document.left(), 42f)
            canvas.lineTo(document.right(), 42f)
            canvas.stroke()
            ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_LEFT,
                Phrase("TOPIC SNAPSHOT", font),
                document.left(),
                28f,
                0f,
            )
            ColumnText.showTextAligned(
                canvas,
                Element.ALIGN_RIGHT,
                Phrase("Content page ${writer.pageNumber}", font),
                document.right(),
                28f,
                0f,
            )
        } finally {
            canvas.restoreState()
        }
    }
}
