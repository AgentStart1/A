/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Coordinates confirmation; the account's GlobalDialog owns the sign-out request. */
internal class PanelSignoutHost(
    parentScope: CoroutineScope,
    coordination: CoroutineDispatcher,
    private val onConfirmed: () -> Unit,
) {
    private val scope =
        CoroutineScope(
            parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]) + coordination,
        )
    private val mutableConfirming = MutableStateFlow(false)
    val confirming = mutableConfirming.asStateFlow()

    fun request() {
        scope.launch { mutableConfirming.value = true }
    }

    fun cancel() {
        scope.launch { mutableConfirming.value = false }
    }

    fun confirm() {
        scope.launch {
            if (mutableConfirming.value) {
                mutableConfirming.value = false
                onConfirmed()
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
