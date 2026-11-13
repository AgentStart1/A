/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.storyteller_f.alphaheart.app.rtc

import dev.onvoid.webrtc.media.video.VideoTrack
import dev.onvoid.webrtc.media.video.VideoTrackSink
import io.ktor.client.webrtc.JvmWebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcMedia
import io.ktor.client.webrtc.WebRtcPeerConnection
import io.ktor.client.webrtc.media.JvmMediaDevices
import io.ktor.client.webrtc.media.getNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DesktopRtcVideo(private val track: VideoTrack) : RtcVideo {
    private val access = Mutex()
    private val sinks = mutableSetOf<VideoTrackSink>()
    private var closed = false

    suspend fun attach(sink: VideoTrackSink): Boolean =
        access.withLock {
            if (closed) return@withLock false
            track.addSink(sink)
            sinks.add(sink)
            true
        }

    suspend fun detach(sink: VideoTrackSink) =
        access.withLock {
            if (sinks.remove(sink)) track.removeSink(sink)
        }

    override suspend fun close() =
        access.withLock {
            closed = true
            sinks.forEach { track.removeSink(it) }
            sinks.clear()
        }
}

internal actual fun createRtcEngine(): RtcEngine =
    KtorRtcEngine(Dispatchers.IO.limitedParallelism(1)) {
        val devices = JvmMediaDevices()
        try {
            devices.audioModule.initPlayout()
            devices.audioModule.startPlayout()
            KtorRtcRuntime(
                WebRtcClient(JvmWebRtc) { mediaTrackFactory = devices },
                closeAudio = { it.getNative().dispose() },
                closeResources = devices::close,
            )
        } catch (failure: Exception) {
            devices.close()
            throw failure
        }
    }

internal actual fun wrapRtcVideo(track: WebRtcMedia.VideoTrack): RtcVideo = DesktopRtcVideo(track.getNative())

internal actual fun releaseRtcConnection(connection: WebRtcPeerConnection) = Unit

internal actual fun playRtcAudio(track: WebRtcMedia.AudioTrack): RtcAudioPlayback =
    object : RtcAudioPlayback {
        override fun close() {
            track.enable(false)
        }
    }
