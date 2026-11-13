/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.ws

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RtcDeliveryTest {
    @Test
    fun closedRecipientPreservesDispatcher() =
        runTest {
            val closed = Channel<Int>()
            val failure = CancellationException("Recipient disconnected")
            closed.cancel(failure)
            val delivered =
                assertFailsWith<IllegalStateException> {
                    sendRtcFrame { closed.send(1) }
                }
            assertFalse(delivered is CancellationException)
            assertTrue(
                generateSequence(delivered as Throwable) { it.cause }
                    .any { it is CancellationException && it.message == failure.message },
            )
            assertTrue(currentCoroutineContext().isActive)
            val nextRecipient = Channel<Int>(1)
            try {
                sendRtcFrame { nextRecipient.send(2) }
                assertEquals(2, nextRecipient.receive())
            } finally {
                nextRecipient.cancel()
            }
        }

    @Test
    fun cancelledDispatcherDoesNotSend() =
        runTest {
            val cancelled = Job().apply { cancel() }
            var sent = false
            assertFailsWith<CancellationException> {
                withContext(cancelled) { sendRtcFrame { sent = true } }
            }
            assertEquals(false, sent)
        }

    @Test
    fun cancellationDuringDeliveryRemainsCancellation() =
        runTest {
            val job = Job()
            assertFailsWith<CancellationException> {
                withContext(job) {
                    sendRtcFrame {
                        job.cancel()
                        throw CancellationException("Dispatcher stopped")
                    }
                }
            }
        }

    @Test
    fun ordinaryDeliveryFailureIsPreserved() =
        runTest {
            val failure = IllegalArgumentException("Delivery failed")
            val delivered =
                assertFailsWith<IllegalArgumentException> {
                    sendRtcFrame { throw failure }
                }
            assertEquals(failure.message, delivered.message)
        }
}
