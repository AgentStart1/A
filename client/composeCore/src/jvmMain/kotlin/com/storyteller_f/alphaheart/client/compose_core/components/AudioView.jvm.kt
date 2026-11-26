/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlActions
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlState
import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem

@Composable
actual fun AudioViewEmbed(remoteMediaItem: RemoteMediaItem, loadOnPlay: Boolean) {
    var requested by remember(remoteMediaItem.url) { mutableStateOf(false) }
    if (!loadOnPlay || requested) {
        DesktopMedia(remoteMediaItem, Modifier, video = false, startPlaying = requested)
    } else {
        MediaPlayerSurface(
            remoteMediaItem.title ?: remoteMediaItem.name,
            MediaControlState(volumeEnabled = false, durationKnown = false),
            MediaControlActions({ requested = true }, {}, {}, {}),
            Modifier.fillMaxWidth().heightIn(min = if (remoteMediaItem.cover == null) 64.dp else 224.dp),
            video = false,
            sourceUrl = remoteMediaItem.url,
            artwork = remoteMediaItem.cover?.url,
        ) {}
    }
}

@Composable
actual fun AudioViewFilled(remoteMediaItem: RemoteMediaItem) {
    DesktopMedia(remoteMediaItem, Modifier.fillMaxSize(), video = false)
}
