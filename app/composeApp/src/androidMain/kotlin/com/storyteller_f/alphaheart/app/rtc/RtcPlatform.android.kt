/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.storyteller_f.alphaheart.app.rtc

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.rtc_open_settings
import com.storyteller_f.alphaheart.app.rtc_permission_required
import com.storyteller_f.shared.getAppContextRefValue
import io.ktor.client.webrtc.AndroidWebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcMedia
import io.ktor.client.webrtc.WebRtcPeerConnection
import io.ktor.client.webrtc.getNative
import io.ktor.client.webrtc.media.AndroidMediaDevices
import io.ktor.client.webrtc.media.getNative
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.stringResource
import org.webrtc.EglBase
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

private object AndroidRtcDevices {
    private val context: android.content.Context = checkNotNull(getAppContextRefValue()).applicationContext
    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions(),
        )
    }
    val egl: EglBase = EglBase.create()
    val media = AndroidMediaDevices(context, egl)
}

internal actual fun createRtcEngine(): RtcEngine =
    KtorRtcEngine(Dispatchers.IO.limitedParallelism(1)) {
        KtorRtcRuntime(
            WebRtcClient(AndroidWebRtc) { mediaTrackFactory = AndroidRtcDevices.media },
            switchCamera = true,
            // PeerConnection.dispose owns receiver tracks. A separate track close would dispose twice.
            closeRemoteTrack = {},
        )
    }

private class AndroidRtcVideo(private val track: org.webrtc.VideoTrack) : RtcVideo {
    private val access = Any()
    private val sinks = mutableSetOf<org.webrtc.VideoSink>()
    private var closed = false

    fun attach(sink: org.webrtc.VideoSink) =
        synchronized(access) {
            if (closed) return@synchronized
            track.addSink(sink)
            sinks.add(sink)
        }

    fun detach(sink: org.webrtc.VideoSink) =
        synchronized(access) {
            if (sinks.remove(sink)) track.removeSink(sink)
        }

    override suspend fun close() =
        synchronized(access) {
            closed = true
            sinks.forEach(track::removeSink)
            sinks.clear()
        }
}

internal actual fun wrapRtcVideo(track: WebRtcMedia.VideoTrack): RtcVideo = AndroidRtcVideo(track.getNative())

internal actual fun releaseRtcConnection(connection: WebRtcPeerConnection) {
    connection.getNative().dispose()
}

internal actual fun playRtcAudio(track: WebRtcMedia.AudioTrack): RtcAudioPlayback =
    object : RtcAudioPlayback {
        override fun close() {
            track.enable(false)
        }
    }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
internal actual fun RtcJoinButton(video: Boolean, enabled: Boolean, onJoin: () -> Unit) {
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }
    val permissions =
        rememberMultiplePermissionsState(
            if (video) {
                listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
            } else {
                listOf(Manifest.permission.RECORD_AUDIO)
            },
        ) { result ->
            denied = result.values.any { !it }
            if (!denied) onJoin()
        }
    Column {
        DefaultRtcJoinButton(video, enabled) {
            if (permissions.allPermissionsGranted) onJoin() else permissions.launchMultiplePermissionRequest()
        }
        if (denied) {
            Text(stringResource(Res.string.rtc_permission_required))
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")),
                )
            }) {
                Text(stringResource(Res.string.rtc_open_settings))
            }
        }
    }
}

@Composable
internal actual fun RtcVideoView(video: RtcVideo, modifier: Modifier, local: Boolean) {
    val context = LocalContext.current
    val track = video as AndroidRtcVideo
    val renderer = remember { SurfaceViewRenderer(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, renderer, track) {
        var initialized = false
        fun resumeRenderer() {
            if (!initialized) {
                renderer.init(AndroidRtcDevices.egl.eglBaseContext, null)
                renderer.setMirror(local)
                track.attach(renderer)
                initialized = true
            }
        }
        fun pauseRenderer() {
            if (initialized) {
                // A peer may finish closing its native track before Compose disposes the tile.
                track.detach(renderer)
                renderer.release()
                initialized = false
            }
        }
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> resumeRenderer()
                    Lifecycle.Event.ON_PAUSE -> pauseRenderer()
                    else -> Unit
                }
            }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) resumeRenderer()
        onDispose {
            lifecycle.removeObserver(observer)
            pauseRenderer()
        }
    }
    AndroidView(
        factory = { renderer.apply { setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT) } },
        modifier = modifier,
    )
}
