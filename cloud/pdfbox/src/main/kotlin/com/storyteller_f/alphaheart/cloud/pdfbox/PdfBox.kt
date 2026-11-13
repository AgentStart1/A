package com.storyteller_f.alphaheart.cloud.pdfbox

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.map
import com.storyteller_f.result.firstOrNull
import com.storyteller_f.alphaheart.cloud.pdf.PdfError

import com.storyteller_f.alphaheart.cloud.pdf.PdfGenerationSpec
import com.storyteller_f.alphaheart.cloud.pdf.PdfService
import com.storyteller_f.alphaheart.cloud.pdf.SnapshotGeneration
import com.storyteller_f.alphaheart.cloud.pdf.collectPlainText
import com.storyteller_f.alphaheart.cloud.pdf.getFont
import com.storyteller_f.alphaheart.cloud.pdf.getMonoFont
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.utils.astNode
import com.storyteller_f.shared.utils.extractImageUrl
import com.storyteller_f.shared.utils.readCodeFence
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxThemes
import eu.europa.esig.dss.enumerations.DigestAlgorithm
import eu.europa.esig.dss.enumerations.SignatureLevel
import eu.europa.esig.dss.model.FileDocument
import eu.europa.esig.dss.pades.PAdESSignatureParameters
import eu.europa.esig.dss.pades.signature.PAdESService
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier
import eu.europa.esig.dss.token.Pkcs12SignatureToken
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.FontMappers
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.acceptChildren
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.ast.visitors.Visitor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import rst.pdfbox.layout.elements.Document
import rst.pdfbox.layout.elements.Frame
import rst.pdfbox.layout.elements.ImageElement
import rst.pdfbox.layout.elements.Paragraph
import rst.pdfbox.layout.shape.RoundRect
import rst.pdfbox.layout.text.StyledText
import rst.pdfbox.layout.text.TextFlow
import java.awt.Color
import java.io.File
import java.security.KeyStore

class PdfBox : PdfService {
    override fun generateSignedSnapshot(
        creatorInfo: UserInfo,
        authorInfo: UserInfo,
        content: String,
        map: Map<String, File>,
        snapshotGeneration: SnapshotGeneration,
        pdfGenerationSpec: PdfGenerationSpec
    ): CustomResult<Unit, PdfError> {
        val saveToFile = snapshotGeneration.path
        Document(50f, 50f, 50f, 50f).apply {
            val creatorId = if (creatorInfo.aid == null) creatorInfo.address else creatorInfo.aid
            val authorId = if (authorInfo.aid == null) authorInfo.address else authorInfo.aid
            val font = loadSystemFont(pdDocument)
            add(Paragraph().apply {
                addText("pub by $authorId", 14f, font)
            })
            add(Paragraph().apply {
                addText("pub at ${pdfGenerationSpec.created}", 14f, font)
            })
            add(Paragraph().apply {
                addText("capture by $creatorId", 14f, font)
            })
            add(Paragraph().apply {
                addText("capture at ${pdfGenerationSpec.captured}", 14f, font)
            })
            val parsedTree = astNode(content)
            parsedTree.accept(PdfBoxVisitor(font, content, map, this))
            save(saveToFile)
        }
        when (snapshotGeneration) {
            is SnapshotGeneration.KeyStoreGeneration -> {
                signPdfWithDss(saveToFile, snapshotGeneration, pdfGenerationSpec)
            }

            is SnapshotGeneration.SimpleGeneration -> Unit
        }
        return CustomResult.Success(Unit)
    }
}

