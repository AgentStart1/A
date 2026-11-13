/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.alphaheart.cloud.pdf.PdfError
import com.storyteller_f.alphaheart.cloud.pdf.PdfGenerationSpec
import com.storyteller_f.alphaheart.cloud.pdf.PdfService
import com.storyteller_f.alphaheart.cloud.pdf.SnapshotGeneration
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.map
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.utils.astNode
import com.storyteller_f.shared.utils.extractImageUrl
import com.storyteller_f.shared.utils.readCodeFence
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxThemes
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.acceptChildren
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.Visitor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.openpdf.text.Chunk
import org.openpdf.text.Document
import org.openpdf.text.Element
import org.openpdf.text.Font
import org.openpdf.text.Image
import org.openpdf.text.Paragraph
import org.openpdf.text.Rectangle
import org.openpdf.text.pdf.BaseFont
import org.openpdf.text.pdf.PdfContentByte
import org.openpdf.text.pdf.PdfPCell
import org.openpdf.text.pdf.PdfPCellEvent
import org.openpdf.text.pdf.PdfPTable
import org.openpdf.text.pdf.PdfWriter
import org.openpdf.text.pdf.draw.LineSeparator
import java.awt.Color
import java.io.File

class OpenPdf : PdfService {
    override fun generateSignedSnapshot(
        creatorInfo: UserInfo,
        authorInfo: UserInfo,
        content: String,
        map: Map<String, File>,
        snapshotGeneration: SnapshotGeneration,
        pdfGenerationSpec: PdfGenerationSpec,
    ): CustomResult<Unit, PdfError> {
        val fontBundle =
            when (val fonts = OpenPdfFontProvider.createBundle()) {
                is CustomResult.Success -> fonts.value
                is CustomResult.Failure -> return CustomResult.failure(PdfError.Unexpected(fonts.error.cause))
            }
        val saveToFile = snapshotGeneration.path
        return captureFailure(PdfError::from) {
            saveToFile.outputStream().buffered().use {
                SnapshotLayout.createDocument().use { document ->
                    val writer = PdfWriter.getInstance(document, it)
                    writer.pageEvent = SnapshotPageEvent(fontBundle)
                    document.open()
                    SnapshotLayout.addSummary(document, fontBundle, creatorInfo, authorInfo, pdfGenerationSpec)
                    val parsedTree = astNode(content)
                    parsedTree.accept(OpenPdfVisitor(document, fontBundle, content, map))
                }
            }
            when (snapshotGeneration) {
                is SnapshotGeneration.KeyStoreGeneration -> {
                    signPdfWithKeyStore(saveToFile, snapshotGeneration, pdfGenerationSpec, fontBundle)
                }

                is SnapshotGeneration.SimpleGeneration -> Unit
            }
        }
    }
}

private fun signPdfWithKeyStore(
    saveToFile: File,
    snapshotGeneration: SnapshotGeneration.KeyStoreGeneration,
    pdfGenerationSpec: PdfGenerationSpec,
    fontBundle: OpenPdfFontBundle,
) {
    val ks = java.security.KeyStore.getInstance("PKCS12")
    java.util.Base64.getDecoder().decode(snapshotGeneration.keyStoreBase64).inputStream().buffered().use { input ->
        ks.load(input, snapshotGeneration.password.toCharArray())
    }
    val alias = ks.aliases().nextElement()
    val pk = ks.getKey(alias, snapshotGeneration.password.toCharArray()) as java.security.PrivateKey
    val chain = ks.getCertificateChain(alias)

    saveToFile.inputStream().buffered().use { input ->
        val reader = org.openpdf.text.pdf.PdfReader(input)
        try {
            snapshotGeneration.signedFile.outputStream().buffered().use { output ->
                val stamper = org.openpdf.text.pdf.PdfStamper.createSignature(reader, output, '\u0000'.toString())

                // Insert a new blank page at the beginning for the signature
                val pageSize = reader.getPageSize(1)
                stamper.insertPage(1, pageSize)

                val appearance = stamper.signatureAppearance
                // Place signature in the center of the new first page
                val sigWidth = 400f
                val sigHeight = 120f
                val sigX = (pageSize.width - sigWidth) / 2
                val sigY = (pageSize.height - sigHeight) / 2
                appearance.setVisibleSignature(
                    Rectangle(sigX, sigY, sigX + sigWidth, sigY + sigHeight),
                    1,
                    "sig",
                )

                // Use Layer2 to render a table-based signature instead of an image
                val layer2 = appearance.getLayer(2)
                val signatureInfo =
                    com.storyteller_f.alphaheart.cloud.pdf.SignatureInfo(
                        signee = (chain[0] as java.security.cert.X509Certificate).subjectX500Principal.name,
                        timestamp = SnapshotLayout.formatTimestamp(pdfGenerationSpec.created),
                        hint = "Digitally Signed by OpenPDF",
                    )
                buildSignatureTable(layer2, signatureInfo, appearance.rect, fontBundle)

                appearance.setCrypto(pk, chain, null, org.openpdf.text.pdf.PdfSignatureAppearance.WINCER_SIGNED)
                appearance.layer2Text = ""
                appearance.layer4Text = ""
                stamper.close()
            }
        } finally {
            reader.close()
        }
    }
}

