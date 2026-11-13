/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import com.storyteller_f.alphaheart.client.core.SessionManager
import com.storyteller_f.alphaheart.client.core.recordSigninActivity
import com.storyteller_f.result.onSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

/** Actual pointer/key input reports activity; background work and passive WS traffic do not. */
class SigninActivity(private val coordination: CoroutineDispatcher = Dispatchers.Default) {
    private val events = Channel<Unit>(Channel.CONFLATED)

    fun recordInput() {
        events.trySend(Unit)
    }

    suspend fun observeInput(manager: SessionManager<*>, panel: Boolean) =
        withContext(coordination) {
            var last = TimeSource.Monotonic.markNow() - kotlin.time.Duration.parse("5m")
            while (true) {
                events.receive()
                if (manager.model.userHandler.data.value == null || last.elapsedNow().inWholeSeconds < 300) continue
                manager.recordSigninActivity(panel).onSuccess { last = TimeSource.Monotonic.markNow() }
            }
        }
}

fun Modifier.signinActivity(manager: SessionManager<*>, panel: Boolean = false): Modifier =
    composed {
        val activity = remember(manager, panel) { SigninActivity() }
        onPreviewKeyEvent {
            activity.recordInput()
            false
        }.pointerInput(manager) {
            coroutineScope {
                launch { activity.observeInput(manager, panel) }
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) activity.recordInput()
                    }
                }
            }
        }
    }
