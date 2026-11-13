/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun AudioViewEmbed(remoteMediaItem: RemoteMediaItem) {
    WasmMediaPlayer(remoteMediaItem, Modifier, video = false)
}

@Composable
actual fun AudioViewFilled(remoteMediaItem: RemoteMediaItem) {
    WasmMediaPlayer(remoteMediaItem, Modifier.fillMaxSize(), video = false)
}

@Composable
actual fun AudioViewFullScreen(remoteMediaItem: RemoteMediaItem) {
    WasmMediaPlayer(remoteMediaItem, Modifier.fillMaxSize(), video = false)
}
