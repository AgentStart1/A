/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.CreatePanelTaskRecordsViewModel
import com.storyteller_f.alphaheart.panel.common.CreateTaskRecordSummariesViewModel
import com.storyteller_f.alphaheart.panel.common.submitPanelRequest
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.review_all
import com.storyteller_f.alphaheart.panel.review_all_failures
import com.storyteller_f.alphaheart.panel.review_failure
import com.storyteller_f.alphaheart.panel.review_failure_data_access
import com.storyteller_f.alphaheart.panel.review_failure_model_execution
import com.storyteller_f.alphaheart.panel.review_failure_model_response
import com.storyteller_f.alphaheart.panel.review_failure_unknown
import com.storyteller_f.alphaheart.panel.review_retry
import com.storyteller_f.alphaheart.panel.review_retry_requested
import com.storyteller_f.alphaheart.panel.review_success
import com.storyteller_f.alphaheart.panel.review_task_detail_title
import com.storyteller_f.alphaheart.panel.review_task_failure_type
import com.storyteller_f.alphaheart.panel.review_task_record_meta
import com.storyteller_f.alphaheart.panel.review_task_summary
import com.storyteller_f.alphaheart.panel.worker_records
import com.storyteller_f.shared.model.TaskRecordInfo
import com.storyteller_f.shared.model.TaskRecordSummary
import com.storyteller_f.shared.model.TaskRecordType
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRecordsPage(type: TaskRecordType? = null) {
    if (type != null) {
        TaskRecordDetailContent(type)
        return
    }
    val panelNav = LocalPanelNav.current
    val viewModel = CreateTaskRecordSummariesViewModel()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.worker_records)) },
                navigationIcon = {
                    IconButton({ panelNav.open() }) {
                        Icon(Icons.Default.Menu, stringResource(Res.string.menu))
                    }
                },
            )
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        StateView(viewModel.handler, Modifier.safeArea(paddingValues, direction).fillMaxSize()) { summaries ->
            LazyColumn(contentPadding = PaddingValues(20.dp)) {
                itemsIndexed(summaries, key = { _, summary -> summary.type }) { index, summary ->
                    TaskRecordSummaryItem(summary) {
                        panelNav.gotoTaskRecordDetail(summary.type)
                    }
                    if (index < summaries.lastIndex) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRecordDetailContent(type: TaskRecordType) {
    val panelNav = LocalPanelNav.current
    val summariesViewModel = CreateTaskRecordSummariesViewModel()
    var isSuccess by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var failureType by rememberSaveable { mutableStateOf<String?>(null) }
    val recordsViewModel = CreatePanelTaskRecordsViewModel(type, isSuccess, failureType)
    val dialog = LocalPanelGlobalDialog.current
    val retryRequestedIds by recordsViewModel.retryRequestedIds.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.review_task_detail_title, type.name)) },
                navigationIcon = {
                    IconButton({ panelNav.open() }) {
                        Icon(Icons.Default.Menu, stringResource(Res.string.menu))
                    }
                },
            )
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Column(Modifier.safeArea(paddingValues, direction).fillMaxSize()) {
            StateView(summariesViewModel.handler, Modifier.fillMaxWidth().heightIn(max = 120.dp)) { summaries ->
                summaries.firstOrNull { it.type == type }?.let { summary ->
                    TaskRecordOverview(summary)
                }
            }
            TaskRecordStatusFilter(isSuccess) { selectedStatus ->
                isSuccess = selectedStatus
                failureType = failureTypeForStatus(selectedStatus, failureType)
            }
            if (isSuccess == false || failureType != null) {
                TaskFailureTypeFilter(failureType) { failureType = it }
            }
            StateView(recordsViewModel, modifier = Modifier.weight(1f)) { records ->
                LazyColumn(contentPadding = PaddingValues(20.dp)) {
                    pagingItems(records, key = { it.id }) { index ->
                        records[index]?.let { record ->
                            TaskRecordHistoryItem(
                                record,
                                record.isRetryRequested || record.id in retryRequestedIds,
                            ) { id ->
                                dialog.submitPanelRequest(
                                    request = { recordsViewModel.markForRetry(id) },
                                    onSuccess = {
                                        records.refresh()
                                        summariesViewModel.handler.refresh()
                                    },
                                )
                            }
                            if (index < records.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                        }
                    }
                }
            }
        }
    }
}

