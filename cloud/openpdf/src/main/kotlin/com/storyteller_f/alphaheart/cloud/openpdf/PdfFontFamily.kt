/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import java.io.File

/** Static faces take precedence independently for each weight. Selected file contents load lazily. */
internal class PdfFontFamily private constructor(
    loadRegular: () -> FontFaceData?,
    loadBold: () -> FontFaceData?,
    loadVariable: () -> FontFaceData?,
) {
    constructor(regular: ByteArray?, bold: ByteArray?, variable: ByteArray?) : this(
        { regular?.let(::FontFaceData) },
        { bold?.let(::FontFaceData) },
        { variable?.let(::FontFaceData) },
    )

    private val regular by lazy(loadRegular)
    private val bold by lazy(loadBold)
    private val variable by lazy(loadVariable)

    fun getInstance(repository: FontInstanceRepository, weight: Int): CustomResult<FontInstance, FontError> =
        captureFontFailure {
            require(weight == 400 || weight == 700) { "PDF styles require Regular (400) or Bold (700)" }
            val static = if (weight == 400) regular else bold
            if (static != null) {
                require(!SfntFont(static.bytes, static.faceIndex).hasTable("fvar")) {
                    "Static PDF font slot contains a variable font"
                }
                repository.getInstance(static.bytes, static.faceIndex).getOrThrow()
            } else {
                val source = requireNotNull(variable) { "Missing PDF font face for weight $weight" }
                repository.getInstance(source.bytes, source.faceIndex, mapOf("wght" to weight.toFloat())).getOrThrow()
            }
        }

    companion object {
        fun load(
            name: String,
            directory: File? =
                System.getProperty(
                    "pdf.font.directory",
                )?.let(::File),
        ): PdfFontFamily =
            if (directory == null) {
                InstalledPdfFonts.load(name)
            } else {
                require(directory.isDirectory) { "PDF font directory does not exist" }
                PdfFontFamily(
                    { optionalFile(directory, "$name-regular") },
                    { optionalFile(directory, "$name-bold") },
                    { optionalFile(directory, "$name-variable") },
                )
            }

        fun fromFiles(regular: FontFileSource?, bold: FontFileSource?, variable: FontFileSource?): PdfFontFamily =
            PdfFontFamily({ regular?.readFace() }, { bold?.readFace() }, { variable?.readFace() })

        private fun FontFileSource.readFace(): FontFaceData = FontFaceData(readBytes(), faceIndex)

        private fun optionalFile(directory: File, name: String): FontFaceData? {
            val file = listOf("ttf", "otf").map { directory.resolve("$name.$it") }.firstOrNull { it.exists() }
            return file?.let { FontFileSource(it).readFace() }
        }
    }
}

internal data class FontFaceData(val bytes: ByteArray, val faceIndex: Int = 0)
