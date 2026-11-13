/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import io.ktor.server.application.ApplicationStopped
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.TestApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestHostLifecycleTest {
    @Test
    fun failedHostStopsBeforeReturningFailure() =
        runBlocking {
            var stopped = false
            val host =
                TestApplication {
                    environment { config = MapApplicationConfig() }
                    application { monitor.subscribe(ApplicationStopped) { stopped = true } }
                }
            val failure =
                assertFailsWith<IllegalStateException> {
                    useTestHost(host) { error("Synthetic body failure") }
                }
            assertEquals("Synthetic body failure", failure.message)
            assertTrue(stopped)
        }

    @Test
    fun cleanupPreservesCancellationAndSuppressedFailure() =
        runBlocking {
            val failure =
                assertFailsWith<CancellationException> {
                    withCleanupPreservingFailure(cleanup = { error("Synthetic cleanup failure") }) {
                        throw CancellationException("Synthetic cancellation")
                    }
                }
            assertEquals("Synthetic cancellation", failure.message)
            assertEquals(listOf("Synthetic cleanup failure"), failure.suppressedExceptions.map { it.message })
        }

    @Test
    fun successfulBodyReturnsValueAfterCleanup() =
        runBlocking {
            var cleaned = false
            val result = withCleanupPreservingFailure(cleanup = { cleaned = true }) { 42 }
            assertTrue(cleaned)
            assertEquals(42, result)
        }

    @Test
    fun successfulBodyPropagatesCleanupFailure() =
        runBlocking {
            var cleanupCalls = 0
            val failure =
                assertFailsWith<IllegalStateException> {
                    withCleanupPreservingFailure(cleanup = {
                        cleanupCalls++
                        error("Synthetic cleanup failure")
                    }) { 42 }
                }
            assertEquals("Synthetic cleanup failure", failure.message)
            assertEquals(1, cleanupCalls)
        }
}
