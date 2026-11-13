/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.CreatePanelTaskRecordsViewModel
import com.storyteller_f.alphaheart.panel.common.CreateTaskRecordSummariesViewModel
import com.storyteller_f.alphaheart.panel.worker_records
import com.storyteller_f.shared.model.TaskRecordInfo
import com.storyteller_f.shared.model.TaskRecordSummary
import com.storyteller_f.shared.model.TaskRecordType
import org.jetbrains.compose.resources.stringResource

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
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        StateView(viewModel.handler, Modifier.safeArea(paddingValues, direction).fillMaxSize()) { summaries ->
            LazyColumn {
                items(summaries, key = { it.type }) { summary ->
                    TaskRecordSummaryItem(summary) {
                        panelNav.gotoTaskRecordDetail(summary.type)
                    }
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
    val retryRequestedIds by recordsViewModel.retryRequestedIds.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(type.name) },
                navigationIcon = {
                    IconButton({ panelNav.open() }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
            )
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Column(Modifier.safeArea(paddingValues, direction).fillMaxSize()) {
            StateView(summariesViewModel.handler) { summaries ->
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
                LazyColumn {
                    pagingItems(records, key = { it.id }) { index ->
                        records[index]?.let { record ->
                            TaskRecordHistoryItem(
                                record,
                                record.isRetryRequested || record.id in retryRequestedIds,
                                recordsViewModel::markForRetry,
                            )
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
                "Success: ${summary.successCount} | Failure: ${summary.failureCount} | " +
                    "Retry: ${summary.retryRequestedCount}",
            )
        },
    )
    HorizontalDivider()
}

@Composable
private fun TaskRecordOverview(summary: TaskRecordSummary) {
    Text(
        "Success: ${summary.successCount} | Failure: ${summary.failureCount} | " +
            "Retry requested: ${summary.retryRequestedCount}",
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun TaskRecordStatusFilter(selectedStatus: Boolean?, onSelectedStatusChanged: (Boolean?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TaskRecordFilterChip("ALL", selectedStatus == null) { onSelectedStatusChanged(null) }
        TaskRecordFilterChip("SUCCESS", selectedStatus == true) { onSelectedStatusChanged(true) }
        TaskRecordFilterChip("FAILURE", selectedStatus == false) { onSelectedStatusChanged(false) }
    }
}

@Composable
private fun TaskFailureTypeFilter(selectedFailureType: String?, onSelectedFailureTypeChanged: (String?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TaskRecordFilterChip("ALL FAILURES", selectedFailureType == null) {
            onSelectedFailureTypeChanged(null)
        }
        listOf(
            TaskRecordType.MODEL_RESPONSE_FAILURE,
            TaskRecordType.MODEL_EXECUTION_FAILURE,
            TaskRecordType.DATA_ACCESS_FAILURE,
            TaskRecordType.UNKNOWN_FAILURE,
        ).forEach { failureType ->
            TaskRecordFilterChip(failureType, selectedFailureType == failureType) {
                onSelectedFailureTypeChanged(failureType)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRecordFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(modifier = Modifier.padding(end = 8.dp)) {
        FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
    }
}

@Composable
private fun TaskRecordHistoryItem(record: TaskRecordInfo, retryRequested: Boolean, onMarkForRetry: (Long) -> Unit) {
    ListItem(
        headlineContent = { Text("${if (record.isSuccess) "SUCCESS" else "FAILURE"}: ${record.objectId}") },
        supportingContent = {
            Column {
                Text("Record: ${record.id} | Time: ${record.createdTime}")
                record.failureType?.let { Text("Failure type: $it") }
                record.failureReason?.let { reason ->
                    Text(reason, modifier = Modifier.widthIn(max = 720.dp))
                }
            }
        },
        trailingContent = {
            if (!record.isSuccess) {
                val retry = { onMarkForRetry(record.id) }
                Button(
                    enabled = !retryRequested,
                    onClick = retry,
                    modifier =
                    Modifier.appiumSemantics(
                        text = if (retryRequested) "Retry requested" else "Retry",
                        onClick = retry.takeUnless { retryRequested },
                    ),
                ) {
                    Text(if (retryRequested) "Retry requested" else "Retry")
                }
            }
        },
    )
    HorizontalDivider()
}
