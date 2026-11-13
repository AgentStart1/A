/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import com.storyteller_f.shared.obj.CustomAnswer
import com.storyteller_f.shared.obj.CustomCandidate
import com.storyteller_f.shared.obj.RoomFrame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RtcCallHostTest {
    @Test
    fun audioJoinPublishesMediaState() =
        runTest {
            val fixture = CallFixture(this)
            fixture.join(video = false)
            val active = assertIs<RtcCallState.Active>(fixture.host.state.value)
            assertTrue(active.videoMuted)
            assertEquals(listOf(RoomFrame.StartCall(1L), RoomFrame.UpdateCallMediaState(1L, false, true)), fixture.sent)
            fixture.close()
        }

    @Test
    fun mediaFailureReturnsToReady() =
        runTest {
            val fixture = CallFixture(this)
            fixture.engine.pending =
                CompletableDeferred<RtcMedia>().apply {
                    completeExceptionally(IllegalStateException("No capture device"))
                }
            fixture.join()
            assertTrue(assertIs<RtcCallState.Ready>(fixture.host.state.value).failed)
            assertTrue(fixture.sent.isEmpty())
            fixture.close()
        }

    @Test
    fun signalingFailureReleasesLocalMedia() =
        runTest {
            val fixture = CallFixture(this)
            fixture.failSend = true
            fixture.join()
            assertTrue(assertIs<RtcCallState.Ready>(fixture.host.state.value).failed)
            assertTrue(fixture.engine.media.closed)
            fixture.close()
        }

    @Test
    fun sixPeersKeepEveryEarlyCandidate() =
        runTest {
            val fixture = CallFixture(this)
            fixture.join()
            for (uid in 2L..7L) {
                fixture.frame(RoomFrame.CreateOffer(uid, 1L))
                repeat(24) { index ->
                    fixture.frame(RoomFrame.ReceiveCandidate(CustomCandidate("audio", index, "$uid-$index"), 1L, uid))
                }
                assertTrue(
                    fixture.engine.peers.last().candidates
                        .isEmpty(),
                )
                fixture.frame(RoomFrame.RespondAnswer(CustomAnswer("answer"), 1L, uid))
            }
            assertEquals(6, assertIs<RtcCallState.Active>(fixture.host.state.value).participants.size)
            assertEquals(List(6) { 24 }, fixture.engine.peers.map { it.candidates.size })
            fixture.frame(RoomFrame.PeerLeft(3L, 1L))
            assertTrue(fixture.engine.peers[1].closed)
            assertFalse(fixture.engine.peers[0].closed)
            assertEquals(5, assertIs<RtcCallState.Active>(fixture.host.state.value).participants.size)
            fixture.close()
        }

    @Test
    fun cancelledMediaAcquisitionCannotJoinLater() =
        runTest {
            val fixture = CallFixture(this)
            val pending = CompletableDeferred<RtcMedia>()
            fixture.engine.pending = pending
            fixture.host.join(video = true)
            testScheduler.runCurrent()
            assertIs<RtcCallState.Joining>(fixture.host.state.value)
            fixture.host.leave()
            testScheduler.runCurrent()
            pending.complete(fixture.engine.media)
            testScheduler.runCurrent()
            assertTrue(fixture.engine.media.closed)
            assertTrue(fixture.sent.isEmpty())
            assertIs<RtcCallState.Ready>(fixture.host.state.value)
            fixture.close()
        }

    @Test
    fun oldPeerCallbacksCannotReplaceNewTrack() =
        runTest {
            val fixture = CallFixture(this)
            fixture.join()
            fixture.frame(RoomFrame.CreateOffer(2L, 1L))
            val old = fixture.engine.peers.single()
            fixture.frame(RoomFrame.PeerLeft(2L, 1L))
            fixture.frame(RoomFrame.CreateOffer(2L, 1L))
            val video = FakeRtcVideo()
            fixture.engine.peers.last().events
                .video(video)
            old.events.video(FakeRtcVideo())
            old.events.connection(RtcConnectionState.Failed)
            testScheduler.runCurrent()
            val participant = assertIs<RtcCallState.Active>(fixture.host.state.value).participants.single()
            assertSame(video, participant.video)
            assertEquals(RtcConnectionState.Connecting, participant.connection)
            fixture.close()
        }

    @Test
    fun closingDuringNegotiationReleasesAllMedia() =
        runTest {
            val fixture = CallFixture(this)
            fixture.join()
            fixture.frame(RoomFrame.CreateOffer(2L, 1L))
            fixture.close()
            assertTrue(fixture.engine.peers.single().closed)
            assertTrue(fixture.engine.media.closed)
            assertEquals(RoomFrame.StopCall(1L), fixture.sent.last())
        }

    @Test
    fun unrelatedRoomsCannotCreatePeers() =
        runTest {
            val fixture = CallFixture(this)
            fixture.join()
            fixture.frame(RoomFrame.CreateOffer(2L, 99L))
            fixture.frame(RoomFrame.PeerMediaState(2L, 99L, true, true))
            assertTrue(fixture.engine.peers.isEmpty())
            assertTrue(assertIs<RtcCallState.Active>(fixture.host.state.value).participants.isEmpty())
            fixture.close()
        }

    @Test
    fun muteControlsPreserveParticipants() =
        runTest {
            val fixture = CallFixture(this)
            fixture.join()
            fixture.frame(RoomFrame.PeerMediaState(2L, 1L, true, false))
            fixture.host.toggleAudio()
            fixture.host.toggleVideo()
            testScheduler.runCurrent()
            val active = assertIs<RtcCallState.Active>(fixture.host.state.value)
            assertTrue(active.audioMuted)
            assertTrue(active.videoMuted)
            assertEquals(2L, active.participants.single().uid)
            assertEquals(RoomFrame.UpdateCallMediaState(1L, true, true), fixture.sent.last())
            fixture.close()
        }

    @Test
    fun cameraSwitchPublishesReplacement() =
        runTest {
            val fixture = CallFixture(this)
            fixture.join()
            val before = assertIs<RtcCallState.Active>(fixture.host.state.value).localVideo
            fixture.host.switchCamera()
            testScheduler.runCurrent()
            val after = assertIs<RtcCallState.Active>(fixture.host.state.value)
            assertFalse(before === after.localVideo)
            assertSame(fixture.engine.media.video, after.localVideo)
            fixture.close()
        }

    @Test
    fun cameraFailurePublishesMissingVideo() =
        runTest {
            val fixture = CallFixture(this)
            fixture.join()
            fixture.engine.media.failSwitch = true
            fixture.host.switchCamera()
            testScheduler.runCurrent()
            val active = assertIs<RtcCallState.Active>(fixture.host.state.value)
            assertEquals(null, active.localVideo)
            assertTrue(active.videoMuted)
            assertTrue(active.mediaFailed)
            fixture.close()
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class CallFixture(private val test: TestScope) {
    val engine = FakeRtcEngine()
    val sent = mutableListOf<RoomFrame>()
    var failSend = false
    private val frames = MutableSharedFlow<RoomFrame>()
    val host =
        RtcCallHost(1L, test.backgroundScope, StandardTestDispatcher(test.testScheduler), engine, frames) {
            check(!failSend) { "Synthetic signaling failure" }
            sent.add(it)
        }

    fun join(video: Boolean = true) {
        host.join(video)
        test.testScheduler.runCurrent()
    }

    suspend fun frame(frame: RoomFrame) {
        frames.emit(frame)
        test.testScheduler.runCurrent()
    }

    fun close() {
        host.close()
        test.testScheduler.runCurrent()
    }
}

private class FakeRtcEngine : RtcEngine {
    val media = FakeRtcMedia()
    val peers = mutableListOf<FakeRtcPeer>()
    var pending: CompletableDeferred<RtcMedia>? = null

    override suspend fun openMedia(video: Boolean): RtcMedia =
        pending?.let { withContext(NonCancellable) { it.await() } } ?: media

    override suspend fun createPeer(media: RtcMedia, events: RtcPeerEvents, scope: CoroutineScope): RtcPeer =
        FakeRtcPeer(events).also { peers.add(it) }
}

private class FakeRtcVideo : RtcVideo

private class FakeRtcMedia : RtcMedia {
    override var video: RtcVideo? = FakeRtcVideo()
    var failSwitch = false
    var closed = false

    override suspend fun mute(audioMuted: Boolean, videoMuted: Boolean) = Unit
    override suspend fun switchCamera() {
        video = null
        check(!failSwitch) { "Synthetic camera failure" }
        video = FakeRtcVideo()
    }
    override suspend fun close() {
        closed = true
    }
}

private class FakeRtcPeer(val events: RtcPeerEvents) : RtcPeer {
    val candidates = mutableListOf<CustomCandidate>()
    var closed = false
    private var remoteReady = false

    override suspend fun createOffer() = "offer"
    override suspend fun createAnswer(offer: String): String {
        remoteReady = true
        return "answer"
    }

    override suspend fun acceptAnswer(answer: String) {
        remoteReady = true
    }

    override suspend fun addCandidate(candidate: CustomCandidate) {
        assertTrue(remoteReady)
        candidates.add(candidate)
    }

    override suspend fun close() {
        closed = true
    }
}
