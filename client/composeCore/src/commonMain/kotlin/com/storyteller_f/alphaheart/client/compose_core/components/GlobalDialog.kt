/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogState
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog as BasicAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingDialog as Dialog

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
    if (message is GlobalDialogState.Error) {
        Dialog(onDismissRequest = dismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.padding(24.dp)) { GlobalDialogContent(message, scrollState, dismiss) }
        }
        return
    }

    BasicAlertDialog(
        dismiss,
        properties =
        if (message is GlobalDialogState.Loading) {
            DialogProperties(dismissOnClickOutside = false, dismissOnBackPress = false)
        } else {
            DialogProperties()
        },
    ) {
        DialogContainer(onDismissRequest = dismiss.takeUnless { message is GlobalDialogState.Loading }) {
            GlobalDialogContent(message, scrollState, dismiss)
        }
    }
}

@Composable
internal fun GlobalDialogContent(message: GlobalDialogState, scrollState: ScrollState, onDismissRequest: () -> Unit) {
    when (message) {
        is GlobalDialogState.Error -> ReadingFailureSurface(message.throwable, scrollState, onDismissRequest)

        is GlobalDialogState.Loading -> Column(Modifier.height(200.dp)) { LoadingGlobalDialogContent(message) }

        is GlobalDialogState.Custom ->
            Column(Modifier.height(200.dp)) {
                message.content.content()
                Box(contentAlignment = Alignment.CenterEnd) {
                    Button(onDismissRequest) { Text(stringResource(Res.string.close)) }
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
