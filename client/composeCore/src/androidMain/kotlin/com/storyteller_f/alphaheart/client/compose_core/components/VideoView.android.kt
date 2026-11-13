/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import android.util.Rational
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import io.github.aakira.napier.Napier
import kotlin.uuid.ExperimentalUuidApi

// Constant for broadcast receiver
const val ACTION_BROADCAST_CONTROL = "broadcast_control"

// Intent extras for broadcast controls from Picture-in-Picture mode.
const val EXTRA_CONTROL_TYPE = "control_type"
const val EXTRA_CONTROL_PLAY = 1
const val EXTRA_CONTROL_PAUSE = 2

@Composable
actual fun VideoViewEmbed(remoteMediaItem: RemoteMediaItem) {
    MediaPlayerEmbed(remoteMediaItem) { playingSession, localMediaPlaySession ->
        VideoPlayer(playingSession, localMediaPlaySession, remoteMediaItem)
    }
}

@Composable
actual fun VideoViewFullScreen(remoteMediaItem: RemoteMediaItem) {
    MediaPlayerFullScreen(remoteMediaItem) { playingSession, localMediaPlaySession ->
        VideoPlayer(playingSession, localMediaPlaySession, remoteMediaItem, fill = true)
    }
}

@Composable
actual fun VideoViewFilled(remoteMediaItem: RemoteMediaItem) {
    MediaPlayerFilled(remoteMediaItem) { playingSession, localMediaPlaySession ->
        VideoPlayer(playingSession, localMediaPlaySession, remoteMediaItem, fill = true)
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun VideoPlayer(
    playingSession: MediaPlaySession?,
    localMediaPlaySession: LocalMediaPlaySession,
    remoteMediaItem: RemoteMediaItem,
    fill: Boolean = false,
) {
    val mediaPlayerService = LocalMediaPlayerService.current
    val player by mediaPlayerService.controller.collectAsState()
    val videoSize = playingSession?.videoSize
    val ratio =
        remember(playingSession, localMediaPlaySession) {
            if (videoSize != null &&
                playingSession.lastUuid == localMediaPlaySession.uuid
            ) {
                Rational(videoSize.width, videoSize.height)
            } else {
                Rational(16, 9)
            }
        }
    Napier.d {
        "VideoPlayer ${localMediaPlaySession.uuid} ratio $ratio ${playingSession?.uuids ?: "<none>"} ${videoSize ?: "<none>"}"
    }
    val playerState by rememberPlayerState(player, localMediaPlaySession)
    val enablePip = playerState.currentIsPlaying && playingSession?.lastUuid == localMediaPlaySession.uuid
    Napier.d(tag = "MediaPlayer") {
        "VideoPlayer ${localMediaPlaySession.uuid} $enablePip"
    }
    val pipModifier = Modifier.androidPipMode(enablePip, ratio)
    EnablePipPre31(enablePip, localMediaPlaySession)
    Box(
        modifier =
        if (fill) pipModifier.fillMaxSize() else pipModifier.aspectRatio(ratio.toFloat()),
    ) {
        when {
            player == null -> PlayerWaiting(localMediaPlaySession, remoteMediaItem)

            playingSession == null -> PlayerWaiting(localMediaPlaySession, remoteMediaItem)

            playingSession.lastUuid == localMediaPlaySession.uuid ->
                VideoPlayerInternal(
                    player,
                    remoteMediaItem,
                    localMediaPlaySession,
                    playingSession,
                )

            playingSession.id == localMediaPlaySession.id -> PlayerOccupy(localMediaPlaySession)

            else -> PlayerWaiting(localMediaPlaySession, remoteMediaItem)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun BoxScope.VideoPlayerInternal(
    player: MediaController?,
    item: RemoteMediaItem,
    localSession: LocalMediaPlaySession,
    playingSession: MediaPlaySession,
) {
    player ?: return
    AndroidPlayerContainer(player) {
        val isPip = rememberIsInPipMode()
        AndroidMediaSurface(
            item,
            player,
            localSession,
            playingSession,
            Modifier.fillMaxSize(),
            controlsVisible = !isPip,
        ) {
            AndroidView(
                factory = { PlayerView(it).apply { useController = false } },
                modifier = Modifier.fillMaxSize(),
                onReset = null,
                onRelease = { it.player = null },
            ) { it.player = player }
        }
    }
}

@Composable
actual fun rememberIsInPipMode(): Boolean {
    val activity = LocalContext.current.findActivity()
    var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val observer =
            Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
        activity.addOnPictureInPictureModeChangedListener(observer)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }
    return pipMode
}
