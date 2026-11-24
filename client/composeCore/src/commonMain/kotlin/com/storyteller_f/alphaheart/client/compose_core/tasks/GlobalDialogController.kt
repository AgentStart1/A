/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.tasks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.storyteller_f.alphaheart.client.compose_core.error.DialogError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.mapError
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.ExperimentalUuidApi

sealed interface GlobalDialogState {
    data class Loading(
        val title: String? = null,
        val progress: GlobalDialogStateProgress? = null,
        val content: CustomGlobalDialogContent? = null,
    ) : GlobalDialogState

    data class Error(val throwable: Throwable) : GlobalDialogState

    data class Custom(val content: CustomGlobalDialogContent) : GlobalDialogState
}

data class GlobalDialogStateProgress(val value: Long, val total: Long?)

data class CustomGlobalDialogContent(val content: @Composable () -> Unit)

class GlobalDialogContext<C>(val events: MutableSharedFlow<Any>, val sessionManager: C) {
    suspend fun emitEvent(any: Any) {
        events.emit(any)
    }

    suspend fun <T, E> request(block: suspend C.() -> CustomResult<T, E>): CustomResult<T, E> = block(sessionManager)
}

/**
 * Coordinates user-visible dialog work that must survive local UI changes.
 *
 * @param C type of request context used by dialog work.
 * @property scope coroutine scope that owns launched dialog work.
 * @property context request and event context available to dialog work.
 * @property state stack of dialog states currently presented to the user.
 */
class CustomGlobalDialogController<C>(
    val scope: CoroutineScope,
    val context: C,
    val state: MutableStateFlow<PersistentList<GlobalDialogState>> = MutableStateFlow(persistentListOf()),
) {
    internal val mutex = Mutex()

    /** Launches work with a nested dialog controller. */
    fun launch(block: suspend NestedGlobalDialogController<C>.() -> Unit) {
        scope.launch {
            val nestedController = NestedGlobalDialogController(this@CustomGlobalDialogController)
            nestedController.block()
        }
    }

    /** Runs [block] while presenting its loading, custom-content, or error state. */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun <T, E : FailureDetails> useResult(
        block: suspend CustomGlobalDialogController<C>.() -> CustomResult<T, E>,
    ): CustomResult<T, DialogError> =
        mutex.withLock {
            val dialogState = state.value
            if (!dialogState.isEmpty()) {
                CustomResult.Failure(DialogError.Busy())
            } else {
                try {
                    state.value = persistentListOf(GlobalDialogState.Loading())
                    val result =
                        when (val operation = this.block()) {
                            is CustomResult.Success -> operation.value

                            is CustomResult.Failure -> {
                                val error = DialogError.fromError(operation.error)
                                state.value = persistentListOf(GlobalDialogState.Error(error.cause))
                                return@withLock CustomResult.Failure(error)
                            }
                        }
                    if (result is CustomGlobalDialogContent) {
                        state.value = persistentListOf(GlobalDialogState.Custom(result))
                    } else {
                        state.value = persistentListOf()
                    }
                    CustomResult.success(result)
                } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                    state.value = persistentListOf()
                    throw cancellation
                } catch (e: Exception) {
                    Napier.e(e) {
                        "global dialog"
                    }
                    state.value = persistentListOf(GlobalDialogState.Error(e))
                    CustomResult.Failure(DialogError.from(e))
                } finally {
                    if (state.value.lastOrNull() is GlobalDialogState.Loading) state.value = persistentListOf()
                }
            }
        }

    /** Updates the active loading dialog with progress produced by [block]. */
    fun emitProgress(block: (GlobalDialogState.Loading) -> GlobalDialogState.Loading) {
        val currentState = state.value
        val loading = currentState.lastOrNull() as? GlobalDialogState.Loading ?: return
        state.value = currentState.replacingAt(currentState.lastIndex, block(loading))
    }
}

/**
 * Adds a nested dialog state on top of an existing [controller].
 *
 * @param C type of request context used by nested dialog work.
 * @property controller parent controller that owns the state stack.
 */
class NestedGlobalDialogController<C>(val controller: CustomGlobalDialogController<C>) {
    /** Dialog state stack shared with the parent controller. */
    val state: MutableStateFlow<PersistentList<GlobalDialogState>>
        get() = controller.state

    /** Request and event context shared with the parent controller. */
    val context: C
        get() = controller.context

    /** Runs [block] with a nested loading state and removes it when complete. */
    suspend fun <T, E : FailureDetails> useResult(
        block: suspend NestedGlobalDialogController<C>.() -> CustomResult<T, E>,
    ): CustomResult<T, DialogError> {
        val currentState = state.value
        state.value = currentState.adding(GlobalDialogState.Loading())
        try {
            return block().mapError(DialogError::fromError)
        } finally {
            state.value = state.value.removingAt(state.value.lastIndex)
        }
    }

    /** Updates the nested loading state with progress produced by [block]. */
    fun emitProgress(block: (GlobalDialogState.Loading) -> GlobalDialogState.Loading) {
        val value = state.value
        val last = value.lastOrNull() ?: return
        if (last !is GlobalDialogState.Loading) return
        state.value = value.replacingAt(value.lastIndex, block(last))
    }
}

/** Executes [block] against the session manager in this nested dialog context. */
suspend inline fun <T, R, E> NestedGlobalDialogController<GlobalDialogContext<T>>.request(
    noinline block: suspend T.() -> CustomResult<R, E>,
): CustomResult<R, E> = context.request(block)

/** Emits [event] from this nested dialog context. */
suspend inline fun <T> NestedGlobalDialogController<GlobalDialogContext<T>>.emitEvent(event: Any) {
    context.emitEvent(event)
}

/** Executes [block] against the session manager in this dialog context. */
suspend inline fun <T, R, E> CustomGlobalDialogController<GlobalDialogContext<T>>.request(
    noinline block: suspend T.() -> CustomResult<R, E>,
): CustomResult<R, E> = context.request(block)

/** Emits [event] from this dialog context. */
suspend inline fun <T> CustomGlobalDialogController<GlobalDialogContext<T>>.emitEvent(event: Any) {
    context.emitEvent(event)
}
