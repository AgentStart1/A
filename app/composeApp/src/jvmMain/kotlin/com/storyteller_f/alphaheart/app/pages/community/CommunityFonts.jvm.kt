/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.platform.Font
import com.storyteller_f.alphaheart.app.common.FileEngine
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.impl.use
import java.io.File

@OptIn(ExperimentalTextApi::class)
internal actual suspend fun buildCommunityFont(
    identity: String,
    faces: List<CommunityFontFace>,
    files: FileEngine,
): LoadedCommunityFont {
    val validation = CommunityFontValidation(faces)
    // Compose silently substitutes a system face on decode failure; validate file input first.
    faces.forEach { face ->
        checkNotNull(FontMgr.default.makeFromFile(face.name)) { "Invalid font data" }.use { typeface ->
            val names = checkNotNull(typeface.getTableData("name")) { "Missing font naming table" }.use { it.bytes }
            val metrics = checkNotNull(typeface.getTableData("OS/2")) { "Missing font metrics table" }.use { it.bytes }
            validation.validate(face, names, metrics)
        }
    }
    val family =
        FontFamily(
            faces.map { face ->
                Font(File(face.name), face.weight, face.style)
            },
        )
    val resolver = createFontFamilyResolver()
    resolver.preload(family)
    return LoadedCommunityFont(family, resolver)
}
