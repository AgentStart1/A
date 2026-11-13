/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.close
import com.storyteller_f.alphaheart.client.compose_core.error.DialogError
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
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
import org.jetbrains.compose.resources.stringResource
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
    private val mutex = Mutex()

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

@Composable
fun <C> GlobalDialog(state: CustomGlobalDialogController<C>) {
    val message by state.state.collectAsState()
    val dialogState = message.lastOrNull()
    dialogState?.let {
        GlobalDialogInternal(it) {
            state.state.value = persistentListOf()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalDialogInternal(message: GlobalDialogState, dismiss: () -> Unit) {
    val scrollState = rememberScrollState()

    BasicAlertDialog(
        dismiss,
        properties =
        if (message is GlobalDialogState.Loading) {
            DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
        } else {
            DialogProperties()
        },
    ) {
        DialogContainer {
            GlobalDialogContent(message, scrollState, dismiss)
        }
    }
}

@Composable
private fun GlobalDialogContent(message: GlobalDialogState, scrollState: ScrollState, onDismissRequest: () -> Unit) {
    Column(modifier = Modifier.height(200.dp)) {
        when (message) {
            is GlobalDialogState.Error -> {
                ExceptionView(message.throwable, modifier = Modifier.weight(1f).verticalScroll(scrollState))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    val label = stringResource(Res.string.close)
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier.appiumSemantics(text = label, onClick = onDismissRequest),
                    ) {
                        Text(label)
                    }
                }
            }

            is GlobalDialogState.Loading -> {
                LoadingGlobalDialogContent(message)
            }

            is GlobalDialogState.Custom -> {
                message.content.content()
                Box(contentAlignment = Alignment.CenterEnd) {
                    Button({
                        onDismissRequest()
                    }) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingGlobalDialogContent(loading: GlobalDialogState.Loading) {
    if (loading.content != null) {
        loading.content.content()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                loading.title?.let {
                    Text(it)
                    Spacer(Modifier.height(20.dp))
                }
                if (loading.progress != null && loading.progress.total != null) {
                    LinearProgressIndicator(
                        progress = { loading.progress.value.toFloat() / loading.progress.total },
                    )
                } else {
                    Box(modifier = Modifier, contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
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
