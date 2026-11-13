/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import com.storyteller_f.shared.obj.RoomFrame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/** Candidates remain queued until this peer has installed its remote description. */
internal class RtcSignaling {
    val answer = CompletableDeferred<RoomFrame.RespondAnswer>()
    private val candidateEvents = Channel<RoomFrame.ReceiveCandidate>(Channel.UNLIMITED)
    val candidates = candidateEvents.receiveAsFlow()

    fun receiveAnswer(frame: RoomFrame.RespondAnswer) {
        answer.complete(frame)
    }

    fun receiveCandidate(frame: RoomFrame.ReceiveCandidate) {
        candidateEvents.trySend(frame)
    }

    fun cancel() {
        answer.cancel()
        candidateEvents.cancel()
    }
}
