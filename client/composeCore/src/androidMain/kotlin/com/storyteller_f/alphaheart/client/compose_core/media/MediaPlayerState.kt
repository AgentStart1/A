/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.session.MediaController

data class MediaPlayerState(
    val currentLoading: Boolean,
    val currentIsPlaying: Boolean,
    val currentPlayingItem: MediaItem?,
)

fun MediaController.playNewMedia(playList: List<ConstPlayItem>) {
    clearMediaItems()

    addMediaItems(
        playList.map { playItem ->
            val uri = playItem.url
            MediaItem.Builder().setUri(uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtworkUri(playItem.icon?.toUri())
                        .setTitle(playItem.title)
                        .build(),
                ).build()
        },
    )
    play()
}

internal fun buildListener(player: Player, listener: VideoListener): Player.Listener =
    object : Player.Listener {
        override fun onVideoSizeChanged(videoSize: VideoSize) {
            super.onVideoSizeChanged(videoSize)
            listener.onUpdateSize(CustomVideoSize(videoSize.width, videoSize.height))
        }

        override fun onIsLoadingChanged(isLoading: Boolean) {
            super.onIsLoadingChanged(isLoading)
            listener.onUpdateLoading(isLoading)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            listener.onPlayStateChange(isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            listener.onMediaItemChanged(mediaItem?.mediaId, player.currentMediaItemIndex)
        }
    }
