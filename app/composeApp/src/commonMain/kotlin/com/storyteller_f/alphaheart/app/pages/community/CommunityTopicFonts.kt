/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.client.compose_core.components.LocalTopicContentFonts
import com.storyteller_f.alphaheart.client.compose_core.components.TopicContentFonts
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FontSettingsWithInfo
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

internal val LocalCommunityFontStore =
    staticCompositionLocalOf<CommunityFontStore> {
        error("CommunityFontStore must be provided")
    }

internal data class CommunityFontPresentation(val fonts: TopicContentFonts, val resolver: FontFamily.Resolver? = null)

internal fun combineCommunityFonts(body: LoadedCommunityFont, code: LoadedCommunityFont): CommunityFontPresentation =
    CommunityFontPresentation(
        TopicContentFonts(body.family, code.family, code.family ?: body.family),
        body.resolver ?: code.resolver,
    )

@Composable
internal fun topicCommunityId(topic: TopicDetail): PrimaryKey? =
    when (topic.rootType) {
        ObjectType.COMMUNITY -> topic.rootId

        ObjectType.ROOM -> {
            val room = topic.roomInfo
            if (room != null) {
                room.communityId
            } else {
                val roomModel = createRoomViewModel(topic.rootId)
                val loadedRoom by roomModel.handler.data.collectAsState()
                loadedRoom?.communityId
            }
        }

        else -> null
    }

/** Keep uploaded font registration and content styles local to each topic. */
@Composable
internal fun CommunityTopicContent(communityId: PrimaryKey?, content: @Composable () -> Unit) {
    val settings =
        communityId?.let {
            val model = createCommunityViewModel(it)
            val community by model.handler.data.collectAsState()
            community?.fontSettings
        }
    CommunityFontContent(settings, LocalCommunityFontStore.current, content)
}

@Composable
internal fun CommunityFontContent(
    settings: FontSettingsWithInfo?,
    store: CommunityFontStore,
    content: @Composable () -> Unit,
) {
    val presentation = rememberCommunityFonts(settings, store)
    CompositionLocalProvider(
        LocalTopicContentFonts provides presentation.fonts,
        LocalFontFamilyResolver provides (presentation.resolver ?: LocalFontFamilyResolver.current),
        content = content,
    )
}

@Composable
private fun rememberCommunityFonts(
    settings: FontSettingsWithInfo?,
    store: CommunityFontStore,
): CommunityFontPresentation {
    val body = rememberCommunityFont(settings?.contentFont, store)
    val code = rememberCommunityFont(settings?.codeFont, store)
    return combineCommunityFonts(body, code)
}

@Composable
private fun rememberCommunityFont(file: FileInfo?, store: CommunityFontStore): LoadedCommunityFont =
    key(store, file?.let(::fontFileKey)) {
        val fonts = remember { file?.let { observeCommunityFont(it, store) } ?: flowOf(LoadedCommunityFont(null)) }
        val loaded by fonts.collectAsState(LoadedCommunityFont(null))
        loaded
    }

internal fun observeCommunityFont(file: FileInfo, store: CommunityFontStore): Flow<LoadedCommunityFont> =
    store.observeFont(file).map { (it as? CommunityFontState.Ready)?.bundle?.loaded ?: LoadedCommunityFont(null) }
