/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.toImmutableList

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
