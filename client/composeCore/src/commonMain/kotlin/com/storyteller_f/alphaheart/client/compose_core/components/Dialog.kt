/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DialogSaveState {
    val dialogShown = MutableStateFlow(false)
    fun markDialogShown() {
        dialogShown.value = true
    }
}

@Composable
fun rememberAlertDialogController(): CustomAlertDialogController =
    remember {
        CustomAlertDialogController()
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
    val positiveButton: String = "Yes",
    val enableCopy: Boolean = false,
    val negativeButton: String? = null,
)

@Composable
fun CustomAlertDialog(controller: CustomAlertDialogController, dismiss: () -> Unit, onClickOk: () -> Unit) {
    val state by controller.state
    CustomAlertDialogInternal(state, dismiss, onClickOk)
}

@Composable
fun CustomAlertDialogInternal(dialogState: CustomAlertDialogState?, dismiss: () -> Unit, onClickOk: () -> Unit) {
    if (dialogState == null) return
    AlertDialog({
        dismiss()
    }, title = {
        dialogState.title?.let {
            Text(it, Modifier.appiumSemantics(text = it))
        }
    }, text = {
        Text(dialogState.message, Modifier.appiumSemantics(text = dialogState.message))
    }, confirmButton = {
        if (dialogState.enableCopy) {
            val clipboardManager = LocalClipboard.current
            val toast = LocalToaster.current
            val scope = rememberCoroutineScope()
            Button({
                dismiss()
                scope.launch {
                    clipboardManager.copyText(dialogState.message, toast)
                }
            }) {
                Text("Copy")
            }
        } else {
            val confirm = {
                dismiss()
                onClickOk()
            }
            Button(
                confirm,
                modifier = Modifier.appiumSemantics(text = dialogState.positiveButton, onClick = confirm),
            ) {
                Text(dialogState.positiveButton)
            }
        }
    }, dismissButton = {
        dialogState.negativeButton?.let { label ->
            TextButton(dismiss, Modifier.appiumSemantics(text = label, onClick = dismiss)) {
                Text(label)
            }
        }
    })
}

@Composable
fun rememberCommonDialogController(): CommonDialogController =
    remember {
        CommonDialogController()
    }

class CommonDialogController(val shown: MutableState<Boolean> = mutableStateOf(false)) {
    fun update(new: Boolean) {
        shown.value = new
    }
}

@Composable
fun DialogContainer(
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    onDismissRequest: (() -> Unit)? = null,
    block: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier =
        Modifier.appiumSemantics(
            description = "dialog",
            onClick = onDismissRequest,
        ),
        shape = RoundedCornerShape(8.dp),
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier.padding(20.dp).verticalScroll(scrollState),
            verticalArrangement = verticalArrangement,
        ) {
            block()
        }
    }
}
