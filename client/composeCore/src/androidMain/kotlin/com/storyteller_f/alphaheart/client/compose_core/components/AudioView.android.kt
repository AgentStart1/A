/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import android.util.Rational
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.session.MediaController
import io.github.aakira.napier.Napier
import kotlin.uuid.ExperimentalUuidApi

@Composable
actual fun AudioViewEmbed(remoteMediaItem: RemoteMediaItem) {
    MediaPlayerEmbed(remoteMediaItem) { playingSession, localMediaPlaySession ->
        AudioPlayer(playingSession, localMediaPlaySession, remoteMediaItem)
    }
}

@Composable
actual fun AudioViewFilled(remoteMediaItem: RemoteMediaItem) {
    MediaPlayerFilled(remoteMediaItem) { playingSession, localMediaPlaySession ->
        AudioPlayer(playingSession, localMediaPlaySession, remoteMediaItem, fill = true)
    }
}

@Composable
actual fun AudioViewFullScreen(remoteMediaItem: RemoteMediaItem) {
    MediaPlayerFullScreen(remoteMediaItem) { playingSession, localMediaPlaySession ->
        AudioPlayer(playingSession, localMediaPlaySession, remoteMediaItem, fill = true)
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun AudioPlayer(
    playingSession: MediaPlaySession?,
    localMediaPlaySession: LocalMediaPlaySession,
    remoteMediaItem: RemoteMediaItem,
    fill: Boolean = false,
) {
    val mediaPlayerService = LocalMediaPlayerService.current
    val player by mediaPlayerService.controller.collectAsState(null)
    val playerState by rememberPlayerState(player, localMediaPlaySession)
    val enablePip = playerState.currentIsPlaying && playingSession?.lastUuid == localMediaPlaySession.uuid
    Napier.d(tag = "MediaPlayer") {
        "VideoPlayer ${localMediaPlaySession.uuid} enablePip: $enablePip"
    }
    val ratio = Rational(16, 9)
    val pipModifier = Modifier.androidPipMode(enablePip, ratio)
    Box(modifier = if (fill) pipModifier.fillMaxSize() else pipModifier.fillMaxWidth().heightIn(min = 224.dp)) {
        when {
            player == null -> PlayerWaiting(localMediaPlaySession, remoteMediaItem, video = false)

            playingSession == null -> PlayerWaiting(localMediaPlaySession, remoteMediaItem, video = false)

            playingSession.lastUuid == localMediaPlaySession.uuid ->
                AudioPlayerInternal(
                    player,
                    remoteMediaItem,
                    localMediaPlaySession,
                    playingSession,
                )

            playingSession.id == localMediaPlaySession.id -> PlayerOccupy(localMediaPlaySession)

            else -> PlayerWaiting(localMediaPlaySession, remoteMediaItem, video = false)
        }
    }
}

@Composable
private fun BoxScope.AudioPlayerInternal(
    player: MediaController?,
    remoteMediaItem: RemoteMediaItem,
    localSession: LocalMediaPlaySession,
    playingSession: MediaPlaySession,
) {
    player ?: return
    AndroidPlayerContainer(player) {
        AndroidMediaSurface(
            remoteMediaItem,
            player,
            localSession,
            playingSession,
            Modifier.fillMaxSize(),
            video = false,
            controlsVisible = !rememberIsInPipMode(),
        ) {}
    }
}
