/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.storyteller_f.alphaheart.app.rtc

import com.storyteller_f.shared.obj.CustomCandidate
import dev.onvoid.webrtc.PeerConnectionFactory
import dev.onvoid.webrtc.media.audio.CustomAudioSource
import dev.onvoid.webrtc.media.audio.HeadlessAudioDeviceModule
import dev.onvoid.webrtc.media.video.CustomVideoSource
import dev.onvoid.webrtc.media.video.NativeI420Buffer
import dev.onvoid.webrtc.media.video.VideoFrame
import dev.onvoid.webrtc.media.video.VideoTrackSink
import io.ktor.client.webrtc.JvmWebRtc
import io.ktor.client.webrtc.MediaTrackFactory
import io.ktor.client.webrtc.WebRtc
import io.ktor.client.webrtc.WebRtcClient
import io.ktor.client.webrtc.WebRtcMedia
import io.ktor.client.webrtc.media.JvmAudioTrack
import io.ktor.client.webrtc.media.JvmVideoTrack
import io.ktor.client.webrtc.media.getNative
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds

class KtorRtcPeerTest {
    @Test
    fun nativePeerRejectsInvalidIceCandidate() =
        withConnectedNativePeers { _, _, firstPeer, _ ->
            assertFailsWith<WebRtc.IceException> {
                firstPeer.addCandidate(CustomCandidate("0", 0, "invalid-candidate"))
            }
        }

    @Test
    fun nativePeersNegotiateAudioAndVideo() =
        withConnectedNativePeers { first, second, firstPeer, secondPeer ->
            firstPeer.addCandidate(CustomCandidate("0", 0, ""))
            secondPeer.addCandidate(CustomCandidate("0", 0, ""))
            val firstVideo = assertNotNull(first.video.first { it != null })
            val secondVideo = assertNotNull(second.video.first { it != null })
            receiveSyntheticFrames(firstVideo as DesktopRtcVideo, second)
            receiveSyntheticFrames(secondVideo as DesktopRtcVideo, first)
        }
}

private fun withConnectedNativePeers(block: suspend (NativeCallFixture, NativeCallFixture, RtcPeer, RtcPeer) -> Unit) =
    runTest {
        // Native ICE and shutdown must use real time rather than runTest's virtual scheduler.
        withContext(Dispatchers.IO) {
            coroutineScope {
                val first = NativeCallFixture()
                val second = NativeCallFixture()
                val firstMedia = first.engine.openMedia(true)
                val secondMedia = second.engine.openMedia(true)
                val firstPeer = first.engine.createPeer(firstMedia, first.events, this)
                val secondPeer = second.engine.createPeer(secondMedia, second.events, this)
                try {
                    val answer = secondPeer.createAnswer(firstPeer.createOffer())
                    firstPeer.acceptAnswer(answer)
                    val forward = launch { for (candidate in first.candidates) secondPeer.addCandidate(candidate) }
                    val backward = launch { for (candidate in second.candidates) firstPeer.addCandidate(candidate) }
                    try {
                        withTimeout(30.seconds) {
                            combine(first.connection, second.connection) { a, b ->
                                a == RtcConnectionState.Connected && b == RtcConnectionState.Connected
                            }.first { it }
                            block(first, second, firstPeer, secondPeer)
                        }
                    } finally {
                        forward.cancel()
                        backward.cancel()
                    }
                } finally {
                    withContext(NonCancellable) {
                        firstPeer.close()
                        secondPeer.close()
                        firstMedia.close()
                        secondMedia.close()
                        first.candidates.cancel()
                        second.candidates.cancel()
                    }
                }
            }
        }
    }

private suspend fun receiveSyntheticFrames(video: DesktopRtcVideo, source: NativeCallFixture) =
    kotlinx.coroutines.coroutineScope {
        val decoded = CompletableDeferred<Int>()
        val sink = VideoTrackSink { decoded.complete(it.buffer.width) }
        check(video.attach(sink))
        val producing =
            launch {
                while (isActive) {
                    source.pushFrame()
                    delay(33)
                }
            }
        try {
            assertEquals(64, decoded.await())
        } finally {
            producing.cancelAndJoin()
            video.detach(sink)
        }
    }

private class NativeCallFixture {
    private val audioModule = HeadlessAudioDeviceModule()
    private val factory = PeerConnectionFactory(audioModule)
    private val audio = CustomAudioSource()
    private val source = CustomVideoSource()
    private val audioTrack = JvmAudioTrack(factory.createAudioTrack("audio", audio))
    private val videoTrack = JvmVideoTrack(factory.createVideoTrack("video", source))
    val engine =
        KtorRtcEngine(Dispatchers.IO.limitedParallelism(1)) {
            val tracks =
                object : MediaTrackFactory {
                    override suspend fun createAudioTrack(constraints: WebRtcMedia.AudioTrackConstraints) = audioTrack
                    override suspend fun createVideoTrack(constraints: WebRtcMedia.VideoTrackConstraints) = videoTrack
                }
            KtorRtcRuntime(
                WebRtcClient(JvmWebRtc) {
                    mediaTrackFactory = tracks

                    rtcFactory = factory
                },
                closeAudio = {},
                closeResources = {
                    audioTrack.getNative().dispose()
                    videoTrack.getNative().dispose()
                    source.dispose()
                    audio.dispose()
                    factory.dispose()
                    audioModule.dispose()
                },
            )
        }
    fun pushFrame() {
        val buffer = NativeI420Buffer.allocate(64, 48)
        val frame = VideoFrame(buffer, System.nanoTime())
        try {
            buffer.dataY.apply { while (hasRemaining()) put(80.toByte()) }
            buffer.dataU.apply { while (hasRemaining()) put(128.toByte()) }
            buffer.dataV.apply { while (hasRemaining()) put(128.toByte()) }
            source.pushFrame(frame)
        } finally {
            frame.release()
        }
    }

    val candidates = Channel<CustomCandidate>(Channel.UNLIMITED)
    val video = MutableStateFlow<RtcVideo?>(null)
    val connection = MutableStateFlow(RtcConnectionState.Connecting)
    val events =
        RtcPeerEvents(
            candidate = { candidates.trySend(it) },
            video = { video.value = it },
            connection = { connection.value = it },
        )
}
