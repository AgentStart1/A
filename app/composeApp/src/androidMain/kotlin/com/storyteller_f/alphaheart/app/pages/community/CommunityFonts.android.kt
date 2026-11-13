/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import com.storyteller_f.alphaheart.app.common.FileEngine
import com.storyteller_f.shared.getAppContextRefValue
import java.io.File

@OptIn(ExperimentalTextApi::class)
internal actual suspend fun buildCommunityFont(
    identity: String,
    faces: List<CommunityFontFace>,
    files: FileEngine,
): LoadedCommunityFont {
    val validation = CommunityFontValidation(faces)
    faces.forEach { face ->
        readAndroidFontMetadata(face, validation)
    }
    val context = checkNotNull(getAppContextRefValue()) { "Application context is not initialized" }
    val family =
        FontFamily(
            faces.map { face ->
                val font = Font(File(face.name), face.weight, face.style) as AndroidFont
                checkNotNull(font.typefaceLoader.loadBlocking(context, font)) { "Invalid font data" }
                font
            },
        )
    val resolver = createFontFamilyResolver(context)
    resolver.preload(family)
    return LoadedCommunityFont(family, resolver)
}
