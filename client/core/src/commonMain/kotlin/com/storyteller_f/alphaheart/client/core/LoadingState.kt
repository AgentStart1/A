/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface LoadingState {
    data object Loading : LoadingState
    data class Processing(val value: Long, val total: Long) : LoadingState
    data class Error(val e: Throwable) : LoadingState
    data object Done : LoadingState
}

suspend fun <T> LoadingHandler<T>.request(
    onSave: suspend (T) -> Unit,
    service: suspend () -> CustomResult<T, FailureDetails>,
) {
    if (state.value is LoadingState.Loading) return
    state.markLoading()
    service().onSuccess { res ->
        if (res != null) {
            onSave(res)
            state.markDone()
        } else {
            state.markError("content not found")
        }
    }.onFailure {
        state.markError(it.cause)
    }
}

interface LoadingHandler<T> {
    val state: MutableStateFlow<LoadingState?>
    val data: StateFlow<T?>

    fun refresh()
}

class FixedLoadingHandler<T> : LoadingHandler<T> {
    override val state: MutableStateFlow<LoadingState?> = MutableStateFlow(null)
    override val data = MutableStateFlow<T?>(null)

    fun done(t: T) {
        data.value = t
    }

    override fun refresh() = Unit
}

class SimpleLoadingHandler<T>(val scope: CoroutineScope, val loader: suspend () -> CustomResult<T, FailureDetails>) :
    LoadingHandler<T> {
    override val state: MutableStateFlow<LoadingState?> = MutableStateFlow(null)
    override val data: MutableStateFlow<T?> = MutableStateFlow(null)

    init {
        refresh()
    }

    fun done(t: T) {
        data.value = t
        state.value = LoadingState.Done
    }

    override fun refresh() {
        scope.launch {
            request({
                data.value = it
            }) {
                loader()
            }
        }
    }
}

fun MutableStateFlow<LoadingState?>.markError(e: Throwable) {
    value = LoadingState.Error(e)
}

fun MutableStateFlow<LoadingState?>.markDone() {
    value = LoadingState.Done
}

fun MutableStateFlow<LoadingState?>.markError(e: String) {
    value = LoadingState.Error(Exception(e))
}

fun MutableStateFlow<LoadingState?>.markLoading() {
    value = LoadingState.Loading
}

val MutableStateFlow<LoadingState?>.isLoading get() = this.value is LoadingState.Loading
