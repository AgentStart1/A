/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import kotlinx.coroutines.flow.MutableStateFlow

actual abstract class MediaPlayerService {
    actual val state: MutableStateFlow<MediaPlaySession?> = MutableStateFlow(null)
    actual abstract val enablePip: Boolean
    actual abstract fun fullscreen(remoteMediaItem: RemoteMediaItem)
    actual abstract suspend fun start(
        remoteMediaItem: RemoteMediaItem,
        localMediaPlaySession: LocalMediaPlaySession,
        playList: List<ConstPlayItem>,
    )
}
