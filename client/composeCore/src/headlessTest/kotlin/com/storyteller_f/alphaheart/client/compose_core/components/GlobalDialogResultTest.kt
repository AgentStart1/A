/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import com.storyteller_f.alphaheart.client.compose_core.error.DialogError
import com.storyteller_f.alphaheart.client.core.RequestError
import com.storyteller_f.result.CustomResult
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class GlobalDialogResultTest {
    @Test
    fun `dialog cancellation clears loading`() =
        runTest {
            val controller = CustomGlobalDialogController(this, Unit)
            val cancellation = CancellationException("cancelled")
            assertSame(
                cancellation,
                assertFailsWith<CancellationException> {
                    controller.useResult<Unit, RequestError> { throw cancellation }
                },
            )
            assertTrue(controller.state.value.isEmpty())
        }

    @Test
    fun `dialog publishes operation failure`() =
        runTest {
            val controller = CustomGlobalDialogController(this, Unit)
            val cause = IllegalStateException("request failed")
            val result =
                controller.useResult<Unit, RequestError> {
                    CustomResult.Failure(RequestError.TransportFailure(cause))
                }
            assertIs<CustomResult.Failure<DialogError>>(result)
            assertSame(cause, assertIs<GlobalDialogState.Error>(controller.state.value.single()).throwable)
        }

    @Test
    fun `busy dialog rejects an action without invoking it`() =
        runTest {
            val controller = CustomGlobalDialogController(this, Unit)
            val existing = GlobalDialogState.Loading("existing")
            controller.state.value = persistentListOf(existing)
            val result = controller.useResult<Unit, RequestError> { error("must not execute") }
            assertIs<DialogError.Busy>(assertIs<CustomResult.Failure<DialogError>>(result).error)
            assertSame(existing, controller.state.value.single())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `launched action retains thrown failure after loading finishes`() =
        runTest {
            val controller = CustomGlobalDialogController(this, Unit)
            val cause = IllegalStateException("operation failed")
            controller.launch {
                controller.useResult<Unit, RequestError> { throw cause }
            }
            runCurrent()
            val failure = assertIs<GlobalDialogState.Error>(controller.state.value.single())
            assertSame(cause, failure.throwable)
            runCurrent()
            assertSame(failure, controller.state.value.single())
        }
}
