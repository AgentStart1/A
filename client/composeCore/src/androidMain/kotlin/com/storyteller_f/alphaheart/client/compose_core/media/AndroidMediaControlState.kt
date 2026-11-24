/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util

@androidx.annotation.OptIn(UnstableApi::class)
internal fun readMediaControls(player: Player): MediaControlState =
    MediaControlState(
        playing = !Util.shouldShowPlayButton(player),
        loading = player.playbackState == Player.STATE_BUFFERING,
        failed = player.playerError != null,
        ended = player.playbackState == Player.STATE_ENDED,
        enabled = player.playbackState != Player.STATE_IDLE || player.playerError == null,
        position = player.currentPosition.coerceAtLeast(0) / 1000.0,
        duration = player.duration.coerceAtLeast(0) / 1000.0,
        volume = player.volume,
        muted = player.volume == 0f,
        title = player.currentMediaItem?.mediaMetadata?.title?.toString(),
    )

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
