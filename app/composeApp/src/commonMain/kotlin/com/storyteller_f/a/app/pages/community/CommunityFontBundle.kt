/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.toImmutableList
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import no.synth.kmpzip.kotlinx.asSource
import no.synth.kmpzip.zip.ZipFile
import no.synth.kmpzip.zip.isZip

internal data class CommunityFontFace(
    val name: String,
    val bytes: ByteArray,
    val weight: FontWeight,
    val style: FontStyle,
)

internal class CommunityFontBundle(val identity: String, faces: List<CommunityFontFace>) {
    val faces = faces.toImmutableList()
    val byteCount = faces.sumOf { it.bytes.size.toLong() }
}

internal const val MAX_FONT_SOURCE_BYTES = 64L * 1024 * 1024
internal const val MAX_FONT_DECODED_BYTES = 80L * 1024 * 1024
private const val MAX_FONT_FACES = 18

/** ZIP families retain Regular/Bold/etc names; standalone font files are regular faces. No disk IO. */
internal fun decodeCommunityFont(identity: String, fileName: String, bytes: ByteArray): CommunityFontBundle {
    require(bytes.isNotEmpty() && bytes.size <= MAX_FONT_SOURCE_BYTES) { "Unsupported font size" }
    val faces =
        if (isZip(bytes)) {
            ZipFile(bytes).use { archive ->
                val entries = archive.entries.filter { !it.isDirectory && isFontName(it.name) }
                require(entries.size in 1..MAX_FONT_FACES) { "Font archive has no usable family or too many faces" }
                var remaining = MAX_FONT_DECODED_BYTES
                entries.map { entry ->
                    require(entry.size in 1..remaining) { "Font archive exceeds the decoded size limit" }
                    val data = archive.getInputStream(entry).asSource().buffered().use { it.readFontBytes(remaining) }
                    remaining -= data.size
                    createFontFace(entry.name, data)
                }
            }
        } else {
            require(isFontName(fileName)) { "Unsupported font format" }
            listOf(createFontFace(fileName, bytes))
        }
    return CommunityFontBundle(identity, faces)
}

private fun Source.readFontBytes(limit: Long): ByteArray {
    val output = Buffer()
    while (!exhausted()) {
        val count = readAtMostTo(output, minOf(8192, limit - output.size + 1))
        check(count > 0) { "Font archive made no progress" }
        require(output.size <= limit) { "Font archive exceeds the decoded size limit" }
    }
    return output.readByteArray()
}

internal fun isFontName(name: String): Boolean =
    name.substringAfterLast('.').lowercase() in listOf("ttf", "otf", "ttc", "woff", "woff2")

internal fun createFontFace(name: String, bytes: ByteArray): CommunityFontFace {
    val fileName = name.replace('\\', '/').substringAfterLast('/')
    val face = fileName.substringBeforeLast('.').substringAfterLast('-').lowercase()
    val weight =
        when (face.removeSuffix("italic")) {
            "thin" -> FontWeight.Thin
            "extralight" -> FontWeight.ExtraLight
            "light" -> FontWeight.Light
            "medium" -> FontWeight.Medium
            "semibold" -> FontWeight.SemiBold
            "bold" -> FontWeight.Bold
            "extrabold" -> FontWeight.ExtraBold
            "black" -> FontWeight.Black
            else -> FontWeight.Normal
        }
    return CommunityFontFace(name, bytes, weight, if (face.endsWith("italic")) FontStyle.Italic else FontStyle.Normal)
}
