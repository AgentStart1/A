/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.test

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FixtureResourcesTest {
    @Test
    fun closeInReverseOrderAfterFailure() =
        runBlocking {
            val events = mutableListOf<String>()
            val original = IllegalStateException("test failure")
            val closeFailure = IllegalArgumentException("close failure")
            val failure =
                assertFailsWith<IllegalStateException> {
                    withFixtureResources(File("build/test/unused")) { resources ->
                        resources.own(AutoCloseable { events.add("network") })
                        resources.own(
                            AutoCloseable {
                                events.add("first")
                                throw closeFailure
                            },
                        )
                        resources.own(AutoCloseable { events.add("second") })
                        throw original
                    }
                }
            assertRetainedFailure(original, failure)
            assertEquals(listOf("second", "first", "network"), events)
            assertEquals(listOf(closeFailure), original.suppressed.toList())
        }

    @Test
    fun closePartiallyStartedContainer() =
        runBlocking {
            val container = FailingContainer()
            assertFailsWith<IllegalStateException> {
                withFixtureResources(File("build/test/unused")) { resources -> resources.start("redis", container) }
            }
            assertEquals(1, container.stops)
        }

    @Test
    fun closeResourcesUnderCancellation() =
        runBlocking {
            val acquired = CompletableDeferred<Unit>()
            val cleanup = CompletableDeferred<Unit>()
            val job =
                launch {
                    withFixtureResources(File("build/test/unused")) { resources ->
                        resources.own(AutoCloseable { cleanup.complete(Unit) })
                        acquired.complete(Unit)
                        CompletableDeferred<Unit>().await()
                    }
                }
            acquired.await()
            job.cancelAndJoin()
            assertEquals(Unit, cleanup.await())
        }

    @Test
    fun preserveCancellationWhenCleanupFails() =
        runBlocking {
            val original = CancellationException("cancelled")
            val failure =
                assertFailsWith<CancellationException> {
                    withFixtureResources(File("build/test/unused")) { resources ->
                        resources.own(AutoCloseable { error("cleanup") })
                        throw original
                    }
                }
            assertRetainedFailure(original, failure)
            assertEquals("cleanup", original.suppressed.single().message)
        }

    @Test
    fun closeWhenBlockingStartupReturnsAfterCancellation() =
        runBlocking {
            val container = BlockingContainer()
            val job =
                launch {
                    withFixtureResources(File("build/test/unused")) { resources ->
                        resources.start("redis", container)
                        error("Cancelled startup must not deliver its container")
                    }
                }
            container.entered.await()
            job.cancel()
            container.release.complete(Unit)
            job.join()
            assertEquals(1, container.stops)
        }
}

private fun assertRetainedFailure(original: Throwable, failure: Throwable) {
    // Coroutine stacktrace recovery can copy an exception across the IO boundary, retaining its original cause.
    assertTrue(generateSequence(failure) { it.cause }.any { it === original })
    assertEquals(original.message, failure.message)
}

private class FailingContainer : GenericContainer<FailingContainer>(DockerImageName.parse("redis:7.4-alpine")) {
    var stops = 0
        private set

    override fun start(): Unit = error("startup failure")

    override fun stop() {
        stops++
    }
}

private class BlockingContainer : GenericContainer<BlockingContainer>(DockerImageName.parse("redis:7.4-alpine")) {
    val entered = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    var stops = 0
        private set

    override fun start() {
        // Models Testcontainers' blocking Java boundary, which cannot cooperate with caller cancellation.
        runBlocking {
            entered.complete(Unit)
            release.await()
        }
    }

    override fun stop() {
        stops++
    }
}
