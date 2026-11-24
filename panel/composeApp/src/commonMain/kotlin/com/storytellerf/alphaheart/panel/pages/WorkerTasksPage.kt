/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.panel.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.storyteller_f.alphaheart.client.compose_core.components.CachedRefreshIndicator
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ExceptionCell
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.CreateWorkerTasksViewModel
import com.storyteller_f.alphaheart.panel.common.WorkerTaskEditorState
import com.storyteller_f.alphaheart.panel.common.WorkerTaskEffect
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.no_worker_task_configurations
import com.storyteller_f.alphaheart.panel.review_enabled
import com.storyteller_f.alphaheart.panel.save
import com.storyteller_f.alphaheart.panel.save_failed
import com.storyteller_f.alphaheart.panel.worker_task_configurations
import com.storyteller_f.alphaheart.panel.worker_task_fetch_size
import com.storyteller_f.alphaheart.panel.worker_task_invalid_configuration
import com.storyteller_f.alphaheart.panel.worker_task_save_failed_detail
import com.storyteller_f.alphaheart.panel.worker_task_wait_duration
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField

/** Displays persisted worker task configurations and their editable values. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkerTasksPage() {
    val panelNav = LocalPanelNav.current
    val viewModel = CreateWorkerTasksViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dialog = rememberAlertDialogController()
    val saveFailedTitle = stringResource(Res.string.save_failed)
    val invalidConfiguration = stringResource(Res.string.worker_task_invalid_configuration)
    val saveFailedDetail = stringResource(Res.string.worker_task_save_failed_detail)
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effects.collect { effect ->
                when (effect) {
                    is WorkerTaskEffect.SaveFailed ->
                        dialog.showMessage(
                            saveFailedTitle,
                            effect.message ?: saveFailedDetail,
                        )

                    WorkerTaskEffect.InvalidConfiguration -> dialog.showMessage(saveFailedTitle, invalidConfiguration)
                }
            }
        }
    }
    CustomAlertDialog(dialog, dialog::close, dialog::close)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.worker_task_configurations)) },
                navigationIcon = {
                    IconButton({ panelNav.open() }) {
                        Icon(Icons.Default.Menu, stringResource(Res.string.menu))
                    }
                },
            )
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Box(Modifier.safeArea(paddingValues, direction).fillMaxSize()) {
            WorkerTaskList(uiState, viewModel)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.WorkerTaskList(
    uiState: com.storyteller_f.alphaheart.panel.common.WorkerTasksUiState,
    viewModel: com.storyteller_f.alphaheart.panel.common.WorkerTasksViewModel,
) {
    if (uiState.isLoading && uiState.configs.isEmpty()) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
    } else {
        Column(Modifier.fillMaxSize()) {
            if (uiState.configs.isEmpty() && uiState.error != null) {
                CenterBox { ExceptionCell(IllegalStateException(uiState.error), viewModel::refresh) }
            } else if (uiState.configs.isEmpty()) {
                CenterBox { Text(stringResource(Res.string.no_worker_task_configurations)) }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier.widthIn(max = 740.dp).fillMaxWidth(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        itemsIndexed(uiState.configs, key = { _, config -> config.type }) { index, config ->
                            WorkerTaskEditor(
                                config = config,
                                showDivider = index < uiState.configs.lastIndex,
                                onEnabledChange = { viewModel.updateEnabled(config.type, it) },
                                onFetchSizeChange = { viewModel.updateFetchSize(config.type, it) },
                                onWaitDurationChange = { viewModel.updateWaitDuration(config.type, it) },
                                onSave = { viewModel.save(config.type) },
                            )
                        }
                    }
                }
            }
        }
    }
    if (uiState.configs.isNotEmpty()) {
        val refreshState =
            when {
                uiState.isLoading -> LoadingState.Loading
                uiState.error != null -> LoadingState.Error(IllegalStateException(uiState.error))
                else -> LoadingState.Done
            }
        CachedRefreshIndicator(refreshState, viewModel::refresh)
    }
}

@Composable
private fun WorkerTaskEditor(
    config: WorkerTaskEditorState,
    showDivider: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onFetchSizeChange: (String) -> Unit,
    onWaitDurationChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            WorkerTaskHeader(config, onEnabledChange)
            WorkerNumericField(config, false, onFetchSizeChange)
            WorkerNumericField(config, true, onWaitDurationChange)
            Button(
                onClick = onSave,
                enabled = !config.isSaving,
                modifier =
                Modifier.align(Alignment.End).appiumSemantics(
                    description = "save worker ${config.type.name}",
                    onClick = onSave.takeUnless { config.isSaving },
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Row(
                        Modifier.alpha(if (config.isSaving) 0f else 1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(actionIcon(ActionGlyph.CHECK), null)
                        Text(stringResource(Res.string.save))
                    }
                    if (config.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
        }
        if (showDivider) HorizontalDivider(Modifier.padding(top = 24.dp))
    }
}

@Composable
private fun WorkerNumericField(config: WorkerTaskEditorState, waiting: Boolean, onChange: (String) -> Unit) {
    val value = if (waiting) config.waitDurationMillis else config.fetchSize
    val label = if (waiting) Res.string.worker_task_wait_duration else Res.string.worker_task_fetch_size
    val description = if (waiting) "wait duration" else "fetch size"
    val fieldLabel = stringResource(label)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(fieldLabel, modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            enabled = !config.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier =
            Modifier.width(132.dp).appiumSemantics(
                description = "worker ${config.type.name} $description",
                input = true,
                inputValue = value,
                onInputValueChange = onChange.takeUnless { config.isSaving },
            ),
        )
    }
}

@Composable
private fun WorkerTaskHeader(config: WorkerTaskEditorState, onEnabledChange: (Boolean) -> Unit) {
    val toggle = { onEnabledChange(!config.isEnabled) }
    Text(config.type.name, style = MaterialTheme.typography.titleMedium)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.review_enabled), Modifier.weight(1f))
        Switch(
            checked = config.isEnabled,
            enabled = !config.isSaving,
            onCheckedChange = onEnabledChange,
            modifier =
            Modifier.appiumSemantics(
                description = "worker ${config.type.name} enabled ${config.isEnabled}",
                onClick = toggle.takeUnless { config.isSaving },
            ),
        )
    }
}
