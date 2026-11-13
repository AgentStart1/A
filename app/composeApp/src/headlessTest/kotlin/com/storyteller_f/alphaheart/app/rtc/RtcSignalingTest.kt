/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import com.storyteller_f.shared.obj.CustomCandidate
import com.storyteller_f.shared.obj.RoomFrame
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RtcSignalingTest {
    @Test
    fun candidatesRemainOrderedUntilNegotiationCompletes() =
        runTest {
            val signaling = RtcSignaling()
            val frames =
                (0 until 24).map { index ->
                    RoomFrame.ReceiveCandidate(CustomCandidate("audio", index, "candidate-$index"), 1L, 2L)
                }
            frames.forEach(signaling::receiveCandidate)
            assertEquals(frames, signaling.candidates.take(frames.size).toList())
            signaling.cancel()
        }

    @Test
    fun peersKeepIndependentCandidates() =
        runTest {
            val first = RtcSignaling()
            val second = RtcSignaling()
            val frame = RoomFrame.ReceiveCandidate(CustomCandidate("audio", 0, "candidate"), 1L, 2L)
            val received = async { second.candidates.take(1).toList() }
            first.cancel()
            second.receiveCandidate(frame)
            assertEquals(listOf(frame), received.await())
            assertTrue(first.answer.isCancelled)
            second.cancel()
        }
}
