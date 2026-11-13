/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.room

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class CancellationSafeSQLiteDriverTest {
    @Test
    internal fun cancelledRequestWaitsForWorkerResponse() = runTest { verifyCancellation() }

    @Test
    internal fun cancelledAllocationDisposesUndeliveredResource() =
        runTest {
            val started = CompletableDeferred<Unit>()
            val response = CompletableDeferred<Any>()
            var disposed = false
            var delivered = false
            val job =
                launch {
                    awaitWorkerResponse(onCancelled = { disposed = true }) {
                        started.complete(Unit)
                        response.await()
                    }
                    delivered = true
                }
            started.await()
            job.cancel()
            testScheduler.runCurrent()
            assertFalse(disposed)
            response.complete(Any())
            testScheduler.runCurrent()
            assertTrue(disposed)
            assertFalse(delivered)
            assertTrue(job.isCancelled)
        }

    @Test
    internal fun cancelledCleanupWaitsForReleaseResponse() =
        runTest {
            val response = CompletableDeferred<Unit>()
            var recycled = false
            val job =
                launch {
                    try {
                        awaitCancellation()
                    } finally {
                        awaitWorkerFinalizer { response.await() }
                        recycled = true
                    }
                }
            testScheduler.runCurrent()
            job.cancel()
            testScheduler.runCurrent()
            assertFalse(job.isCompleted)
            assertFalse(recycled)
            response.complete(Unit)
            testScheduler.runCurrent()
            assertTrue(recycled)
            assertTrue(job.isCancelled)
        }

    private suspend fun TestScope.verifyCancellation() {
        val requestStarted = CompletableDeferred<Unit>()
        val workerResponse = CompletableDeferred<String>()
        val job =
            launch {
                awaitWorkerResponse {
                    requestStarted.complete(Unit)
                    workerResponse.await()
                }
            }

        requestStarted.await()
        job.cancel()
        testScheduler.runCurrent()
        assertFalse(job.isCompleted)

        workerResponse.complete("done")
        testScheduler.runCurrent()
        assertTrue(job.isCancelled)
    }
}
