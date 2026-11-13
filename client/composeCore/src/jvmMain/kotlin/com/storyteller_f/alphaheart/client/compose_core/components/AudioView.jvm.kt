/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AudioViewEmbed(remoteMediaItem: RemoteMediaItem) {
    DesktopMedia(remoteMediaItem, Modifier, video = false)
}

@Composable
actual fun AudioViewFilled(remoteMediaItem: RemoteMediaItem) {
    DesktopMedia(remoteMediaItem, Modifier.fillMaxSize(), video = false)
}

@Composable
actual fun AudioViewFullScreen(remoteMediaItem: RemoteMediaItem) {
    DesktopMedia(remoteMediaItem, Modifier.fillMaxSize(), video = false)
}
