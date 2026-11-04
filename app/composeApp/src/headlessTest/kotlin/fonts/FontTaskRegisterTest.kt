/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.a.app.common.SimpleTaskRegister
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FontTaskRegisterTest {
    @Test
    fun waitsForTheExistingDownloadBeforeReadingItsCache() =
        runTest {
            val register = SimpleTaskRegister(this)
            val release = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            register.lockTask("font") {
                events += "download"
                release.await()
                events += "cached"
            }
            runCurrent()
            val reader = async { register.runTask("font") { events += "read" } }
            runCurrent()
            assertFalse(reader.isCompleted)
            release.complete(Unit)
            reader.await()
            assertEquals(listOf("download", "cached", "read"), events)
            assertEquals(0, register.runningTaskCount.value)
        }

    @Test
    fun cancelledDownloadReleasesItsRegistration() =
        runTest {
            val register = SimpleTaskRegister(this)
            val started = CompletableDeferred<Unit>()
            val owner =
                async {
                    register.runTask("font") {
                        started.complete(Unit)
                        CompletableDeferred<Unit>().await()
                    }
                }
            started.await()
            owner.cancelAndJoin()
            assertEquals(0, register.runningTaskCount.value)
            var reads = 0
            register.runTask("font") { reads++ }
            assertEquals(1, reads)
        }

    @Test
    fun duplicateDownloadDoesNotStartAnotherRequest() =
        runTest {
            val register = SimpleTaskRegister(this)
            val release = CompletableDeferred<Unit>()
            var requests = 0
            register.lockTask("font") {
                requests++
                release.await()
            }
            runCurrent()
            register.lockTask("font") { requests++ }
            runCurrent()
            assertEquals(1, requests)
            release.complete(Unit)
        }

    @Test
    fun pauseCancelsRunningAndQueuedTasksBeforePublishingState() =
        runTest {
            val register = SimpleTaskRegister(this)
            val started = CompletableDeferred<Unit>()
            val paused = CompletableDeferred<Unit>()
            var executions = 0
            val owner =
                async {
                    register.runTask("download") {
                        started.complete(Unit)
                        awaitCancellation()
                    }
                }
            started.await()
            val queued = async { register.runTask("download") { executions++ } }
            runCurrent()
            register.stop("download") { paused.complete(Unit) }
            paused.await()
            runCurrent()
            assertTrue(owner.isCancelled)
            assertTrue(queued.isCancelled)
            assertEquals(0, executions)
            assertEquals(0, register.runningTaskCount.value)
            register.runTask("download") { executions++ }
            assertEquals(1, executions)
        }

    @Test
    fun laterRequestWaitsForPausePublication() =
        runTest {
            val register = SimpleTaskRegister(this)
            val pausing = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            register.stop("download") {
                pausing.complete(Unit)
                release.await()
            }
            pausing.await()
            var executions = 0
            val later = async { register.runTask("download") { executions++ } }
            runCurrent()
            assertFalse(later.isCompleted)
            assertEquals(0, executions)
            release.complete(Unit)
            later.await()
            assertEquals(1, executions)
            assertEquals(0, register.runningTaskCount.value)
        }

    @Test
    fun mergedRequestsStillWaitForEarlierOrdinaryWork() =
        runTest {
            val register = SimpleTaskRegister(this)
            val release = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            val owner =
                async {
                    register.runTask("download") {
                        entered.complete(Unit)
                        release.await()
                    }
                }
            entered.await()
            var resumes = 0
            val first = async { register.runTask("download", merge = true) { resumes++ } }
            val duplicate = async { register.runTask("download", merge = true) { resumes++ } }
            runCurrent()
            assertFalse(first.isCompleted)
            assertTrue(duplicate.isCompleted)
            release.complete(Unit)
            owner.await()
            first.await()
            assertEquals(1, resumes)
            assertEquals(0, register.runningTaskCount.value)
        }

    @Test
    fun pauseWaitsForTheRunningTaskToFinishCleanup() =
        runTest {
            val register = SimpleTaskRegister(this)
            val entered = CompletableDeferred<Unit>()
            val cleaning = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val paused = CompletableDeferred<Unit>()
            register.lockTask("download") {
                try {
                    entered.complete(Unit)
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        cleaning.complete(Unit)
                        release.await()
                    }
                }
            }
            entered.await()
            register.stop("download") { paused.complete(Unit) }
            cleaning.await()
            assertFalse(paused.isCompleted)
            release.complete(Unit)
            paused.await()
            assertEquals(0, register.runningTaskCount.value)
        }

    @Test
    fun cancellingQueuedWorkPreservesOrderAndExclusion() =
        runTest {
            val register = SimpleTaskRegister(this)
            val release = CompletableDeferred<Unit>()
            val entered = CompletableDeferred<Unit>()
            val events = mutableListOf<String>()
            val owner =
                async {
                    register.runTask("download") {
                        events += "owner"
                        entered.complete(Unit)
                        release.await()
                    }
                }
            entered.await()
            val cancelled = async { register.runTask("download") { events += "cancelled" } }
            val next = async { register.runTask("download") { events += "next" } }
            val last = async { register.runTask("download") { events += "last" } }
            runCurrent()
            cancelled.cancelAndJoin()
            assertEquals(listOf("owner"), events)
            release.complete(Unit)
            owner.await()
            next.await()
            last.await()
            assertEquals(listOf("owner", "next", "last"), events)
            assertEquals(0, register.runningTaskCount.value)
        }
}
