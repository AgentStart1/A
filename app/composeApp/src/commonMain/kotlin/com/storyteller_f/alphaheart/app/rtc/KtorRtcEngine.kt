/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.storyteller_f.alphaheart.app.rtc

import com.storyteller_f.shared.obj.CustomCandidate
import io.ktor.client.webrtc.TrackEvent
import io.ktor.client.webrtc.WebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcMedia
import io.ktor.client.webrtc.WebRtcPeerConnection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal interface RtcAudioPlayback {
    fun close()
}

internal expect fun playRtcAudio(track: WebRtcMedia.AudioTrack): RtcAudioPlayback
internal expect fun wrapRtcVideo(track: WebRtcMedia.VideoTrack): RtcVideo
internal expect fun releaseRtcConnection(connection: WebRtcPeerConnection)

internal data class KtorRtcRuntime(
    val client: WebRtcClient,
    val switchCamera: Boolean = false,
    val closeAudio: (WebRtcMedia.AudioTrack) -> Unit = { it.close() },
    val closeResources: () -> Unit = {},
    val closeRemoteTrack: (WebRtcMedia.Track) -> Unit = { it.close() },
)

internal class KtorRtcEngine(
    private val worker: CoroutineDispatcher,
    private val createRuntime: () -> KtorRtcRuntime,
) : RtcEngine {
    override suspend fun openMedia(video: Boolean): RtcMedia {
        // Native handle acquisition completes under protection; cancellation still belongs to the caller.
        val caller = currentCoroutineContext()
        caller.ensureActive()
        var acquired: KtorRtcMedia? = null
        var delivered = false
        try {
            val media =
                withContext(NonCancellable + worker) {
                    acquireMedia(video, caller).also { acquired = it }
                }
            caller.ensureActive()
            delivered = true
            return media
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            caller.ensureActive()
            throw failure
        } finally {
            if (!delivered) withContext(NonCancellable + worker) { acquired?.close() }
        }
    }

    private suspend fun acquireMedia(video: Boolean, caller: CoroutineContext): KtorRtcMedia {
        val runtime = createRuntime()
        var audio: WebRtcMedia.AudioTrack? = null
        var camera: WebRtcMedia.VideoTrack? = null
        var acquired = false
        try {
            caller.ensureActive()
            val microphone =
                runtime.client.createAudioTrack {
                    echoCancellation = true
                    noiseSuppression = true
                }
            audio = microphone
            caller.ensureActive()
            camera =
                if (video) {
                    runtime.client.createVideoTrack {
                        width = 640
                        height = 480
                    }
                } else {
                    null
                }
            caller.ensureActive()
            val media = KtorRtcMedia(runtime, microphone, camera, worker)
            acquired = true
            return media
        } finally {
            if (!acquired) {
                camera?.close()
                audio?.let(runtime.closeAudio)
                runtime.client.close()
                runtime.closeResources()
            }
        }
    }

    override suspend fun createPeer(media: RtcMedia, events: RtcPeerEvents, scope: CoroutineScope): RtcPeer {
        val local = media as KtorRtcMedia
        val connection =
            local.runtime.client.createPeerConnection {
                iceServers = listOf(WebRtc.IceServer("stun:stun.l.google.com:19302"))
            }
        val peer = KtorRtcPeer(connection, local, events, scope, worker)
        var attached = false
        try {
            peer.attachTracks()
            attached = true
            return peer
        } finally {
            if (!attached) withContext(NonCancellable) { peer.close() }
        }
    }
}

