/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.e2e

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class E2eTerminalCleanupTest {
    @Test
    fun closesUnderCallerCancellation(): Unit =
        runBlocking {
            val ready = CompletableDeferred<Unit>()
            val cleaned = CompletableDeferred<Throwable?>()
            val owner =
                launch {
                    withE2eTerminalCleanup(
                        cleanup = { failure ->
                            withContext(NonCancellable) {
                                yield()
                                cleaned.complete(failure)
                            }
                        },
                    ) {
                        ready.complete(Unit)
                        awaitCancellation()
                    }
                }
            ready.await()
            owner.cancelAndJoin()
            assertIs<CancellationException>(cleaned.await())
        }

    @Test
    fun retainsExplicitCancellationIdentity() =
        runBlocking {
            val original = CancellationException("synthetic cancellation")
            val actual =
                assertFailsWith<CancellationException> {
                    withE2eTerminalCleanup(
                        cleanup = { failure ->
                            withContext(NonCancellable) { yield() }
                            assertSame(original, failure)
                        },
                    ) { throw original }
                }
            assertSame(original, actual)
        }
}
