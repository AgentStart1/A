/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.ws

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.cloud.core.service.checkRootReadPermission
import com.storyteller_f.alphaheart.cloud.error.WebSocketError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.CustomAnswer
import com.storyteller_f.shared.obj.CustomOffer
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import io.github.aakira.napier.Napier
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.sendSerialized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

data class RtcFrame(val frame: RoomFrame, val uid: PrimaryKey, val session: DefaultWebSocketServerSession)

data class RtcUser(val uid: PrimaryKey, val session: DefaultWebSocketServerSession)

data class RtcMediaState(val audioMuted: Boolean = false, val videoMuted: Boolean = false)

data class RtcSession(
    val roomId: PrimaryKey,
    val uidList: MutableList<RtcUser> = mutableListOf(),
    val socketMap: MutableMap<PrimaryKey, DefaultWebSocketServerSession> = mutableMapOf(),
    val offerList: MutableMap<PrimaryKey, MutableMap<PrimaryKey, CustomOffer>> = mutableMapOf(),
    val answerList: MutableMap<PrimaryKey, MutableMap<PrimaryKey, CustomAnswer>> = mutableMapOf(),
    val mediaStateMap: MutableMap<PrimaryKey, RtcMediaState> = mutableMapOf(),
)

val rtcSession = mutableMapOf<PrimaryKey, RtcSession>()
val rtcChannel =
    Channel<RtcFrame> {
    }

/**
 * 结束会话.
 */
private suspend fun processStopCall(frame: RoomFrame.StopCall, uid: PrimaryKey) {
    val roomId = frame.roomId
    val session = rtcSession[roomId] ?: return
    if (session.uidList.none { it.uid == uid }) {
        return
    }
    notifyPeerLeft(session, uid)
    cleanupRtcUser(session, uid)
    if (session.uidList.isEmpty()) {
        rtcSession.remove(roomId)
    }
}

private suspend fun notifyPeerLeft(session: RtcSession, uid: PrimaryKey) {
    session.socketMap.filterKeys {
        it != uid
    }.values.forEach { socket ->
        captureFailure(WebSocketError::from) {
            socket.sendFrame(RoomFrame.PeerLeft(uid, session.roomId))
        }.onFailure { e ->
            Napier.e(e.cause) {
                "send PeerLeft"
            }
        }
    }
}

private suspend fun syncRtcMediaState(session: RtcSession, uid: PrimaryKey, socket: DefaultWebSocketServerSession) {
    session.mediaStateMap.filterKeys {
        it != uid
    }.forEach { (peerUid, state) ->
        captureFailure(WebSocketError::from) {
            socket.sendFrame(
                RoomFrame.PeerMediaState(
                    uid = peerUid,
                    roomId = session.roomId,
                    audioMuted = state.audioMuted,
                    videoMuted = state.videoMuted,
                ),
            )
        }.onFailure { e ->
            Napier.e(e.cause) {
                "sync PeerMediaState"
            }
        }
    }
}

private fun cleanupRtcUser(session: RtcSession, uid: PrimaryKey) {
    session.uidList.removeIf {
        it.uid == uid
    }
    session.socketMap.remove(uid)
    session.offerList.remove(uid)
    session.answerList.remove(uid)
    session.mediaStateMap.remove(uid)
    session.offerList.values.forEach {
        it.remove(uid)
    }
    session.answerList.values.forEach {
        it.remove(uid)
    }
}

/**
 * 发起会话.
 */
private suspend fun processStartCall(
    frame: RoomFrame.StartCall,
    backend: Backend,
    uid: PrimaryKey,
    session: DefaultWebSocketServerSession,
) {
    Napier.i {
        "processStartCall $uid"
    }

    val roomId = frame.roomId
    backend.checkRootReadPermission(ObjectType.ROOM, roomId, uid).onSuccess { permission ->
        if (permission == null) {
            session.sendFrame(RoomFrame.Error("no permission"))
        } else {
            val list =
                rtcSession.getOrPut(roomId) {
                    RtcSession(roomId)
                }
            cleanupRtcUser(list, uid)
            list.uidList.add(RtcUser(uid, session))
            list.socketMap[uid] = session
            list.mediaStateMap[uid] = RtcMediaState()
            syncRtcMediaState(list, uid, session)
        }
    }.onFailure {
        session.sendFrame(RoomFrame.Error(it.cause.message?.toString().orEmpty()))
    }
}

/**
 * answer 创建成功，要求另一个用户回应answer.
 */
private suspend fun processSendAnswer(frame: RoomFrame.SendAnswer, uid: PrimaryKey) {
    val answer = frame.answer
    val session = rtcSession[frame.roomId]
    if (session != null) {
        session.socketMap[frame.targetUid]?.sendFrame(RoomFrame.RespondAnswer(answer, frame.roomId, uid))
        session.answerList.getOrPut(frame.targetUid) {
            mutableMapOf()
        }[uid] = answer
    }
}

/**
 * offer 创建成功，要求另一个用户创建answer.
 */
