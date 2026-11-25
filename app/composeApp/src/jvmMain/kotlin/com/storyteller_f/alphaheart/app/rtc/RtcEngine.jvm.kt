/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.storyteller_f.alphaheart.app.rtc

import dev.onvoid.webrtc.media.MediaDevices
import dev.onvoid.webrtc.media.audio.AudioOptions
import dev.onvoid.webrtc.media.video.VideoTrack
import dev.onvoid.webrtc.media.video.VideoTrackSink
import io.ktor.client.webrtc.JvmWebRtc
import io.ktor.client.webrtc.MediaTrackFactory
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcMedia
import io.ktor.client.webrtc.WebRtcPeerConnection
import io.ktor.client.webrtc.media.JvmAudioTrack
import io.ktor.client.webrtc.media.JvmMediaDevices
import io.ktor.client.webrtc.media.getNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

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
            val audioModule = devices.audioModule
            MediaDevices.getDefaultAudioRenderDevice()?.let(audioModule::setPlayoutDevice)
            KtorRtcRuntime(
                WebRtcClient(JvmWebRtc) {
                    mediaTrackFactory = DesktopRtcTrackFactory(devices)
                    rtcFactory = devices.peerConnectionFactory
                },
                closeAudio = { it.getNative().dispose() },
                closeResources = { closeDesktopDevices(devices) },
            )
        } catch (failure: Throwable) {
            runCatching { closeDesktopDevices(devices) }.exceptionOrNull()?.let(failure::addSuppressed)
            // WebRTC JNI reports device failures as plain Error instances.
            if (failure.javaClass == Error::class.java) {
                throw IllegalStateException("Failed to initialize desktop audio devices", failure)
            }
            throw failure
        }
    }

private class DesktopRtcTrackFactory(private val devices: JvmMediaDevices) : MediaTrackFactory {
    override suspend fun createAudioTrack(constraints: WebRtcMedia.AudioTrackConstraints): WebRtcMedia.AudioTrack {
        // Let the peer connection register its audio transport before starting capture or playout.
        val options =
            AudioOptions().apply {
                autoGainControl = constraints.autoGainControl != false
                echoCancellation = constraints.echoCancellation != false
                noiseSuppression = constraints.noiseSuppression != false
            }
        val factory = devices.peerConnectionFactory
        val track = factory.createAudioTrack("audio-${UUID.randomUUID()}", factory.createAudioSource(options))
        return JvmAudioTrack(track)
    }

    override suspend fun createVideoTrack(constraints: WebRtcMedia.VideoTrackConstraints) =
        devices.createVideoTrack(constraints)
}

private fun closeDesktopDevices(devices: JvmMediaDevices) {
    try {
        devices.audioModule.stopPlayout()
    } finally {
        devices.close()
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