private fun signPdfWithDss(
    saveToFile: File,
    snapshotGeneration: SnapshotGeneration.KeyStoreGeneration,
    pdfGenerationSpec: PdfGenerationSpec
) {
    // Create a copy with a blank page at the beginning for the signature
    // This preserves the original file unchanged
    val tempFileWithBlankPage = File.createTempFile("pdf_with_blank_page", ".pdf")
    try {
        insertBlankPageAtBeginning(saveToFile, tempFileWithBlankPage)

        val password = snapshotGeneration.password
        java.util.Base64.getDecoder().decode(snapshotGeneration.keyStoreBase64).inputStream().buffered().use { keyStoreInput ->
            val token = Pkcs12SignatureToken(
                keyStoreInput,
                KeyStore.PasswordProtection(password.toCharArray())
            )
            val key = token.keys.first()
            val parameters = PAdESSignatureParameters().apply {
                signingCertificate = key.certificate
                certificateChain = key.certificateChain.toList()
                signatureLevel = SignatureLevel.PAdES_BASELINE_B
                digestAlgorithm = DigestAlgorithm.SHA256
                bLevel().signingDate = java.util.Date(key.certificate.notBefore.time + 1000)
                val imageParameters = eu.europa.esig.dss.pades.SignatureImageParameters()
                val subjectName = key.certificate.certificate.subjectX500Principal.name
                val imageBytes = com.storyteller_f.alphaheart.cloud.pdf.SignatureImageGenerator.generate(
                    subjectName,
                    pdfGenerationSpec.created.toString(),
                    "Digitally Signed by PdfBox"
                )
                imageParameters.image = eu.europa.esig.dss.model.InMemoryDocument(
                    imageBytes,
                    "signature.png",
                    eu.europa.esig.dss.enumerations.MimeTypeEnum.PNG
                )
                val fieldParameters = eu.europa.esig.dss.pades.SignatureFieldParameters().apply {
                    page = 1
                    // Center the signature on the new first page
                    // Image is 600x90, so we maintain similar aspect ratio
                    val sigWidth = 500f
                    val sigHeight = 75f
                    // A4 page size: 595 x 842 points
                    originX = (595f - sigWidth) / 2
                    originY = (842f - sigHeight) / 2
                    width = sigWidth
                    height = sigHeight
                }
                imageParameters.fieldParameters = fieldParameters
                this.imageParameters = imageParameters
            }
            val service = PAdESService(CommonCertificateVerifier())
            val toSignDocument = FileDocument(tempFileWithBlankPage)
            val toBeSigned = service.getDataToSign(toSignDocument, parameters)
            val signatureValue = token.sign(toBeSigned, parameters.digestAlgorithm, key)
            val signedDocument = service.signDocument(toSignDocument, parameters, signatureValue)
            signedDocument.save(snapshotGeneration.signedFile.absolutePath)
        }
    } finally {
        tempFileWithBlankPage.delete()
    }
}

/**
 * Creates a copy of the source PDF with a new blank page inserted at the beginning.
 * The new page will have the same size as the original first page (defaults to A4 if no pages exist).
 * The original source file is not modified.
 */
private fun insertBlankPageAtBeginning(sourceFile: File, destinationFile: File) {
    org.apache.pdfbox.Loader.loadPDF(sourceFile).use { document ->
        val pageSize = if (document.numberOfPages > 0) {
            document.getPage(0).mediaBox
        } else {
            org.apache.pdfbox.pdmodel.common.PDRectangle.A4
        }
        val blankPage = org.apache.pdfbox.pdmodel.PDPage(pageSize)
        document.pages.insertBefore(blankPage, document.getPage(0))
        document.save(destinationFile)
    }
}

fun loadSystemFont(
    document: PDDocument,
): PDType0Font {
    val font = getFont()
    // 使用 PDFBox 加载字体
    return PDType0Font.load(document, FontMappers.instance().getTrueTypeFont(font.name, null).font, true)
}

data class PdfFontBundle(
    val plain: PDFont,
    val bold: PDFont,
    val italic: PDFont,
    val mono: PDFont
)

