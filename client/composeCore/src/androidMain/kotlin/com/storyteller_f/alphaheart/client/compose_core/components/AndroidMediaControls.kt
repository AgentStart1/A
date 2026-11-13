/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun rememberMediaControls(player: MediaController): State<MediaControlState> =
    produceState(readMediaControls(player), player) {
        while (isActive) {
            value = readMediaControls(player)
            delay(250)
        }
    }

@androidx.annotation.OptIn(UnstableApi::class)
internal fun readMediaControls(player: Player): MediaControlState =
    MediaControlState(
        playing = !Util.shouldShowPlayButton(player),
        loading = player.playbackState == Player.STATE_BUFFERING,
        failed = player.playerError != null,
        enabled = player.playbackState != Player.STATE_IDLE || player.playerError == null,
        position = player.currentPosition.coerceAtLeast(0) / 1000.0,
        duration = player.duration.coerceAtLeast(0) / 1000.0,
        volume = player.volume,
        muted = player.volume == 0f,
        title = player.currentMediaItem?.mediaMetadata?.title?.toString(),
    )

@Composable
internal fun rememberMediaActions(player: MediaController): MediaControlActions =
    remember(player) { createMediaActions(player) }

@androidx.annotation.OptIn(UnstableApi::class)
internal fun createMediaActions(player: Player): MediaControlActions {
    val volume = MediaVolumeControl { level, muted -> player.volume = if (muted) 0f else level }
    return MediaControlActions(
        { Util.handlePlayPauseButtonAction(player) },
        { player.seekTo((it * 1000).toLong()) },
        volume::setVolume,
        { volume.toggleMute(player.volume, player.volume == 0f) },
        retry = { Util.handlePlayButtonAction(player) },
    )
}
