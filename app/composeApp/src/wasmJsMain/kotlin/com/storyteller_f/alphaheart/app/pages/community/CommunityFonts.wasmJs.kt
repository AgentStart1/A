/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.LoadedFont
import com.storyteller_f.alphaheart.app.common.FileEngine
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.impl.use

@OptIn(ExperimentalTextApi::class)
internal actual suspend fun buildCommunityFont(
    identity: String,
    faces: List<CommunityFontFace>,
    files: FileEngine,
): LoadedCommunityFont {
    val validation = CommunityFontValidation(faces)
    val family =
        FontFamily(
            faces.map { face ->
                val bytes = files.read(face.name, MAX_FONT_DECODED_BYTES)
                // Compose silently falls back on invalid Skia data. Its public LoadedFont API retains
                // the data supplier, so these bytes live with the family, not a separate source cache.
                Data.makeFromBytes(bytes).use { data ->
                    checkNotNull(FontMgr.default.makeFromData(data)) { "Invalid font data" }.use { typeface ->
                        val names =
                            checkNotNull(typeface.getTableData("name")) { "Missing font naming table" }.use { it.bytes }
                        val metrics =
                            checkNotNull(typeface.getTableData("OS/2")) { "Missing OS/2 table" }.use { it.bytes }
                        validation.validate(face, names, metrics)
                    }
                }
                LoadedFont("$identity:${face.name}", { bytes }, face.weight, face.style)
            },
        )
    val resolver = createFontFamilyResolver()
    resolver.preload(family)
    return LoadedCommunityFont(family, resolver)
}
