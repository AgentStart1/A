/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.pages.community

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.LoadedFont
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.impl.use

@OptIn(ExperimentalTextApi::class)
internal actual suspend fun buildCommunityFont(bundle: CommunityFontBundle?): LoadedCommunityFont =
    buildSkiaCommunityFont(bundle, createFontFamilyResolver()) { loaded ->
        FontFamily(
            loaded.faces.map { face ->
                Data.makeFromBytes(face.bytes).use { data ->
                    checkNotNull(FontMgr.default.makeFromData(data)) { "Invalid font data" }.close()
                }
                LoadedFont("${loaded.identity}:${face.name}", { face.bytes }, face.weight, face.style)
            },
        )
    }