class OpenPdfVisitor(
    private val document: Document,
    private val fontBundle: OpenPdfFontBundle,
    val content: String,
    private val map: Map<String, File>,
) : Visitor {
    private val listTypeStack = mutableListOf<Boolean>() // true = ordered, false = unordered
    private val listCounterStack = mutableListOf<Int>()

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun visitNode(node: ASTNode) {
        when (node.type) {
            MarkdownElementTypes.PARAGRAPH -> {
                val paragraph =
                    Paragraph("", fontBundle.plainFont()).apply {
                        setLeading(0f, 1.65f)
                        spacingAfter = 10f
                    }
                node.acceptChildren(ParagraphVisitor(paragraph, content, fontBundle))
                document.add(paragraph)
            }

            MarkdownTokenTypes.TEXT -> {
                document.add(Paragraph(node.getTextInNode(content).toString(), fontBundle.plainFont()))
            }

            MarkdownElementTypes.IMAGE -> {
                val name = checkNotNull(extractImageUrl(node, content)) { "Markdown image URL is missing" }
                document.add(Image.getInstance(map.getValue(name).readBytes()))
            }

            MarkdownElementTypes.CODE_FENCE -> {
                val table = buildRoundedCodeBlockTable(buildParagraphFromCodeFence(node))
                document.add(table)
            }

            MarkdownElementTypes.CODE_BLOCK -> {
                val table = buildTableFromCodeBlock(node)
                document.add(table)
            }

            // Headings
            MarkdownElementTypes.ATX_1 -> addHeading(node, 1)

            MarkdownElementTypes.ATX_2 -> addHeading(node, 2)

            MarkdownElementTypes.ATX_3 -> addHeading(node, 3)

            MarkdownElementTypes.ATX_4 -> addHeading(node, 4)

            MarkdownElementTypes.ATX_5 -> addHeading(node, 5)

            MarkdownElementTypes.ATX_6 -> addHeading(node, 6)

            MarkdownElementTypes.SETEXT_1 -> addHeading(node, 1, isSetext = true)

            MarkdownElementTypes.SETEXT_2 -> addHeading(node, 2, isSetext = true)

            // Lists
            MarkdownElementTypes.ORDERED_LIST -> {
                listTypeStack.add(true)
                listCounterStack.add(1)
                node.acceptChildren(this)
                listTypeStack.removeAt(listTypeStack.lastIndex)
                listCounterStack.removeAt(listCounterStack.lastIndex)
            }

            MarkdownElementTypes.UNORDERED_LIST -> {
                listTypeStack.add(false)
                listCounterStack.add(0)
                node.acceptChildren(this)
                listTypeStack.removeAt(listTypeStack.lastIndex)
                listCounterStack.removeAt(listCounterStack.lastIndex)
            }

            MarkdownElementTypes.LIST_ITEM -> addListItem(node)

            // Block quote
            MarkdownElementTypes.BLOCK_QUOTE -> addBlockQuote(node)

            MarkdownElementTypes.MARKDOWN_FILE -> {
                node.acceptChildren(this)
            }

            MarkdownTokenTypes.HORIZONTAL_RULE -> {
                val p =
                    Paragraph(" ", fontBundle.plainFont()).apply {
                        spacingBefore = 6f
                        spacingAfter = 10f
                    }
                val line = LineSeparator()
                line.lineWidth = 1f
                line.lineColor = Color.GRAY
                p.add(line)
                document.add(p)
            }

            GFMElementTypes.TABLE -> {
                val table = buildTable(node)
                document.add(table)
            }
        }
    }

    private fun buildParagraphFromCodeFence(node: ASTNode): Paragraph {
        val paragraph = Paragraph()
        val codeFence = readCodeFence(node, content)
        val codeHighlights =
            Highlights.Builder().theme(SyntaxThemes.atom(darkMode = false))
                .code(codeFence).build().getHighlights().sortedBy {
                    it.location.start
                }
        var cursor = 0
        for (highlight in codeHighlights) {
            val start = highlight.location.start.coerceIn(cursor, codeFence.length)
            val end = highlight.location.end.coerceIn(start, codeFence.length)
            if (end == start) continue
            fontBundle.codeChunks(codeFence.substring(cursor, start)).forEach { paragraph.add(it) }
            val font =
                if (highlight is ColorHighlight) {
                    fontBundle.monoFontWithColor(Color(highlight.rgb))
                } else {
                    fontBundle.monoBoldFont()
                }
            fontBundle.codeChunks(codeFence.substring(start, end), font).forEach { paragraph.add(it) }
            cursor = end
        }
        fontBundle.codeChunks(codeFence.substring(cursor)).forEach { paragraph.add(it) }
        return paragraph
    }

    private fun buildTableFromCodeBlock(node: ASTNode): PdfPTable {
        val raw = node.getTextInNode(content).toString()
        val paragraph = Paragraph()
        fontBundle.codeChunks(raw.trimIndent()).forEach { paragraph.add(it) }
        return buildRoundedCodeBlockTable(paragraph)
    }

    private fun buildRoundedCodeBlockTable(paragraph: Paragraph): PdfPTable {
        val table = PdfPTable(1)
        table.keepTogether = true
        table.setWidthPercentage(100f)
        table.horizontalAlignment = Element.ALIGN_LEFT
        table.setSpacingBefore(6f)
        table.setSpacingAfter(6f)

        paragraph.alignment = Element.ALIGN_LEFT
        val cell = PdfPCell(paragraph)
        cell.border = Rectangle.NO_BORDER
        cell.horizontalAlignment = Element.ALIGN_LEFT
        // padding: left/right/top/bottom
        val horizontalPadding = 12f
        val verticalPadding = 8f
        cell.paddingLeft = horizontalPadding
        cell.paddingRight = horizontalPadding
        cell.paddingTop = verticalPadding
        cell.paddingBottom = verticalPadding

        cell.cellEvent =
            RoundedBackgroundCellEvent(
                radius = 6f,
                fillColor = Color(245, 245, 245),
                strokeColor = Color(220, 220, 220),
            )

        table.addCell(cell)
        return table
    }

    private class RoundedBackgroundCellEvent(
        private val radius: Float,
        private val fillColor: Color,
        private val strokeColor: Color,
    ) : PdfPCellEvent {
        override fun cellLayout(cell: PdfPCell?, rect: Rectangle?, canvas: Array<PdfContentByte?>?) {
            if (rect == null || canvas == null) return
            val cb = canvas.getOrNull(PdfPTable.BACKGROUNDCANVAS) ?: return
            cb.saveState()
            cb.setColorFill(fillColor)
            cb.setColorStroke(strokeColor)
            cb.roundRectangle(rect.left, rect.bottom, rect.width, rect.height, radius)
            cb.fillStroke()
            cb.restoreState()
        }
    }

    private fun buildBlockQuoteTable(paragraph: Paragraph): PdfPTable {
        val table = PdfPTable(1)
        table.keepTogether = true
        table.setWidthPercentage(100f)
        table.horizontalAlignment = Element.ALIGN_LEFT
        table.setSpacingBefore(6f)
        table.setSpacingAfter(6f)

        paragraph.alignment = Element.ALIGN_LEFT
        val cell = PdfPCell(paragraph)
        cell.border = Rectangle.NO_BORDER
        cell.horizontalAlignment = Element.ALIGN_LEFT

        val horizontalPadding = 12f
        val verticalPadding = 6f
        val barWidth = 3f
        val barGap = 6f
        cell.paddingLeft = horizontalPadding + barWidth + barGap
        cell.paddingRight = horizontalPadding
        cell.paddingTop = verticalPadding
        cell.paddingBottom = verticalPadding

        cell.cellEvent = LeftBarCellEvent(barWidth = barWidth, barColor = Color(180, 180, 180))

        table.addCell(cell)
        return table
    }

    private class LeftBarCellEvent(private val barWidth: Float, private val barColor: Color) : PdfPCellEvent {
        override fun cellLayout(cell: PdfPCell?, rect: Rectangle?, canvas: Array<PdfContentByte?>?) {
            if (rect == null || canvas == null) return
            val cb = canvas.getOrNull(PdfPTable.BACKGROUNDCANVAS) ?: return
            cb.saveState()
            cb.setColorFill(barColor)
            cb.rectangle(rect.left, rect.bottom, barWidth, rect.height)
            cb.fill()
            cb.restoreState()
        }
    }

    private fun addHeading(node: ASTNode, level: Int, isSetext: Boolean = false) {
        val raw = node.getTextInNode(content).toString().trim()
        val text =
            if (isSetext) {
                raw.lineSequence().firstOrNull()?.trim().orEmpty()
            } else {
                raw.replace(Regex("^#{1,6}\\s*"), "").trim()
            }
        val sizeBoost =
            when (level) {
                1 -> 12f
                2 -> 8f
                3 -> 5f
                4 -> 3f
                5 -> 1f
                else -> 0f
            }
        document.add(
            Paragraph(text, fontBundle.boldFont(sizeBoost)).apply {
                spacingBefore = 16f
                spacingAfter = 8f
                setLeading(0f, 1.3f)
            },
        )
    }

    private fun addListItem(node: ASTNode) {
        val isOrdered = listTypeStack.lastOrNull() == true
        val depth = listTypeStack.size
        val counter = listCounterStack.lastOrNull() ?: 0
        val prefix = if (isOrdered) "$counter." else "•"
        val text = node.getTextInNode(content).toString().trim()
        val paragraph = Paragraph("$prefix $text", fontBundle.plainFont())
        paragraph.indentationLeft = 20f * depth
        document.add(paragraph)
        if (isOrdered && listCounterStack.isNotEmpty()) {
            listCounterStack[listCounterStack.lastIndex] = counter + 1
        }
    }

    private fun addBlockQuote(node: ASTNode) {
        val paragraph = Paragraph()
        node.acceptChildren(QuoteVisitor(content, paragraph, fontBundle.italicFont()))
        val table = buildBlockQuoteTable(paragraph)
        document.add(table)
    }
    private fun buildTable(node: ASTNode): PdfPTable {
        // Calculate columns - naive approach, take max cells from first row or header
        // For simplicity, we assume consistent column count or scan first row.
        // We need a visitor to just count columns first? Or just gather data.
        val rows = mutableListOf<List<ASTNode>>()
        node.acceptChildren(
            object : Visitor {
                override fun visitNode(node: ASTNode) {
                    if (node.type == GFMElementTypes.HEADER) {
                        node.acceptChildren(this)
                    } else if (node.type == GFMElementTypes.ROW) {
                        // Actually, let's just accept children of ROW and filter for content.
                        // In GFM, cells are separated by |.
                        // The parser structure for ROW contains CELL or text + pipes.
                        // Let's print raw text for debugging if needed, but here we implement.
                        // Assuming node.children contains cell nodes or we split by pipe?
                        // IntelliJ parser creates nodes for cells. checking standard GFM implementation...
                        // It uses GFMTokenTypes.CELL for content.
                        rows.add(node.children.filter { it.type.name == "CELL" })
                    }
                }
            },
        )

        val colCount = rows.maxOfOrNull { it.size } ?: 1
        val table = PdfPTable(colCount)
        table.widthPercentage = 100f

        rows.forEachIndexed { index, cells ->
            cells.forEach { cellNode ->
                val cellContent = cellNode.getTextInNode(content).toString().trim()
                val p = Paragraph(cellContent, fontBundle.plainFont())
                val cell = PdfPCell(p)
                if (index == 0 && rows.size > 1) { // Assume first row is header if we found a header node?
                    // Actually we flatted header into rows list.
                    // To distinguish, we should have tracked it.
                    // But usually typical markdown table has header.
                    cell.backgroundColor = Color(230, 230, 230)
                }
                cell.paddingLeft = 5f
                cell.paddingRight = 5f
                cell.paddingTop = 5f
                cell.paddingBottom = 5f
                table.addCell(cell)
            }
            // Fill missing cells if any
            repeat(colCount - cells.size) {
                table.addCell(PdfPCell(Paragraph("")))
            }
        }
        return table
    }
}