private fun loadFontBundle(document: PDDocument): PdfFontBundle {
    val base = getFont()
    val env = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
    val candidates = env.allFonts.filter { it.family == base.family }
    fun loadByName(f: java.awt.Font): PDFont = PDType0Font.load(
        document,
        FontMappers.instance().getTrueTypeFont(f.name, null).font,
        true
    )

    val bold = candidates.firstOrNull {
        it.name.contains("Bold") && !it.name.contains("Italic")
    } ?: throw IllegalStateException("bold font not found")
    val italic = candidates.firstOrNull {
        it.name.contains("Italic") && !it.name.contains("Bold")
    } ?: throw IllegalStateException("italic font not found")
    val mono = getMonoFont()
    return PdfFontBundle(
        loadByName(base),
        loadByName(bold),
        loadByName(italic),
        loadByName(mono)
    )
}

private fun headerFontSizeFor(type: IElementType): Float = when (type) {
    MarkdownElementTypes.ATX_1 -> 24f
    MarkdownElementTypes.ATX_2 -> 22f
    MarkdownElementTypes.ATX_3 -> 20f
    MarkdownElementTypes.ATX_4 -> 18f
    MarkdownElementTypes.ATX_5 -> 16f
    MarkdownElementTypes.ATX_6 -> 15f
    else -> 18f
}

class PdfBoxVisitor(
    val font: PDFont,
    val content: String,
    val map: Map<String, File>,
    val document: Document
) : Visitor {
    private val fontBundle: PdfFontBundle by lazy { loadFontBundle(document.pdDocument) }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun visitNode(node: ASTNode) {
        val type = node.type
        when (type) {
            MarkdownElementTypes.MARKDOWN_FILE -> node.acceptChildren(this)

            MarkdownElementTypes.PARAGRAPH -> {
                val p = Paragraph()
                val tf = TextFlow()
                node.acceptChildren(ParagraphVisitor(tf, content, fontBundle))
                p.add(tf)
                document.add(p)
            }

            MarkdownElementTypes.ATX_1,
            MarkdownElementTypes.ATX_2,
            MarkdownElementTypes.ATX_3,
            MarkdownElementTypes.ATX_4,
            MarkdownElementTypes.ATX_5,
            MarkdownElementTypes.ATX_6 -> {
                val headerText = collectPlainText(node, content)
                if (headerText.isNotBlank()) {
                    val size = headerFontSizeFor(type)
                    document.add(Paragraph().apply { addText(headerText, size, font) })
                }
            }

            MarkdownElementTypes.UNORDERED_LIST -> {
                val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }
                for (item in items) {
                    val text = collectPlainText(item, content)
                    if (text.isNotBlank()) {
                        document.add(Paragraph().apply { addText("• $text", 14f, font) })
                    }
                }
            }

            MarkdownElementTypes.ORDERED_LIST -> {
                val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }
                items.forEachIndexed { index, item ->
                    val text = collectPlainText(item, content)
                    if (text.isNotBlank()) {
                        document.add(Paragraph().apply {
                            addText(
                                "${index + 1}. $text",
                                14f,
                                font
                            )
                        })
                    }
                }
            }

            MarkdownElementTypes.CODE_FENCE -> {
                val code = readCodeFence(node, content).trimIndent()
                if (code.isNotBlank()) {
                    val codeParagraph = Paragraph().apply {
                        val tf = TextFlow()
                        addHighlightedCode(tf, code, fontBundle)
                        add(tf)
                    }
                    val frame = Frame(codeParagraph)
                    frame.shape = RoundRect(8f)
                    frame.backgroundColor = Color(0xF0, 0xF0, 0xF0)
                    frame.setPadding(10f, 10f, 8f, 8f)
                    frame.setMargin(6f, 6f, 6f, 6f)
                    document.add(frame)
                }
            }

            MarkdownElementTypes.CODE_BLOCK -> {
                val code = node.getTextInNode(content).toString().trimIndent()
                if (code.isNotBlank()) {
                    val codeParagraph = Paragraph().apply {
                        val tf = TextFlow()
                        tf.addText(code, 12f, fontBundle.mono)
                        add(tf)
                    }
                    val frame = Frame(codeParagraph)
                    frame.shape = RoundRect(8f)
                    frame.backgroundColor = Color(0xF0, 0xF0, 0xF0)
                    frame.setPadding(10f, 10f, 8f, 8f)
                    frame.setMargin(6f, 6f, 6f, 6f)
                    document.add(frame)
                }
            }

            MarkdownElementTypes.BLOCK_QUOTE -> {
                val flow = TextFlow()
                node.acceptChildren(QuoteVisitor(content, flow, fontBundle))
                val frame = Frame(Paragraph().apply { add(flow) })
                frame.shape = RoundRect(6f)
                frame.setPadding(10f, 10f, 8f, 8f)
                frame.setMargin(6f, 6f, 6f, 6f)
                document.add(frame)
            }

            MarkdownElementTypes.IMAGE -> {
                val name = extractImageUrl(node, content)
                document.add(ImageElement(map[name]!!.canonicalPath))
            }

            MarkdownTokenTypes.HORIZONTAL_RULE -> {
                document.add(
                    Paragraph().apply {
                        addText(
                            "________________________________________________________________",
                            12f,
                            font
                        )
                    }
                )
            }

            GFMElementTypes.STRIKETHROUGH -> {
                // Not supported in TextFlow easily, render as text
            }

            GFMElementTypes.TABLE -> {
                addTable(node, content, fontBundle, document)
            }
        }
    }
}

