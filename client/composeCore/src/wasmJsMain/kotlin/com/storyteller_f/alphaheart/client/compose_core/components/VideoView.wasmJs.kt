/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VideoViewEmbed(remoteMediaItem: RemoteMediaItem) {
    WasmMediaPlayer(remoteMediaItem, Modifier.fillMaxWidth().aspectRatio(16f / 9f), video = true)
}

@Composable
actual fun VideoViewFilled(remoteMediaItem: RemoteMediaItem) {
    WasmMediaPlayer(remoteMediaItem, Modifier.fillMaxSize(), video = true)
}

@Composable
actual fun VideoViewFullScreen(remoteMediaItem: RemoteMediaItem) {
    WasmMediaPlayer(remoteMediaItem, Modifier.fillMaxSize(), video = true)
}

@Composable
actual fun rememberIsInPipMode(): Boolean = false
