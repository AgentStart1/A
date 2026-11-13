/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import com.storyteller_f.shared.obj.CustomAnswer
import com.storyteller_f.shared.obj.CustomOffer
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.type.PrimaryKey
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal data class RtcParticipant(
    val uid: PrimaryKey,
    val video: RtcVideo? = null,
    val audioMuted: Boolean = false,
    val videoMuted: Boolean = false,
    val connection: RtcConnectionState = RtcConnectionState.Connecting,
)

internal sealed interface RtcCallState {
    data class Ready(val failed: Boolean = false) : RtcCallState
    data object Joining : RtcCallState
    data object Leaving : RtcCallState
    data class Active(
        val media: RtcMedia,
        val localVideo: RtcVideo? = media.video,
        val participants: ImmutableList<RtcParticipant> = persistentListOf(),
        val audioMuted: Boolean = false,
        val videoMuted: Boolean = false,
        val mediaFailed: Boolean = false,
    ) : RtcCallState
}

internal enum class RtcCallEffect { Left }

/** Owns one room call on the injected serial dispatcher, independently of the UI runtime. */
internal class RtcCallHost(
    private val roomId: PrimaryKey,
    parentScope: CoroutineScope,
    coordination: CoroutineDispatcher,
    private val engine: RtcEngine,
    frames: Flow<RoomFrame>,
    private val send: suspend (RoomFrame) -> Unit,
) {
    private val scope =
        CoroutineScope(
            parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]) + coordination,
        )
    private val mutableState = MutableStateFlow<RtcCallState>(RtcCallState.Ready())
    val state = mutableState.asStateFlow()
    private val effectEvents = Channel<RtcCallEffect>(Channel.BUFFERED)
    val effects = effectEvents.receiveAsFlow()
    private val peers = mutableMapOf<PrimaryKey, Job>()
    private val peerIdentities = mutableMapOf<PrimaryKey, Any>()
    private val signals = mutableMapOf<PrimaryKey, RtcSignaling>()
    private var joining: Job? = null
    private var generation = 0L
    private var exitRequested = false
    private val mediaMutex = Mutex()

    init {
        scope.launch {
            try {
                frames.collect(::receiveFrame)
            } finally {
                withContext(NonCancellable) { releaseCall() }
                effectEvents.close()
            }
        }
    }

    fun join(video: Boolean) {
        scope.launch {
            if (exitRequested || mutableState.value !is RtcCallState.Ready) return@launch
            mutableState.value = RtcCallState.Joining
            val current = ++generation
            joining = scope.launch { acquireMedia(video, current) }
        }
    }

    private suspend fun acquireMedia(video: Boolean, current: Long) {
        val media =
            try {
                engine.openMedia(video)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Napier.e(failure, tag = "rtc") { "Unable to acquire call media" }
                if (current == generation) mutableState.value = RtcCallState.Ready(failed = true)
                return
            }
        if (current != generation || mutableState.value != RtcCallState.Joining) {
            withContext(NonCancellable) { media.close() }
            return
        }
        mutableState.value = RtcCallState.Active(media, videoMuted = !video)
        try {
            send(RoomFrame.StartCall(roomId))
            publishMediaState()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            Napier.e(failure, tag = "rtc") { "Unable to join room call" }
            releaseCall()
            mutableState.value = RtcCallState.Ready(failed = true)
        }
    }

    fun leave() {
        scope.launch {
            if (exitRequested) return@launch
            exitRequested = true
            releaseCall()
            effectEvents.send(RtcCallEffect.Left)
        }
    }

    fun close() {
        scope.cancel()
    }

    fun toggleAudio() = changeMedia { it.copy(audioMuted = !it.audioMuted) }

    fun toggleVideo() =
        changeMedia {
            if (it.localVideo == null) it else it.copy(videoMuted = !it.videoMuted)
        }

    fun switchCamera() =
        updateMedia { active ->
            active.media.switchCamera()
            active.copy(localVideo = active.media.video)
        }

    private fun changeMedia(change: (RtcCallState.Active) -> RtcCallState.Active) =
        updateMedia { active ->
            val updated = change(active)
            updated.media.mute(updated.audioMuted, updated.videoMuted)
            updated
        }

    private fun updateMedia(change: suspend (RtcCallState.Active) -> RtcCallState.Active) {
        scope.launch {
            mediaMutex.withLock {
                val active = mutableState.value as? RtcCallState.Active ?: return@withLock
                try {
                    val updated = change(active)
                    if ((mutableState.value as? RtcCallState.Active)?.media === active.media) {
                        mutableState.value = updated.copy(participants = activeParticipants(), mediaFailed = false)
                        publishMediaState()
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Napier.e(failure, tag = "rtc") { "Unable to update call media" }
                    if ((mutableState.value as? RtcCallState.Active)?.media === active.media) {
                        mutableState.value =
                            active.copy(
                                participants = activeParticipants(),
                                localVideo = active.media.video,
                                videoMuted = active.videoMuted || active.media.video == null,
                                mediaFailed = true,
                            )
                    }
                }
            }
        }
    }

    private suspend fun publishMediaState() {
        val active = mutableState.value as? RtcCallState.Active ?: return
        send(RoomFrame.UpdateCallMediaState(roomId, active.audioMuted, active.videoMuted))
    }

    private suspend fun receiveFrame(frame: RoomFrame) {
        if (mutableState.value !is RtcCallState.Active) return
        when (frame) {
            is RoomFrame.CreateOffer -> if (frame.roomId == roomId) beginPeer(frame.targetUid, frame)

            is RoomFrame.CreateAnswer -> if (frame.roomId == roomId) beginPeer(frame.targetUid, frame)

            is RoomFrame.RespondAnswer -> if (frame.roomId == roomId) signaling(frame.uid).receiveAnswer(frame)

            is RoomFrame.ReceiveCandidate -> if (frame.roomId == roomId) signaling(frame.uid).receiveCandidate(frame)

            is RoomFrame.PeerLeft -> if (frame.roomId == roomId) removePeer(frame.uid)

            is RoomFrame.PeerMediaState ->
                if (frame.roomId == roomId) {
                    updateParticipant(frame.uid) {
                        it.copy(audioMuted = frame.audioMuted, videoMuted = frame.videoMuted)
                    }
                }

            is RoomFrame.Error -> {
                releaseCall()
                mutableState.value = RtcCallState.Ready(failed = true)
            }

            else -> Unit
        }
    }

    private fun signaling(uid: PrimaryKey) = signals.getOrPut(uid) { RtcSignaling() }

    private fun beginPeer(uid: PrimaryKey, frame: RoomFrame) {
        if (peers[uid]?.isActive == true) return
        val active = mutableState.value as? RtcCallState.Active ?: return
        val signaling = signaling(uid)
        val current = generation
        val identity = Any()
        peerIdentities[uid] = identity
        updateParticipant(uid) { it.copy(connection = RtcConnectionState.Connecting) }
        val job =
            scope.launch(start = CoroutineStart.LAZY) {
                try {
                    connectPeer(uid, frame, active.media, signaling, current, identity)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Napier.e(failure, tag = "rtc") { "Unable to connect call participant" }
                    if (current ==
                        generation
                    ) {
                        updateParticipant(uid) { it.copy(connection = RtcConnectionState.Failed) }
                    }
                } finally {
                    if (peerIdentities[uid] === identity) {
                        peers.remove(uid)
                        peerIdentities.remove(uid)
                        signals.remove(uid)?.cancel()
                    }
                }
            }
        peers[uid] = job
        job.start()
    }

    private suspend fun connectPeer(
        uid: PrimaryKey,
        frame: RoomFrame,
        media: RtcMedia,
        signaling: RtcSignaling,
        current: Long,
        identity: Any,
    ): Unit =
        coroutineScope {
            val events =
                RtcPeerEvents(
                    candidate = { candidate ->
                        scope.launch {
                            if (current == generation && peerIdentities[uid] === identity) {
                                send(RoomFrame.SendCandidate(candidate, roomId, uid))
                            }
                        }
                    },
                    video = { video ->
                        scope.launch {
                            if (current == generation && peerIdentities[uid] === identity) {
                                updateParticipant(
                                    uid,
                                ) { it.copy(video = video) }
                            }
                        }
                    },
                    connection = { connection ->
                        scope.launch {
                            if (current == generation && peerIdentities[uid] === identity) {
                                updateParticipant(uid) { it.copy(connection = connection) }
                            }
                        }
                    },
                )
            val peer = engine.createPeer(media, events, this)
            try {
                when (frame) {
                    is RoomFrame.CreateOffer -> {
                        send(RoomFrame.SendOffer(CustomOffer(peer.createOffer()), roomId, uid))
                        peer.acceptAnswer(signaling.answer.await().answer.sdp)
                    }

                    is RoomFrame.CreateAnswer -> {
                        send(RoomFrame.SendAnswer(CustomAnswer(peer.createAnswer(frame.offer.sdp)), roomId, uid))
                    }

                    else -> error("Unexpected peer negotiation frame")
                }
                signaling.candidates.onEach { peer.addCandidate(it.candidate) }.launchIn(this)
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { peer.close() }
            }
        }

    private fun updateParticipant(uid: PrimaryKey, update: (RtcParticipant) -> RtcParticipant) {
        val active = mutableState.value as? RtcCallState.Active ?: return
        val existing = active.participants.firstOrNull { it.uid == uid }
        val changed = update(existing ?: RtcParticipant(uid))
        val participants =
            if (existing == null) {
                active.participants + changed
            } else {
                active.participants.map { if (it.uid == uid) changed else it }
            }
        mutableState.value = active.copy(participants = participants.toImmutableList())
    }

    private fun activeParticipants() = (mutableState.value as? RtcCallState.Active)?.participants ?: persistentListOf()

    private fun removePeer(uid: PrimaryKey) {
        peerIdentities.remove(uid)
        peers.remove(uid)?.cancel()
        signals.remove(uid)?.cancel()
        val active = mutableState.value as? RtcCallState.Active ?: return
        mutableState.value =
            active.copy(
                participants = active.participants.filterNot { it.uid == uid }.toImmutableList(),
            )
    }

    private suspend fun releaseCall() =
        withContext(NonCancellable) {
            if (mutableState.value == RtcCallState.Leaving) return@withContext
            ++generation
            joining?.cancel()
            joining = null
            val active = mutableState.value as? RtcCallState.Active
            mutableState.value = RtcCallState.Leaving
            peerIdentities.clear()
            val jobs = peers.values.toList()
            peers.clear()
            signals.values.forEach(RtcSignaling::cancel)
            signals.clear()
            jobs.forEach(Job::cancel)
            jobs.forEach { it.join() }
            mediaMutex.withLock { active?.media?.close() }
            if (active != null) {
                try {
                    send(RoomFrame.StopCall(roomId))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    Napier.e(failure, tag = "rtc") { "Unable to leave room call" }
                }
            }
            mutableState.value = RtcCallState.Ready()
        }
}
