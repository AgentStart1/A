/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.common

import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogContext
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogState
import com.storyteller_f.alphaheart.client.core.RequestError
import com.storyteller_f.result.CustomResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class PanelSubmissionTest {
    @Test
    fun failedSubmissionShowsTheGlobalErrorAndPreservesTheOpenFormDraft() =
        runTest {
            val controller =
                CustomGlobalDialogController(backgroundScope, GlobalDialogContext(MutableSharedFlow(), Unit))
            val draft = MutableStateFlow("edited nickname and alias")
            var formOpen = true
            val response = CompletableDeferred<CustomResult<Unit, RequestError>>()
            controller.submitPanelRequest(
                request = { response.await() },
                onSuccess = {
                    draft.value = ""
                    formOpen = false
                },
            )
            runCurrent()
            assertIs<GlobalDialogState.Loading>(controller.state.value.single())
            response.complete(
                CustomResult.Failure(RequestError.TransportFailure(IllegalStateException("request failed"))),
            )
            runCurrent()
            val error = assertIs<GlobalDialogState.Error>(controller.state.value.single())
            assertEquals("request failed", error.throwable.message)
            assertTrue(formOpen)
            assertEquals("edited nickname and alias", draft.value)
        }

    @Test
    fun acknowledgedRetryRefreshesTheRecordsAndSummaryOnlyAfterAcknowledgement() =
        runTest {
            val controller =
                CustomGlobalDialogController(backgroundScope, GlobalDialogContext(MutableSharedFlow(), Unit))
            val response = CompletableDeferred<CustomResult<Unit, RequestError>>()
            var recordRefreshes = 0
            var summaryRefreshes = 0
            controller.submitPanelRequest(
                request = { response.await() },
                onSuccess = {
                    recordRefreshes++
                    summaryRefreshes++
                },
            )
            runCurrent()
            assertEquals(0, recordRefreshes)
            assertEquals(0, summaryRefreshes)
            response.complete(CustomResult.Success(Unit))
            runCurrent()
            assertEquals(1, recordRefreshes)
            assertEquals(1, summaryRefreshes)
            assertTrue(controller.state.value.isEmpty())
        }

    @Test
    fun failedRetryIsNotReplayedOrPublishedAsARefresh() =
        runTest {
            val controller =
                CustomGlobalDialogController(backgroundScope, GlobalDialogContext(MutableSharedFlow(), Unit))
            var requests = 0
            var refreshes = 0
            controller.submitPanelRequest(
                request = {
                    requests++
                    CustomResult.Failure(RequestError.TransportFailure(IllegalStateException("retry unavailable")))
                },
                onSuccess = { refreshes++ },
            )
            runCurrent()
            runCurrent()
            assertEquals(1, requests)
            assertEquals(0, refreshes)
            assertIs<GlobalDialogState.Error>(controller.state.value.single())
        }
}
