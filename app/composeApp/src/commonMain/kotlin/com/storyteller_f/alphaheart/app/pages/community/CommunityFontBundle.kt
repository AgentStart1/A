/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.collections.immutable.toImmutableList

internal data class LoadedCommunityFont(val family: FontFamily?, val resolver: FontFamily.Resolver? = null)

internal data class CommunityFontFace(val name: String, val weight: FontWeight, val style: FontStyle)

internal class CommunityFontBundle(faces: List<CommunityFontFace>, val loaded: LoadedCommunityFont) {
    val faces = faces.toImmutableList()
}

internal const val MAX_FONT_SOURCE_BYTES = 64L * 1024 * 1024
internal const val MAX_FONT_DECODED_BYTES = 80L * 1024 * 1024
internal fun isFontName(name: String): Boolean =
    name.substringAfterLast('.').lowercase() in listOf("ttf", "otf", "ttc", "woff", "woff2")

internal fun createFontFace(name: String): CommunityFontFace {
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
    return CommunityFontFace(name, weight, if (face.endsWith("italic")) FontStyle.Italic else FontStyle.Normal)
}