private suspend fun processSendOffer(frame: RoomFrame.SendOffer, uid: PrimaryKey) {
    val offer = frame.offer
    val session = rtcSession[frame.roomId]
    Napier.i {
        "processSendOffer $frame ${session?.socketMap[frame.targetUid]}"
    }
    if (session != null) {
        session.socketMap[frame.targetUid]?.sendFrame(RoomFrame.CreateAnswer(uid, offer, frame.roomId))
        session.offerList.getOrPut(uid) {
            mutableMapOf()
        }[frame.targetUid] = offer
    }
}

private suspend fun processUpdateCallMediaState(frame: RoomFrame.UpdateCallMediaState, uid: PrimaryKey) {
    val session = rtcSession[frame.roomId] ?: return
    if (session.uidList.none { it.uid == uid }) {
        return
    }
    val state =
        RtcMediaState(
            audioMuted = frame.audioMuted,
            videoMuted = frame.videoMuted,
        )
    session.mediaStateMap[uid] = state
    session.socketMap.filterKeys {
        it != uid
    }.values.forEach { socket ->
        socket.sendFrame(
            RoomFrame.PeerMediaState(
                uid = uid,
                roomId = frame.roomId,
                audioMuted = frame.audioMuted,
                videoMuted = frame.videoMuted,
            ),
        )
    }
}

suspend fun listenerRoomRTC() {
    while (true) {
        cleanupInactiveRtcUsers()
        rtcSession.values.forEach {
            it.uidList.forEachIndexed { frontUserIndex, frontRtcUser ->
                val frontSocket = frontRtcUser.session
                if (!frontSocket.isActive) return@forEachIndexed
                it.uidList.forEachIndexed { backUserIndex, backRtcUser ->
                    processRTCSession(frontUserIndex, backUserIndex, backRtcUser, it, frontRtcUser)
                }
            }
        }
        withContext(Dispatchers.IO) {
            delay(1000)
        }
    }
}

private suspend fun cleanupInactiveRtcUsers() {
    val emptyRooms = mutableListOf<PrimaryKey>()
    rtcSession.values.forEach { session ->
        val inactiveUids =
            session.uidList.filterNot {
                it.session.isActive
            }.map(RtcUser::uid)
        inactiveUids.forEach { uid ->
            notifyPeerLeft(session, uid)
            cleanupRtcUser(session, uid)
        }
        if (session.uidList.isEmpty()) {
            emptyRooms.add(session.roomId)
        }
    }
    emptyRooms.forEach(rtcSession::remove)
}

private suspend fun processRTCSession(
    frontUserIndex: Int,
    backUserIndex: Int,
    backRtcUser: RtcUser,
    session: RtcSession,
    frontRtcUser: RtcUser,
) {
    if (frontUserIndex >= backUserIndex) return
    val backSocket = backRtcUser.session
    if (!backSocket.isActive) return
    val offer = session.offerList.getOrPut(frontRtcUser.uid) { mutableMapOf() }[backRtcUser.uid]
    Napier.i {
        "processRTCSession $frontUserIndex ${frontRtcUser.uid} $backUserIndex ${backRtcUser.uid} ${offer ?: "<none>"}"
    }
    if (offer != null) {
        val answer = session.answerList.getOrPut(frontRtcUser.uid) { mutableMapOf() }[backRtcUser.uid]
        if (answer != null) return
        try {
            val frame = RoomFrame.CreateAnswer(frontRtcUser.uid, offer, session.roomId)
            backSocket.sendFrame(frame)
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Napier.e(e) {
                "send CreateAnswer"
            }
        }
        return
    }
    try {
        val frame = RoomFrame.CreateOffer(backRtcUser.uid, session.roomId)
        frontRtcUser.session.sendFrame(frame)
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        Napier.e(e) {
            "send CreateOffer"
        }
    }
}

suspend fun listenerRtcChannel(backend: Backend) {
    for (rtcFrame in rtcChannel) {
        val frame = rtcFrame.frame
        val uid = rtcFrame.uid
        Napier.d(tag = "rtc") {
            "receive $frame $uid"
        }
        try {
            when (frame) {
                is RoomFrame.SendOffer -> {
                    processSendOffer(frame, uid)
                }

                is RoomFrame.SendAnswer -> {
                    processSendAnswer(frame, uid)
                }

                is RoomFrame.StartCall -> {
                    processStartCall(frame, backend, uid, rtcFrame.session)
                }

                is RoomFrame.StopCall -> {
                    processStopCall(frame, uid)
                }

                is RoomFrame.SendCandidate -> {
                    processSendCandidate(frame, uid)
                }

                is RoomFrame.UpdateCallMediaState -> {
                    processUpdateCallMediaState(frame, uid)
                }

                else -> {
                }
            }
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (e: Throwable) {
            Napier.e(e) {
                "catch exception"
            }
        }
    }
}

suspend fun processSendCandidate(frame: RoomFrame.SendCandidate, uid: PrimaryKey) {
    val session = rtcSession[frame.roomId] ?: return
    val targetSession = session.socketMap[frame.targetUid] ?: return
    val f = RoomFrame.ReceiveCandidate(frame.candidate, frame.roomId, uid)
    targetSession.sendFrame(f)
}

suspend fun DefaultWebSocketServerSession.sendFrame(frame: RoomFrame) {
    sendRtcFrame { sendSerialized(frame) }
}