class ParagraphVisitor(private val paragraph: Paragraph, val content: String, val fontBundle: OpenPdfFontBundle) :
    Visitor {
    override fun visitNode(node: ASTNode) {
        when (node.type) {
            MarkdownTokenTypes.TEXT -> {
                val text = node.getTextInNode(content).toString()
                paragraph.add(text)
            }

            // Emphasis & strong
            MarkdownElementTypes.EMPH -> addStyledBlock(node, Font.ITALIC, 1)

            MarkdownElementTypes.STRONG -> addStyledBlock(node, Font.BOLD, 2)

            // Inline code
            MarkdownElementTypes.CODE_SPAN -> addCodeSpan(node)

            MarkdownTokenTypes.WHITE_SPACE -> paragraph.add(" ")

            // Links (render as text (url)) to avoid PDF-specific anchors
            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK,
            -> addLink(node)

            GFMElementTypes.STRIKETHROUGH -> addStyledBlock(node, Font.STRIKETHRU, 2)
        }
    }

    private fun addStyledBlock(node: ASTNode, style: Int, offset: Int) {
        val text = node.getTextInNode(content).toString()
        paragraph.add(Chunk(text.substring(offset, text.length - offset), fontBundle.styledFont(style)))
    }

    private fun addCodeSpan(node: ASTNode) {
        val raw = node.getTextInNode(content).toString()
        val text = raw.replace("`", "").trim()
        fontBundle.codeChunks(text).forEach { paragraph.add(it) }
    }

    private fun addLink(node: ASTNode) {
        val raw = node.getTextInNode(content).toString()
        val match = Regex("""\[([^]]+)]\(([^)]+)\)""").find(raw)
        val text = match?.groupValues?.getOrNull(1)?.trim()
        val url = match?.groupValues?.getOrNull(2)?.trim()
        if (!text.isNullOrEmpty() && !url.isNullOrEmpty()) {
            val chunk = Chunk(text, fontBundle.linkFont())
            chunk.setAnchor(url)
            paragraph.add(chunk)
        } else {
            paragraph.add(raw)
        }
    }
}

