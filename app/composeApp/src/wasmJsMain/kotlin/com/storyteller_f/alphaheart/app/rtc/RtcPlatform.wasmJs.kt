/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.storyteller_f.alphaheart.app.rtc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import io.ktor.client.webrtc.JsWebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcMedia
import io.ktor.client.webrtc.WebRtcPeerConnection
import io.ktor.client.webrtc.getNative
import kotlinx.browser.document
import kotlinx.coroutines.Dispatchers
import org.w3c.dom.HTMLAudioElement
import org.w3c.dom.HTMLVideoElement
import org.w3c.dom.MediaProvider
import web.mediastreams.MediaStream

internal actual fun createRtcEngine(): RtcEngine =
    KtorRtcEngine(Dispatchers.Main) {
        KtorRtcRuntime(WebRtcClient(JsWebRtc) { mediaTrackFactory = BrowserRtcMediaDevices() }, switchCamera = true)
    }

private class BrowserRtcVideo(val track: WebRtcMedia.VideoTrack) : RtcVideo

internal actual fun wrapRtcVideo(track: WebRtcMedia.VideoTrack): RtcVideo = BrowserRtcVideo(track)
internal actual fun releaseRtcConnection(connection: WebRtcPeerConnection) = Unit

/** Audio belongs to the peer, so scrolling a video tile offscreen does not mute a participant. */
internal actual fun playRtcAudio(track: WebRtcMedia.AudioTrack): RtcAudioPlayback {
    val stream = MediaStream().apply { addTrack(track.getNative()) }
    val element =
        (document.createElement("audio") as HTMLAudioElement).apply {
            autoplay = true
            srcObject = stream as MediaProvider
        }
    document.body?.appendChild(element)
    return object : RtcAudioPlayback {
        override fun close() {
            element.pause()
            element.srcObject = null
            element.remove()
            stream.removeTrack(track.getNative())
        }
    }
}

@Composable
internal actual fun RtcJoinButton(video: Boolean, enabled: Boolean, onJoin: () -> Unit) {
    DefaultRtcJoinButton(video, enabled, onJoin)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun RtcVideoView(video: RtcVideo, modifier: Modifier, local: Boolean) {
    val track = (video as BrowserRtcVideo).track
    val stream = remember(track) { MediaStream().apply { addTrack(track.getNative()) } }
    HtmlElementView(
        factory = {
            (document.createElement("video") as HTMLVideoElement).apply {
                muted = true
                autoplay = true
                setAttribute("playsinline", "")
                style.width = "100%"
                style.height = "100%"
                style.objectFit = "contain"
                if (local) style.transform = "scaleX(-1)"
                srcObject = stream as MediaProvider
            }
        },
        modifier = modifier,
        onRelease = {
            it.pause()
            it.srcObject = null
            stream.removeTrack(track.getNative())
        },
    )
}
