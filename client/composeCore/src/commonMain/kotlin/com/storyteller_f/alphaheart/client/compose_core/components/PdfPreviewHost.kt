/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal sealed interface PdfPreviewState {
    data object Loading : PdfPreviewState
    data class Ready(val url: String) : PdfPreviewState
    data class Failed(val cause: Throwable) : PdfPreviewState
}

/** Owns both an in-flight request and its resulting document URL until closed. */
internal interface PdfPreviewDocument : AutoCloseable {
    suspend fun awaitUrl(): String
}

internal class PdfPreviewHost(
    parentScope: CoroutineScope,
    private val coordination: CoroutineDispatcher,
    private val worker: CoroutineDispatcher,
    private val openDocument: () -> PdfPreviewDocument,
) {
    private val scope =
        CoroutineScope(
            parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]) + coordination,
        )
    private val mutableState = MutableStateFlow<PdfPreviewState>(PdfPreviewState.Loading)
    val state = mutableState.asStateFlow()
    private var loading: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            loading?.cancelAndJoin()
            mutableState.value = PdfPreviewState.Loading
            loading =
                scope.launch {
                    try {
                        withContext(worker) {
                            openDocument().use { document ->
                                val url = document.awaitUrl()
                                withContext(coordination) {
                                    mutableState.value = PdfPreviewState.Ready(url)
                                    awaitCancellation()
                                }
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        Napier.e("PDF preview failed", failure)
                        mutableState.value = PdfPreviewState.Failed(failure)
                    }
                }
        }
    }

    fun close() {
        scope.cancel()
    }
}