private const val CODE_FONT_SIZE = 12f

private fun addHighlightedCode(tf: TextFlow, code: String, fontBundle: PdfFontBundle) {
    val codeHighlights = Highlights.Builder()
        .theme(SyntaxThemes.atom(darkMode = false))
        .code(code)
        .build()
        .getHighlights()
        .sortedBy { it.location.start }

    if (codeHighlights.isEmpty()) {
        tf.addText(code, CODE_FONT_SIZE, fontBundle.mono)
        return
    }

    // Add text before first highlight
    if (codeHighlights.first().location.start > 0) {
        tf.addText(code.take(codeHighlights.first().location.start), CODE_FONT_SIZE, fontBundle.mono)
    }

    codeHighlights.forEachIndexed { i, highlight ->
        val color = if (highlight is ColorHighlight) {
            Color(highlight.rgb)
        } else {
            Color.BLACK
        }
        val endIndex = highlight.location.end
        tf.add(StyledText(code.substring(highlight.location.start, endIndex), CODE_FONT_SIZE, fontBundle.mono, color))

        // Add text between this highlight and next
        if (i != codeHighlights.lastIndex) {
            val nextStartIndex = codeHighlights[i + 1].location.start
            if (nextStartIndex > endIndex) {
                tf.addText(code.substring(endIndex, nextStartIndex), CODE_FONT_SIZE, fontBundle.mono)
            }
        } else if (endIndex < code.length) {
            // Add remaining text after last highlight
            tf.addText(code.substring(endIndex), CODE_FONT_SIZE, fontBundle.mono)
        }
    }
}

