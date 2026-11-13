/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import org.openpdf.text.Chunk
import org.openpdf.text.Font
import org.openpdf.text.pdf.BaseFont
import java.awt.Color

/** Installed font files supply sources; instantiated bytes alone are shared across documents. */
internal object OpenPdfFontProvider {
    private val repository = FontInstanceRepository()
    private val serif by lazy { PdfFontFamily.load("serif") }
    private val mono by lazy { PdfFontFamily.load("mono") }

    fun createBundle(): CustomResult<OpenPdfFontBundle, FontError> =
        captureFontFailure {
            createBundle(serif, mono).getOrThrow()
        }

    fun createBundle(
        serifSource: PdfFontFamily,
        monoSource: PdfFontFamily,
    ): CustomResult<OpenPdfFontBundle, FontError> =
        captureFontFailure {
            OpenPdfFontBundle(
                createFont(serifSource, 400),
                createFont(serifSource, 700),
                createFont(monoSource, 400),
                createFont(monoSource, 700),
            )
        }

    private fun createFont(source: PdfFontFamily, weight: Int): BaseFont {
        val bytes = source.getInstance(repository, weight).getOrThrow().copyBytes()
        // Disable OpenPDF's global BaseFont cache: each document owns its mutable subset state.
        return BaseFont.createFont("instance.ttf", BaseFont.IDENTITY_H, true, false, bytes, null)
    }
}

/** Per-document base fonts; every styling call returns a fresh mutable Font. Italics use PDF obliquing. */
class OpenPdfFontBundle internal constructor(
    private val regular: BaseFont,
    private val bold: BaseFont,
    private val monoRegular: BaseFont,
    private val monoBold: BaseFont,
) {
    fun plainFont(): Font = styledFont(Font.NORMAL)
    fun boldFont(sizeBoost: Float = 0f): Font = Font(bold, 12f + sizeBoost, Font.NORMAL)
    fun italicFont(): Font = styledFont(Font.ITALIC)
    fun styledFont(style: Int): Font =
        Font(if (style and Font.BOLD != 0) bold else regular, 12f, style and Font.BOLD.inv())
    fun monoFont(): Font = Font(monoRegular, 12f, Font.NORMAL)
    fun monoFontWithColor(color: Color): Font = Font(monoRegular, 12f, Font.NORMAL, color)
    fun monoBoldFont(): Font = Font(monoBold, 12f, Font.NORMAL)
    fun linkFont(): Font = Font(regular, 12f, Font.UNDERLINE, Color.BLUE)

    /** Preserve missing code glyphs through an explicitly proportional body-font fallback. */
    internal fun codeChunks(text: String, font: Font = monoFont()): List<Chunk> {
        val fallback = if (font.baseFont === monoBold) bold else regular
        val chunks = mutableListOf<Chunk>()
        for (codePoint in text.codePoints().toArray()) {
            val base =
                if (Character.isWhitespace(codePoint) || font.baseFont.charExists(codePoint)) {
                    font.baseFont
                } else {
                    fallback
                }
            require(Character.isWhitespace(codePoint) || base.charExists(codePoint)) {
                "PDF code fonts do not contain U+${codePoint.toString(16).uppercase()}"
            }
            val value = String(Character.toChars(codePoint))
            if (chunks.lastOrNull()?.font?.baseFont === base) {
                chunks.last().append(value)
            } else {
                chunks.add(Chunk(value, Font(base, font.size, font.style, font.color)))
            }
        }
        return chunks
    }
}
