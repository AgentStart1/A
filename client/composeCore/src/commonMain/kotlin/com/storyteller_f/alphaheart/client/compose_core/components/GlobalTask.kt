/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.result.CustomResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates keyed background work that should not be cancelled by page changes.
 *
 * @param C type of request context used by background work.
 * @property scope coroutine scope that owns launched work.
 * @property context request and event context available to the work.
 */
class CustomGlobalTask<C>(val scope: CoroutineScope, val context: C) {
    private val mutex = Mutex()

    /** Observable loading state for each active task key. */
    val stateMap: SnapshotStateMap<String, LoadingState?> = mutableStateMapOf()

    /** Launches [block] unless another task with [key] is already running. */
    fun launch(key: String, block: suspend NestedGlobalTask<C>.() -> Unit) {
        scope.launch {
            useInternal(key, block)
        }
    }

    private suspend fun useInternal(key: String, block: suspend NestedGlobalTask<C>.() -> Unit) {
        val newFlow =
            mutex.withLock {
                if (stateMap.contains(key)) {
                    null
                } else {
                    val flow = MutableStateFlow<LoadingState?>(null)
                    stateMap[key] = null
                    flow
                }
            }
        if (newFlow == null) {
            Napier.e {
                "global task lock map failed"
            }
            return
        }
        try {
            coroutineScope {
                val job =
                    launch {
                        newFlow.collectLatest { loadingState ->
                            stateMap[key] = loadingState
                        }
                    }
                try {
                    val nestedTask = NestedGlobalTask(this@CustomGlobalTask, newFlow)
                    nestedTask.block()
                } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    Napier.e(e) {
                        "global task $key failed"
                    }
                } finally {
                    job.cancel()
                }
            }
        } finally {
            mutex.withLock {
                stateMap.remove(key)
            }
        }
    }
}

/**
 * Exposes state and context for one task owned by [controller].
 *
 * @param C type of request context used by the task.
 * @property controller parent task coordinator.
 * @property state loading state for this task.
 */
class NestedGlobalTask<C>(val controller: CustomGlobalTask<C>, val state: MutableStateFlow<LoadingState?>) {
    /** Request and event context shared with the parent coordinator. */
    val context: C get() = controller.context

    /** Runs [block] after marking this task as loading. */
    suspend inline fun <T, E> use(block: suspend () -> CustomResult<T, E>): CustomResult<T, E> {
        state.value = LoadingState.Loading
        return block()
    }
}

class GlobalTaskContext<C>(val events: MutableSharedFlow<Any>, val sessionManager: C) {
    suspend fun emitEvent(any: Any) {
        events.emit(any)
    }

    suspend fun <T, E> request(block: suspend C.() -> CustomResult<T, E>): CustomResult<T, E> = block(sessionManager)
}

/**
 * Executes a request against the session manager in this task context.
 */
suspend inline fun <T, R, E> NestedGlobalTask<GlobalTaskContext<T>>.request(
    noinline block: suspend T.() -> CustomResult<R, E>,
): CustomResult<R, E> = context.request(block)

/** Emits [event] from this task context. */
suspend inline fun <T> NestedGlobalTask<GlobalTaskContext<T>>.emitEvent(event: Any) {
    context.emitEvent(event)
}