private class ParagraphVisitor(
    private val flow: TextFlow,
    private val content: String,
    private val fontBundle: PdfFontBundle
) : Visitor {
    override fun visitNode(node: ASTNode) {
        when (node.type) {
            MarkdownTokenTypes.TEXT -> {
                val text = node.getTextInNode(content).toString()
                flow.addText(text, 14f, fontBundle.plain)
            }

            MarkdownElementTypes.EMPH -> addStyledBlock(node, fontBundle.italic, 1)
            MarkdownElementTypes.STRONG -> addStyledBlock(node, fontBundle.bold, 2)

            MarkdownElementTypes.CODE_SPAN -> {
                val raw = node.getTextInNode(content).toString()
                val text = raw.replace("`", "").trim()
                flow.addText(text, 12f, fontBundle.mono)
            }

            MarkdownTokenTypes.WHITE_SPACE -> flow.addText(" ", 14f, fontBundle.plain)

            MarkdownElementTypes.INLINE_LINK,
            MarkdownElementTypes.FULL_REFERENCE_LINK,
            MarkdownElementTypes.SHORT_REFERENCE_LINK -> addLink(node)

            GFMElementTypes.STRIKETHROUGH -> {
                val text = node.getTextInNode(content).toString()
                val actual = text.substring(2, text.length - 2)
                flow.addText(actual, 14f, fontBundle.plain)
            }
        }
    }

    private fun addStyledBlock(node: ASTNode, font: PDFont, offset: Int) {
        val text = node.getTextInNode(content).toString()
        val actual = text.substring(offset, text.length - offset)
        flow.addText(actual, 14f, font)
    }

    private fun addLink(node: ASTNode) {
        val raw = node.getTextInNode(content).toString()
        val match = Regex("\\[([^]]+)]\\(([^)]+)\\)").find(raw)
        val text = match?.groupValues?.getOrNull(1)?.trim()
        if (!text.isNullOrEmpty()) {
            flow.addText(text, 14f, fontBundle.plain)
        } else {
            flow.addText(raw, 14f, fontBundle.plain)
        }
    }
}

class QuoteVisitor(private val content: String, val paragraph: TextFlow, val font: PdfFontBundle) :
    Visitor {
    override fun visitNode(node: ASTNode) {
        if (node.type == MarkdownTokenTypes.BLOCK_QUOTE) {
            node.acceptChildren(this)
        } else if (node.type == MarkdownElementTypes.PARAGRAPH) {
            val value = node.getTextInNode(content).toString()
                .trimMargin("> ").replace("\n", " ") + "\n\n"
            paragraph.addText(value, 14f, font.italic)
        }
    }
}

private const val TABLE_FONT_SIZE = 12f

private fun addTable(node: ASTNode, content: String, fontBundle: PdfFontBundle, document: Document) {
    val rows = mutableListOf<List<String>>()

    node.acceptChildren(object : Visitor {
        override fun visitNode(node: ASTNode) {
            when (node.type) {
                GFMElementTypes.HEADER -> {
                    val cells = node.children
                        .filter { it.type.name == "CELL" }
                        .map { it.getTextInNode(content).toString().trim() }
                    if (cells.isNotEmpty()) {
                        rows.add(cells)
                    }
                }
                GFMElementTypes.ROW -> {
                    val cells = node.children
                        .filter { it.type.name == "CELL" }
                        .map { it.getTextInNode(content).toString().trim() }
                    if (cells.isNotEmpty()) {
                        rows.add(cells)
                    }
                }
                else -> node.acceptChildren(this)
            }
        }
    })

    // Render table as formatted text rows
    rows.forEachIndexed { index, cells ->
        val isHeader = index == 0 && rows.size > 1
        val rowText = cells.joinToString(" | ")
        val paragraph = Paragraph()
        val tf = TextFlow()
        val font = if (isHeader) fontBundle.bold else fontBundle.plain
        tf.addText(rowText, TABLE_FONT_SIZE, font)
        paragraph.add(tf)

        // Wrap in frame for visual distinction
        val frame = Frame(paragraph)
        frame.backgroundColor = if (isHeader) Color(230, 230, 230) else Color.WHITE
        frame.setPadding(5f, 5f, 3f, 3f)
        frame.setMargin(0f, 0f, 0f, 0f)
        document.add(frame)

        // Add separator line after header
        if (isHeader) {
            val separator = Paragraph()
            val sepFlow = TextFlow()
            sepFlow.addText("-".repeat(rowText.length.coerceAtMost(80)), TABLE_FONT_SIZE, fontBundle.plain)
            separator.add(sepFlow)
            document.add(separator)
        }
    }
}
