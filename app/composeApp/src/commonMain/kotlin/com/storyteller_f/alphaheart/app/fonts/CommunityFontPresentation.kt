/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.fonts

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import com.storyteller_f.alphaheart.client.compose_core.theme.TopicContentFonts
import com.storyteller_f.shared.model.FileInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val LocalCommunityFontStore =
    staticCompositionLocalOf<CommunityFontStore?> { null }

internal data class CommunityFontPresentation(val fonts: TopicContentFonts, val resolver: FontFamily.Resolver? = null)

internal fun combineCommunityFonts(body: LoadedCommunityFont, code: LoadedCommunityFont): CommunityFontPresentation =
    CommunityFontPresentation(
        TopicContentFonts(body.family, code.family, code.family ?: body.family),
        body.resolver ?: code.resolver,
    )

internal fun observeCommunityFont(file: FileInfo, store: CommunityFontStore): Flow<LoadedCommunityFont> =
    store.observeFont(file).map { (it as? CommunityFontState.Ready)?.bundle?.loaded ?: LoadedCommunityFont(null) }
