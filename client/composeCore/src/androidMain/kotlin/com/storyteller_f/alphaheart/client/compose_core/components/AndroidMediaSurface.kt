/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import android.app.PictureInPictureParams
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.media3.session.MediaController
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.media.LocalMediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.media.LocalMediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.media.MediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.media.MediaSurfaceLayout
import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.media.findActivity
import com.storyteller_f.alphaheart.client.compose_core.media_pip
import com.storyteller_f.alphaheart.client.compose_core.media_playlist
import org.jetbrains.compose.resources.stringResource
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
internal fun AndroidMediaSurface(
    item: RemoteMediaItem,
    player: MediaController,
    localSession: LocalMediaPlaySession,
    session: MediaPlaySession,
    modifier: Modifier,
    video: Boolean = true,
    controlsVisible: Boolean = true,
    layout: MediaSurfaceLayout = MediaSurfaceLayout.Bounded,
    content: @Composable BoxScope.() -> Unit,
) {
    val state by rememberMediaControls(player)
    val actions = rememberMediaActions(player)
    var showPlaylist by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val service = LocalMediaPlayerService.current
    key(item.url) {
        MediaPlayerSurface(
            item.title ?: item.name, state, actions,
            modifier.semantics {
                contentDescription =
                    "${if (video) "video" else "audio"} playback ${item.name}: ${if (state.playing) "playing" else "paused"}"
            },
            video, controlsVisible, sourceUrl = item.url, artwork = item.cover?.url, layout = layout,
            menuContent = { dismiss ->
                ButtonNav(actionIcon(ActionGlyph.LIST_RTL), stringResource(Res.string.media_playlist)) {
                    dismiss()
                    showPlaylist = true
                }
                if (service.enablePip) {
                    ButtonNav(actionIcon(ActionGlyph.PICTURE_IN_PICTURE), stringResource(Res.string.media_pip)) {
                        dismiss()
                        context.findActivity().enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                    }
                }
            }, content = content,
        )
    }
    VideoPlaylistPicker(
        showPlaylist,
        rememberModalBottomSheetState(),
        { showPlaylist = false },
        session.playList,
    ) { _, index ->
        if (session.lastUuid == localSession.uuid) {
            player.seekTo(index, 0)
            player.play()
        }
        showPlaylist = false
    }
}
