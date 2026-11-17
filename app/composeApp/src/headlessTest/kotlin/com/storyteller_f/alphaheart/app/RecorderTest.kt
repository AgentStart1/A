/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import com.storyteller_f.alphaheart.app.utils.Recorder
import com.storyteller_f.alphaheart.app.utils.RecordingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class RecorderTest {
    @Test
    fun failedStartReleasesSessionAndAllowsRetry() =
        runTest {
            val failure = IllegalStateException("Capture unavailable")
            val failed = RecordingFixture(start = { throw failure })
            val next = RecordingFixture()
            val sessions = ArrayDeque(listOf(failed, next))
            val recorder = Recorder { sessions.removeFirst() }
            assertSame(failure, assertFailsWith<IllegalStateException> { recorder.startRecord() })
            assertEquals(1, failed.stops)
            assertIs<RecordingState.Idle>(recorder.state.value)
            recorder.startRecord()
            assertSame(next, assertIs<RecordingState.Active>(recorder.state.value).session)
            recorder.cancelRecord()
        }

    @Test
    fun cancelledStartReleasesSessionWithoutPublishingFailure() =
        runTest {
            val ready = CompletableDeferred<Unit>()
            val session = RecordingFixture(start = { ready.await() })
            val recorder = Recorder { session }
            val job = launch(start = CoroutineStart.UNDISPATCHED) { recorder.startRecord() }
            assertIs<RecordingState.Starting>(recorder.state.value)
            job.cancel(CancellationException("Recorder closed"))
            job.join()
            assertEquals(1, session.stops)
            assertIs<RecordingState.Idle>(recorder.state.value)
        }

    @Test
    fun lateStartCannotReplaceNewRecording() =
        runTest {
            val ready = CompletableDeferred<Unit>()
            val old = RecordingFixture(start = { ready.await() })
            val next = RecordingFixture()
            val sessions = ArrayDeque(listOf(old, next))
            val recorder = Recorder { sessions.removeFirst() }
            val pending = launch(start = CoroutineStart.UNDISPATCHED) { recorder.startRecord() }
            recorder.cancelRecord()
            recorder.startRecord()
            ready.complete(Unit)
            pending.join()
            assertEquals(1, old.stops)
            assertSame(next, assertIs<RecordingState.Active>(recorder.state.value).session)
            recorder.cancelRecord()
            assertEquals(1, next.stops)
            assertIs<RecordingState.Idle>(recorder.state.value)
        }

    @Test
    fun cleanupFailureIsSuppressedOnOriginalStartFailure() =
        runTest {
            val failure = IllegalStateException("Capture unavailable")
            val cleanup = IllegalArgumentException("Cleanup unavailable")
            val recorder = Recorder { RecordingFixture(start = { throw failure }, stop = { throw cleanup }) }
            assertSame(failure, assertFailsWith<IllegalStateException> { recorder.startRecord() })
            assertEquals(listOf(cleanup), failure.suppressedExceptions)
            assertIs<RecordingState.Idle>(recorder.state.value)
        }

    @Test
    fun failedStopDoesNotRetainActiveState() =
        runTest {
            val recorder = Recorder { RecordingFixture(stop = { error("Stop failed") }) }
            recorder.startRecord()
            assertFailsWith<IllegalStateException> { recorder.stopRecord() }
            assertIs<RecordingState.Idle>(recorder.state.value)
        }
}
