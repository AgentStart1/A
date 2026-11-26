/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem

@Composable
actual fun AudioViewEmbed(remoteMediaItem: RemoteMediaItem, loadOnPlay: Boolean) {
    DesktopMedia(remoteMediaItem, Modifier, video = false, loadOnPlay = loadOnPlay)
}

@Composable
actual fun AudioViewFilled(remoteMediaItem: RemoteMediaItem) {
    DesktopMedia(remoteMediaItem, Modifier.fillMaxSize(), video = false)
}
