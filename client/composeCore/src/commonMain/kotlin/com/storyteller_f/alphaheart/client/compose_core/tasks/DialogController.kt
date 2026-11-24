/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.tasks

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.MutableStateFlow

class DialogSaveState {
    val dialogShown = MutableStateFlow(false)
    fun markDialogShown() {
        dialogShown.value = true
    }
}

class CustomAlertDialogController(val state: MutableState<CustomAlertDialogState?> = mutableStateOf(null)) {
    fun showMessage(title: String, message: String, enableCopy: Boolean = false) {
        state.value = CustomAlertDialogState(title, message, enableCopy = enableCopy)
    }

    fun showTitle(title: String) {
        state.value = CustomAlertDialogState(title, "")
    }

    fun close() {
        state.value = null
    }

    fun showErrorMessage(e: Throwable) {
        state.value = CustomAlertDialogState(e.message.orEmpty(), e.stackTraceToString())
    }
}

data class CustomAlertDialogState(
    val title: String?,
    val message: String,
    val positiveButton: String? = null,
    val enableCopy: Boolean = false,
    val negativeButton: String? = null,
)

class CommonDialogController(val shown: MutableState<Boolean> = mutableStateOf(false)) {
    fun update(new: Boolean) {
        shown.value = new
    }
}