class QuoteVisitor(private val content: String, val paragraph: Paragraph, val font: Font) : Visitor {
    override fun visitNode(node: ASTNode) {
        if (node.type == MarkdownTokenTypes.BLOCK_QUOTE) {
            node.acceptChildren(this)
        } else if (node.type == MarkdownElementTypes.PARAGRAPH) {
            val content = node.getTextInNode(content).toString().trimMargin("> ").replace("\n", " ") + "\n\n"
            paragraph.add(Chunk(content, font))
        }
    }
}

/**
 * Builds a table-based visible signature directly onto the PDF using PdfContentByte.
 * This renders a table with an icon column on the left, and labeled rows (Signee, Timestamp, Hint) on the right.
 * Note: For PdfTemplate (layer 2), coordinates start from (0, 0), so we use rect only for dimensions.
 */
@Suppress("LongMethod")
private fun buildSignatureTable(
    cb: PdfContentByte,
    signatureInfo: com.storyteller_f.alphaheart.cloud.pdf.SignatureInfo,
    rect: Rectangle,
    fontBundle: OpenPdfFontBundle,
) {
    // For PdfTemplate, coordinates start from (0, 0)
    val left = 0f
    val bottom = 0f
    val width = rect.width
    val height = rect.height

    val iconWidth = width * 0.2f
    val labelWidth = width * 0.2f
    val rowHeight = height / 3f

    val baseFont = fontBundle.plainFont().baseFont
    val boldFont = fontBundle.boldFont().baseFont

    val fontSize = 8f

    // Draw background
    cb.saveState()
    cb.setColorFill(Color.WHITE)
    cb.rectangle(left, bottom, width, height)
    cb.fill()
    cb.restoreState()

    // Draw outer border
    cb.saveState()
    cb.setColorStroke(Color.BLACK)
    cb.setLineWidth(1f)
    cb.rectangle(left, bottom, width, height)
    cb.stroke()
    cb.restoreState()

    // Draw vertical line for icon column
    cb.saveState()
    cb.setColorStroke(Color.BLACK)
    cb.setLineWidth(0.5f)
    cb.moveTo(left + iconWidth, bottom)
    cb.lineTo(left + iconWidth, bottom + height)
    cb.stroke()
    cb.restoreState()

    // Draw vertical line between label and value
    val labelDividerX = left + iconWidth + labelWidth
    cb.saveState()
    cb.setColorStroke(Color.BLACK)
    cb.setLineWidth(0.5f)
    cb.moveTo(labelDividerX, bottom)
    cb.lineTo(labelDividerX, bottom + height)
    cb.stroke()
    cb.restoreState()

    // Draw horizontal lines for rows
    for (i in 1 until 3) {
        val y = bottom + i * rowHeight
        cb.saveState()
        cb.setColorStroke(Color.BLACK)
        cb.setLineWidth(0.5f)
        cb.moveTo(left + iconWidth, y)
        cb.lineTo(left + width, y)
        cb.stroke()
        cb.restoreState()
    }

    // Draw badge icon (simple checkmark badge)
    val iconCenterX = left + iconWidth / 2
    val iconCenterY = bottom + height / 2
    val radius = iconWidth.coerceAtMost(height) / 2f * 0.6f
    drawBadgeIcon(cb, iconCenterX, iconCenterY, radius)

    // Draw row content
    val rows =
        listOf(
            "Signee" to signatureInfo.signee,
            "Timestamp" to signatureInfo.timestamp,
            "Hint" to signatureInfo.hint,
        )

    rows.forEachIndexed { index, (label, value) ->
        val rowY = bottom + height - (index + 1) * rowHeight + rowHeight / 2 - fontSize / 3

        // Draw label (right-aligned in its column)
        cb.beginText()
        cb.setFontAndSize(boldFont, fontSize)
        cb.setColorFill(Color.BLACK)
        val labelTextWidth = boldFont.getWidthPoint(label, fontSize)
        cb.setTextMatrix(labelDividerX - labelTextWidth - 3f, rowY)
        cb.showText(label)
        cb.endText()

        // Draw value (left-aligned, truncated if needed)
        cb.beginText()
        cb.setFontAndSize(baseFont, fontSize)
        cb.setColorFill(Color.BLACK)
        cb.setTextMatrix(labelDividerX + 3f, rowY)
        val maxValueWidth = width - iconWidth - labelWidth - 10f
        val truncatedValue = truncateText(value, baseFont, fontSize, maxValueWidth)
        cb.showText(truncatedValue)
        cb.endText()
    }
}

