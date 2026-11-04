/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import android.graphics.fonts.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.Typeface
import io.github.aakira.napier.Napier
import java.nio.ByteBuffer
import android.graphics.Typeface as AndroidTypeface
import android.graphics.fonts.FontFamily as AndroidFontFamily
import android.graphics.fonts.FontStyle as AndroidFontStyle

internal actual suspend fun buildCommunityFont(bundle: CommunityFontBundle?): LoadedCommunityFont =
    LoadedCommunityFont(bundle?.toAndroidFontFamily())

private fun CommunityFontBundle.toAndroidFontFamily(): FontFamily? =
    try {
        val fonts =
            faces.distinctBy { it.weight to it.style }.map { face ->
                Font.Builder(
                    ByteBuffer.allocateDirect(face.bytes.size).apply {
                        put(face.bytes)
                        flip()
                    },
                ).setWeight(face.weight.weight)
                    .setSlant(
                        if (face.style ==
                            FontStyle.Italic
                        ) {
                            AndroidFontStyle.FONT_SLANT_ITALIC
                        } else {
                            AndroidFontStyle.FONT_SLANT_UPRIGHT
                        },
                    )
                    .build()
            }
        val family =
            AndroidFontFamily.Builder(fonts.first()).apply {
                fonts.drop(1).forEach { addFont(it) }
            }.build()
        FontFamily(Typeface(AndroidTypeface.CustomFallbackBuilder(family).build()))
    } catch (failure: Exception) {
        Napier.w(failure) { "Unable to decode a community font; using application typography." }
        null
    }
