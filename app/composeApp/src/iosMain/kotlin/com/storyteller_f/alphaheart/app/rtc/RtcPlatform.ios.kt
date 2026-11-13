/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.storyteller_f.alphaheart.app.rtc

import WebRTC.RTCMTLVideoView
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.rtc_permission_required
import io.ktor.client.webrtc.IosWebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcMedia
import io.ktor.client.webrtc.WebRtcPeerConnection
import io.ktor.client.webrtc.media.getNative
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.UIKit.UIViewContentMode
import kotlin.coroutines.resume

internal actual fun createRtcEngine(): RtcEngine =
    KtorRtcEngine(Dispatchers.Default.limitedParallelism(1)) {
        KtorRtcRuntime(WebRtcClient(IosWebRtc), switchCamera = true)
    }

private class IosRtcVideo(private val track: WebRTC.RTCVideoTrack) : RtcVideo {
    private val renderers = mutableSetOf<RTCMTLVideoView>()
    private var closed = false

    fun attach(renderer: RTCMTLVideoView) {
        if (closed) return
        track.addRenderer(renderer)
        renderers.add(renderer)
    }

    fun detach(renderer: RTCMTLVideoView) {
        if (renderers.remove(renderer)) track.removeRenderer(renderer)
    }

    override suspend fun close() =
        withContext(Dispatchers.Main) {
            closed = true
            renderers.forEach(track::removeRenderer)
            renderers.clear()
        }
}

internal actual fun wrapRtcVideo(track: WebRtcMedia.VideoTrack): RtcVideo = IosRtcVideo(track.getNative())
internal actual fun releaseRtcConnection(connection: WebRtcPeerConnection) = Unit

internal actual fun playRtcAudio(track: WebRtcMedia.AudioTrack): RtcAudioPlayback =
    object : RtcAudioPlayback {
        override fun close() {
            track.enable(false)
        }
    }

@Composable
internal actual fun RtcJoinButton(video: Boolean, enabled: Boolean, onJoin: () -> Unit) {
    val scope = rememberCoroutineScope()
    var denied by remember { mutableStateOf(false) }
    Column {
        DefaultRtcJoinButton(video, enabled) {
            scope.launch {
                val allowed =
                    requestMediaPermission(AVMediaTypeAudio) &&
                        (!video || requestMediaPermission(AVMediaTypeVideo))
                denied = !allowed
                if (allowed) onJoin()
            }
        }
        if (denied) Text(stringResource(Res.string.rtc_permission_required))
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun RtcVideoView(video: RtcVideo, modifier: Modifier, local: Boolean) {
    val track = video as IosRtcVideo
    UIKitView(
        factory = {
            RTCMTLVideoView().apply {
                videoContentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                transform = CGAffineTransformMakeScale(if (local) -1.0 else 1.0, 1.0)
                track.attach(this)
            }
        },
        modifier = modifier,
        onRelease = { track.detach(it) },
    )
}

private suspend fun requestMediaPermission(type: String?): Boolean =
    suspendCancellableCoroutine { continuation ->
        AVCaptureDevice.requestAccessForMediaType(type) { granted ->
            if (continuation.isActive) continuation.resume(granted)
        }
    }
