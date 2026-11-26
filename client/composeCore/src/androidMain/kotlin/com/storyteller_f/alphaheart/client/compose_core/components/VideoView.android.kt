/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import android.util.Rational
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.storyteller_f.alphaheart.client.compose_core.media.LocalMediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.media.LocalMediaPlayerService
import com.storyteller_f.alphaheart.client.compose_core.media.MediaPlaySession
import com.storyteller_f.alphaheart.client.compose_core.media.MediaSurfaceLayout
import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.media.findActivity
import io.github.aakira.napier.Napier
import kotlin.uuid.ExperimentalUuidApi

@Composable
actual fun VideoViewEmbed(remoteMediaItem: RemoteMediaItem, loadOnPlay: Boolean) {
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
    val layout = if (fill) MediaSurfaceLayout.Bounded else MediaSurfaceLayout.EmbeddedVideo(ratio.toFloat())
    Box(modifier = if (fill) pipModifier.fillMaxSize() else pipModifier.fillMaxWidth()) {
        when {
            player == null -> PlayerWaiting(localMediaPlaySession, remoteMediaItem, layout = layout)

            playingSession == null -> PlayerWaiting(localMediaPlaySession, remoteMediaItem, layout = layout)

            playingSession.lastUuid == localMediaPlaySession.uuid ->
                VideoPlayerInternal(
                    player,
                    remoteMediaItem,
                    localMediaPlaySession,
                    playingSession,
                    layout,
                )

            playingSession.id == localMediaPlaySession.id ->
                Box(if (fill) Modifier.fillMaxSize() else Modifier.fillMaxWidth().aspectRatio(ratio.toFloat())) {
                    PlayerOccupy(localMediaPlaySession)
                }

            else -> PlayerWaiting(localMediaPlaySession, remoteMediaItem, layout = layout)
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
    layout: MediaSurfaceLayout,
) {
    player ?: return
    AndroidPlayerContainer(player) {
        val isPip = rememberIsInPipMode()
        AndroidMediaSurface(
            item,
            player,
            localSession,
            playingSession,
            if (!isPip && layout is MediaSurfaceLayout.EmbeddedVideo) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxSize()
            },
            controlsVisible = !isPip,
            layout = if (isPip) MediaSurfaceLayout.Bounded else layout,
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