private class KtorRtcMedia(
    val runtime: KtorRtcRuntime,
    val audio: WebRtcMedia.AudioTrack,
    private var camera: WebRtcMedia.VideoTrack?,
    private val worker: CoroutineDispatcher,
) : RtcMedia {
    override var video: RtcVideo? = camera?.let(::wrapRtcVideo)
        private set
    override val canSwitchCamera get() = runtime.switchCamera
    private var environmentCamera = false
    private var cameraMuted = false
    private val access = Mutex()
    private val cameraSenders = mutableListOf<WebRtc.RtpSender>()

    suspend fun attach(connection: WebRtcPeerConnection): List<WebRtc.RtpSender> =
        access.withLock {
            val audioSender = connection.addTrack(audio)
            var attached = false
            try {
                val videoSender = camera?.let { connection.addTrack(it) }
                if (videoSender != null) cameraSenders.add(videoSender)
                val senders = listOfNotNull(audioSender, videoSender)
                attached = true
                senders
            } finally {
                if (!attached) withContext(NonCancellable) { audioSender.replaceTrack(null) }
            }
        }

    suspend fun detach(senders: List<WebRtc.RtpSender>) =
        access.withLock {
            senders.forEach { it.replaceTrack(null) }
            cameraSenders.removeAll(senders.toSet())
        }

    override suspend fun mute(audioMuted: Boolean, videoMuted: Boolean) =
        withContext(worker) {
            audio.enable(!audioMuted)
            camera?.enable(!videoMuted)
            cameraMuted = videoMuted
        }

    override suspend fun switchCamera() =
        withContext(worker) {
            access.withLock {
                if (!runtime.switchCamera || camera == null) return@withLock
                cameraSenders.forEach { it.replaceTrack(null) }
                video?.close()
                camera?.close()
                camera = null
                video = null
                environmentCamera = !environmentCamera
                val replacement =
                    runtime.client.createVideoTrack {
                        width = 640
                        height = 480
                        facingMode =
                            if (environmentCamera) WebRtcMedia.FacingMode.ENVIRONMENT else WebRtcMedia.FacingMode.USER
                    }
                replacement.enable(!cameraMuted)
                camera = replacement
                video = wrapRtcVideo(replacement)
                cameraSenders.forEach { it.replaceTrack(replacement) }
            }
        }

    override suspend fun close() =
        withContext(worker) {
            video?.close()
            camera?.close()
            runtime.closeAudio(audio)
            runtime.client.close()
            runtime.closeResources()
        }
}

private class KtorRtcPeer(
    private val connection: WebRtcPeerConnection,
    private val media: KtorRtcMedia,
    events: RtcPeerEvents,
    scope: CoroutineScope,
    private val worker: CoroutineDispatcher,
) : RtcPeer {
    private val observerJob = SupervisorJob(scope.coroutineContext[Job])
    private val observers = CoroutineScope(scope.coroutineContext + observerJob)
    private var senders = emptyList<WebRtc.RtpSender>()
    private val remoteVideos = mutableListOf<RtcVideo>()
    private val remoteAudio = mutableListOf<RtcAudioPlayback>()
    private val remoteTracks = mutableListOf<WebRtcMedia.Track>()
    init {
        connection.trackEvents.onEach { event ->
            if (event is TrackEvent.Add) {
                remoteTracks.add(event.track)
                when (val track = event.track) {
                    is WebRtcMedia.VideoTrack -> {
                        val video = wrapRtcVideo(track)
                        remoteVideos.add(video)
                        events.video(video)
                    }

                    is WebRtcMedia.AudioTrack -> remoteAudio.add(playRtcAudio(track))
                }
            }
        }.launchIn(observers)

        connection.iceCandidates.filter { it.candidate.isNotEmpty() }.onEach {
            events.candidate(CustomCandidate(it.sdpMid, it.sdpMLineIndex, it.candidate))
        }.launchIn(observers)
        connection.state.onEach {
            events.connection(
                when (it) {
                    WebRtc.ConnectionState.CONNECTED -> RtcConnectionState.Connected

                    WebRtc.ConnectionState.DISCONNECTED,
                    WebRtc.ConnectionState.CLOSED,
                    -> RtcConnectionState.Disconnected

                    WebRtc.ConnectionState.FAILED -> RtcConnectionState.Failed

                    else -> RtcConnectionState.Connecting
                },
            )
        }.launchIn(observers)
    }

    suspend fun attachTracks() {
        senders = media.attach(connection)
    }

    override suspend fun createOffer(): String {
        val offer = connection.createOffer()
        connection.setLocalDescription(offer)
        return offer.sdp
    }

    override suspend fun createAnswer(offer: String): String {
        connection.setRemoteDescription(WebRtc.SessionDescription(WebRtc.SessionDescriptionType.OFFER, offer))
        val answer = connection.createAnswer()
        connection.setLocalDescription(answer)
        return answer.sdp
    }

    override suspend fun acceptAnswer(answer: String) {
        connection.setRemoteDescription(WebRtc.SessionDescription(WebRtc.SessionDescriptionType.ANSWER, answer))
    }

    override suspend fun addCandidate(candidate: CustomCandidate) {
        // Browser end-of-candidates markers are not native ICE candidates.
        // https://www.w3.org/TR/webrtc/#dom-rtcpeerconnectioniceevent-candidate
        if (candidate.candidate.isEmpty()) return
        connection.addIceCandidate(WebRtc.IceCandidate(candidate.candidate, candidate.sdpMid, candidate.sdpMLineIndex))
    }

    override suspend fun close() =
        withContext(worker) {
            observerJob.cancelAndJoin()
            media.detach(senders)
            remoteVideos.forEach { it.close() }
            remoteAudio.forEach(RtcAudioPlayback::close)
            remoteTracks.forEach(media.runtime.closeRemoteTrack)
            connection.close()
            releaseRtcConnection(connection)
        }
}