internal fun failureTypeForStatus(status: Boolean?, failureType: String?): String? =
    failureType.takeIf { status == false }

@Composable
private fun TaskRecordSummaryItem(summary: TaskRecordSummary, onClick: () -> Unit) {
    ListItem(
        modifier =
        Modifier.clickable(onClick = onClick)
            .appiumSemantics(text = summary.type.name, onClick = onClick),
        headlineContent = { Text(summary.type.name) },
        supportingContent = {
            Text(
                stringResource(
                    Res.string.review_task_summary,
                    summary.successCount,
                    summary.failureCount,
                    summary.retryRequestedCount,
                ),
            )
        },
    )
}

@Composable
private fun TaskRecordOverview(summary: TaskRecordSummary) {
    Text(
        stringResource(
            Res.string.review_task_summary,
            summary.successCount,
            summary.failureCount,
            summary.retryRequestedCount,
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun TaskRecordStatusFilter(selectedStatus: Boolean?, onSelectedStatusChanged: (Boolean?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TaskRecordFilterButton(stringResource(Res.string.review_all), selectedStatus == null) {
            onSelectedStatusChanged(null)
        }
        TaskRecordFilterButton(stringResource(Res.string.review_success), selectedStatus == true) {
            onSelectedStatusChanged(true)
        }
        TaskRecordFilterButton(stringResource(Res.string.review_failure), selectedStatus == false) {
            onSelectedStatusChanged(false)
        }
    }
}

@Composable
private fun TaskFailureTypeFilter(selectedFailureType: String?, onSelectedFailureTypeChanged: (String?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TaskRecordFilterButton(stringResource(Res.string.review_all_failures), selectedFailureType == null) {
            onSelectedFailureTypeChanged(null)
        }
        listOf(
            TaskRecordType.MODEL_RESPONSE_FAILURE,
            TaskRecordType.MODEL_EXECUTION_FAILURE,
            TaskRecordType.DATA_ACCESS_FAILURE,
            TaskRecordType.UNKNOWN_FAILURE,
        ).forEach { failureType ->
            TaskRecordFilterButton(taskFailureLabel(failureType), selectedFailureType == failureType) {
                onSelectedFailureTypeChanged(failureType)
            }
        }
    }
}

@Composable
private fun TaskRecordFilterButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Tab(selected, onClick) { Text(label, Modifier.padding(vertical = 10.dp)) }
}

@Composable
private fun taskFailureLabel(type: String): String =
    when (type) {
        TaskRecordType.MODEL_RESPONSE_FAILURE -> stringResource(Res.string.review_failure_model_response)
        TaskRecordType.MODEL_EXECUTION_FAILURE -> stringResource(Res.string.review_failure_model_execution)
        TaskRecordType.DATA_ACCESS_FAILURE -> stringResource(Res.string.review_failure_data_access)
        TaskRecordType.UNKNOWN_FAILURE -> stringResource(Res.string.review_failure_unknown)
        else -> type
    }

@Composable
private fun TaskRecordHistoryItem(record: TaskRecordInfo, retryRequested: Boolean, onMarkForRetry: (Long) -> Unit) {
    ListItem(
        headlineContent = {
            val status = stringResource(if (record.isSuccess) Res.string.review_success else Res.string.review_failure)
            Text("$status · ${record.objectId}")
        },
        supportingContent = {
            Column {
                Text(
                    stringResource(
                        Res.string.review_task_record_meta,
                        record.id,
                        record.createdTime.formatDisplayTime(),
                    ),
                )
                record.failureType?.let {
                    Text(stringResource(Res.string.review_task_failure_type, taskFailureLabel(it)))
                }
                record.failureReason?.let { reason ->
                    Text(reason, modifier = Modifier.widthIn(max = 720.dp))
                }
            }
        },
        trailingContent = {
            if (!record.isSuccess) {
                val retry = { onMarkForRetry(record.id) }
                val retryLabel =
                    stringResource(if (retryRequested) Res.string.review_retry_requested else Res.string.review_retry)
                Button(
                    enabled = !retryRequested,
                    onClick = retry,
                    modifier =
                    Modifier.appiumSemantics(
                        text = retryLabel,
                        onClick = retry.takeUnless { retryRequested },
                    ),
                ) {
                    Text(retryLabel)
                }
            }
        },
    )
}
