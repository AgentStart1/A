/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PdfPreviewHostTest {
    @Test
    fun readyDocumentIsReleasedWhenClosed() =
        runTest {
            var closed = 0
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host =
                PdfPreviewHost(backgroundScope, dispatcher, dispatcher) {
                    FakePdfDocument({ "blob:ready" }) { closed++ }
                }
            runCurrent()
            assertEquals("blob:ready", assertIs<PdfPreviewState.Ready>(host.state.value).url)
            assertEquals(0, closed)
            host.close()
            runCurrent()
            assertEquals(1, closed)
        }

    @Test
    fun replacementWaitsForPreviousDocumentCleanup() =
        runTest {
            var opened = 0
            val closed = mutableListOf<Int>()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host =
                PdfPreviewHost(backgroundScope, dispatcher, dispatcher) {
                    val id = ++opened
                    if (id == 2) assertEquals(listOf(1), closed)
                    FakePdfDocument({ "blob:$id" }) { closed.add(id) }
                }
            runCurrent()
            host.refresh()
            runCurrent()
            assertEquals("blob:2", assertIs<PdfPreviewState.Ready>(host.state.value).url)
            assertEquals(listOf(1), closed)
            host.close()
            runCurrent()
            assertEquals(listOf(1, 2), closed)
        }

    @Test
    fun failedRequestIsClosedAndCanBeRetried() =
        runTest {
            var fail = true
            var closed = 0
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host =
                PdfPreviewHost(backgroundScope, dispatcher, dispatcher) {
                    FakePdfDocument({
                        check(!fail) { "Synthetic request failed" }
                        "blob:retry"
                    }) { closed++ }
                }
            runCurrent()
            assertEquals("Synthetic request failed", assertIs<PdfPreviewState.Failed>(host.state.value).cause.message)
            assertEquals(1, closed)
            fail = false
            host.refresh()
            runCurrent()
            assertEquals("blob:retry", assertIs<PdfPreviewState.Ready>(host.state.value).url)
            host.close()
            runCurrent()
            assertEquals(2, closed)
        }

    @Test
    fun closingAbortsTheOwnedPendingRequest() =
        runTest {
            var closed = 0
            var awaiting = false
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host =
                PdfPreviewHost(backgroundScope, dispatcher, dispatcher) {
                    FakePdfDocument({
                        awaiting = true
                        awaitCancellation()
                    }) { closed++ }
                }
            runCurrent()
            assertTrue(awaiting)
            host.close()
            runCurrent()
            assertEquals(1, closed)
            assertIs<PdfPreviewState.Loading>(host.state.value)
        }

    @Test
    fun cancelledUrlDeliveryStillClosesTheDocument() =
        runTest {
            var closed = 0
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host =
                PdfPreviewHost(backgroundScope, dispatcher, dispatcher) {
                    FakePdfDocument({
                        currentCoroutineContext().cancel()
                        "blob:cancelled"
                    }) { closed++ }
                }
            runCurrent()
            assertEquals(1, closed)
            assertIs<PdfPreviewState.Loading>(host.state.value)
            host.close()
        }
}

private class FakePdfDocument(private val load: suspend () -> String, private val release: () -> Unit) :
    PdfPreviewDocument {
    override suspend fun awaitUrl(): String = load()
    override fun close() = release()
}
