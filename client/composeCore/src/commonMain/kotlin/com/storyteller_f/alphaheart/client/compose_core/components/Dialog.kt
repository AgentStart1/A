/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.close
import com.storyteller_f.alphaheart.client.compose_core.copy
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAlertDialog as AlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

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
    val positiveButton: String? = null,
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
    // Capture host services before Dialog creates a separate owner with its own platform locals.
    val clipboardManager = LocalClipboard.current
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
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
            Button({
                scope.launch {
                    clipboardManager.copyText(dialogState.message, toast)
                    dismiss()
                }
            }) {
                Text(stringResource(Res.string.copy))
            }
        } else {
            val confirm = {
                dismiss()
                onClickOk()
            }
            val label = dialogState.positiveButton ?: CoreStrings.ok()
            Button(
                confirm,
                modifier = Modifier.appiumSemantics(text = label, onClick = confirm),
            ) {
                Text(label)
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
        modifier = Modifier.appiumSemantics(description = "dialog"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Box {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(scrollState),
                verticalArrangement = verticalArrangement,
                content = block,
            )
            if (onDismissRequest != null) {
                IconButton(onDismissRequest, Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 8.dp)) {
                    Icon(actionIcon(ActionGlyph.X), stringResource(Res.string.close))
                }
            }
        }
    }
}