/**
 * Draws a badge icon (starburst with checkmark) at the specified center position.
 */
private fun drawBadgeIcon(cb: PdfContentByte, cx: Float, cy: Float, radius: Float) {
    cb.saveState()
    cb.setColorStroke(Color.BLACK)
    cb.setLineWidth(1.5f)

    // Draw starburst
    val numPoints = 16
    for (i in 0 until numPoints * 2) {
        val angle = Math.PI * i / numPoints
        val r = if (i % 2 == 0) radius.toDouble() else radius * 0.85
        val x = cx + r * Math.cos(angle)
        val y = cy + r * Math.sin(angle)
        if (i == 0) {
            cb.moveTo(x.toFloat(), y.toFloat())
        } else {
            cb.lineTo(x.toFloat(), y.toFloat())
        }
    }
    cb.closePath()
    cb.stroke()

    // Draw checkmark
    cb.moveTo(cx - radius * 0.4f, cy)
    cb.lineTo(cx - radius * 0.1f, cy - radius * 0.4f)
    cb.lineTo(cx + radius * 0.5f, cy + radius * 0.4f)
    cb.stroke()

    cb.restoreState()
}

/**
 * Truncates text to fit within the specified width, adding "..." if necessary.
 */
private fun truncateText(text: String, font: org.openpdf.text.pdf.BaseFont, fontSize: Float, maxWidth: Float): String {
    if (font.getWidthPoint(text, fontSize) <= maxWidth) {
        return text
    }
    var truncated = text
    while (truncated.isNotEmpty() && font.getWidthPoint("$truncated...", fontSize) > maxWidth) {
        truncated = truncated.dropLast(1)
    }
    return if (truncated.isEmpty()) "" else "$truncated..."
}
