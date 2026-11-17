/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalClientFileProvider
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.abort_upload
import com.storyteller_f.alphaheart.app.common.UploadDetailViewModel
import com.storyteller_f.alphaheart.app.common.getUploadViewModel
import com.storyteller_f.alphaheart.app.transfer_field_content_type
import com.storyteller_f.alphaheart.app.transfer_field_hash
import com.storyteller_f.alphaheart.app.transfer_field_message
import com.storyteller_f.alphaheart.app.transfer_field_name
import com.storyteller_f.alphaheart.app.transfer_field_path
import com.storyteller_f.alphaheart.app.transfer_field_size
import com.storyteller_f.alphaheart.app.transfer_field_status
import com.storyteller_f.alphaheart.app.transfer_pause
import com.storyteller_f.alphaheart.app.transfer_resume
import com.storyteller_f.alphaheart.app.transfer_retry
import com.storyteller_f.alphaheart.app.transfer_upload_action
import com.storyteller_f.alphaheart.app.transfer_upload_progress
import com.storyteller_f.alphaheart.app.ui.theme.AppTheme
import com.storyteller_f.alphaheart.app.utils.ClientFile
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.InfoTable
import com.storyteller_f.alphaheart.client.compose_core.components.SheetContainer
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.abortChunkUpload
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.storage.UploadCollection
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UploadStatus
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

class UploadItemPreviewProvider : PreviewParameterProvider<UploadInfo> {
    override val values: Sequence<UploadInfo>
        get() =
            sequenceOf(
                UploadInfo.EMPTY.copy(
                    id = 1,
                    pathHash = "test",
                    path = "text/plain",
                    progress = 100,
                    status = UploadStatus.SUCCESS,
                    message = "message",
                    name = "name",
                    contentType = "image/png",
                ),
                UploadInfo.EMPTY.copy(
                    id = 2,
                    pathHash = "test",
                    path = "text/plain",
                    progress = 100,
                    chunkProgress = 50,
                    status = UploadStatus.FAILED,
                    message = "message",
                    name = "name",
                    contentType = "image/png",
                ),
                UploadInfo.EMPTY.copy(
                    id = 3,
                    pathHash = "test",
                    path = "text/plain",
                    progress = 100,
                    chunkProgress = 100,
                    status = UploadStatus.NOT_UPLOADING,
                    message = "message",
                    name = "name",
                    contentType = "image/png",
                ),
                UploadInfo.EMPTY.copy(
                    id = 4,
                    pathHash = "test",
                    path = "text/plain",
                    progress = 100,
                    status = UploadStatus.PAUSED,
                    message = "message",
                    name = "name",
                    contentType = "image/png",
                ),
                UploadInfo.EMPTY.copy(
                    id = 5,
                    pathHash = "test",
                    path = "text/plain",
                    progress = 100,
                    status = UploadStatus.UPLOADING,
                    message = "message",
                    name = "name",
                    contentType = "image/png",
                ),
            )
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UploadItem(@PreviewParameter(UploadItemPreviewProvider::class) uploadInfo: UploadInfo?) {
    uploadInfo ?: return
    var showSheet by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val statusLabel = uploadStatusLabel(uploadInfo.status)
    UploadItemRow(uploadInfo, statusLabel, { showSheet = true }, { showDropdown = true }) {
        UploadStatusButton(uploadInfo)
    }

    DropdownMenu(
        expanded = showDropdown,
        onDismissRequest = { showDropdown = false },
    ) {
        if (uploadInfo.recordId != null) {
            CancelUploadButton(uploadInfo) { showDropdown = false }
        }
    }

    UploadInfoPage(showSheet, sheetState, uploadInfo.pathHash, uploadInfo.owner) {
        showSheet = false
    }
}

@Composable
internal fun UploadItemRow(
    uploadInfo: UploadInfo,
    statusLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    statusAction: @Composable () -> Unit,
) {
    val rowDescription =
        listOf(
            uploadInfo.name,
            uploadInfo.contentType,
            HumanReadable.fileSize(uploadInfo.total),
            statusLabel,
        ).joinToString(", ")
    Row(
        modifier =
        Modifier
            .semantics(mergeDescendants = true) { contentDescription = rowDescription }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .appiumSemantics(
                description = rowDescription,
                text = rowDescription,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        UploadItemContent(uploadInfo, statusLabel, statusAction)
    }
}

@Composable
private fun RowScope.UploadItemContent(
    uploadInfo: UploadInfo,
    statusLabel: String,
    statusAction: @Composable () -> Unit,
) {
    val progressDescription =
        stringResource(Res.string.transfer_upload_progress, uploadInfo.name, uploadInfo.progress, uploadInfo.total)
    UploadIcon(contentType = uploadInfo.contentType)

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(uploadInfo.name)
        Row {
            Text(HumanReadable.fileSize(uploadInfo.total))
            Spacer(modifier = Modifier.weight(1f))
            Text(statusLabel, maxLines = 1)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LinearProgressIndicator(
                progress = { getUploadProgress(uploadInfo) },
                modifier =
                Modifier.weight(1f).appiumSemantics(
                    description = progressDescription,
                ),
            )
            TransferProgressLabel(uploadInfo.getPercent())
        }
    }

    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { statusAction() }
}

@Composable
fun CancelUploadButton(uploadInfo: UploadInfo, updateDropdown: (Boolean) -> Unit) {
    val fileProvider = LocalClientFileProvider.current
    val globalDialogController = LocalGlobalDialog.current
    val current = LocalUiViewModel.current
    val state by current.instance.collectAsState()
    val modelStorage = state.database
    val abort: () -> Unit = {
        updateDropdown(false)
        globalDialogController.launch {
            globalDialogController.useResult {
                uploadInfo.recordId?.let {
                    request {
                        abortChunkUpload(it)
                    }.getOrThrow()
                }
                modelStorage.upload.delete(UploadCollection.fromInfo(uploadInfo), uploadInfo.pathHash)
                fileProvider.getUploader()?.forget(uploadInfo.pathHash, uploadInfo.owner)
                CustomResult.success(Unit)
            }
        }
    }
    DropdownMenuItem(
        text = { Text(stringResource(Res.string.abort_upload)) },
        onClick = abort,
        modifier = Modifier.appiumSemantics(text = "Abort Upload", onClick = abort),
        leadingIcon = {
            Icon(Icons.Default.Stop, contentDescription = CoreStrings.cancel())
        },
    )
}

@Composable
private fun UploadStatusButton(file: UploadInfo?) {
    file ?: return
    val provider = LocalClientFileProvider.current
    val globalDialogController = LocalGlobalDialog.current
    if (file.status == UploadStatus.SUCCESS) {
        Icon(Icons.Default.Done, uploadStatusLabel(file.status))
        return
    }
    val (label, icon) =
        when (file.status) {
            UploadStatus.UPLOADING -> stringResource(Res.string.transfer_pause) to Icons.Default.Pause
            UploadStatus.FAILED -> stringResource(Res.string.transfer_retry) to Icons.Default.Refresh
            else -> stringResource(Res.string.transfer_resume) to Icons.Default.PlayCircle
        }
    val action: () -> Unit = {
        globalDialogController.launch {
            useResult {
                if (file.status == UploadStatus.UPLOADING) {
                    provider.getUploader()?.pause(file.pathHash, file.owner)
                } else {
                    provider.getUploader()?.resume(file.pathHash, file.owner)
                }
                CustomResult.success(Unit)
            }
        }
    }
    val actionDescription = stringResource(Res.string.transfer_upload_action, label, file.name)
    IconButton(action, Modifier.appiumSemantics(description = actionDescription, onClick = action)) {
        Icon(icon, label)
    }
}

fun ClientFile.getUploadDataFromClipFile(sha256: String) =
    UploadData(
        size,
        name,
        contentType,
        sha256,
    ) {
        source()
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadInfoPage(
    showSheet: Boolean,
    sheetState: SheetState,
    pathHash: String,
    owner: ObjectTuple,
    hideSheet: () -> Unit,
) {
    if (LocalInspectionMode.current) return
    val uploadViewModel = getUploadViewModel(pathHash, owner)
    BaseSheet(showSheet, sheetState, hideSheet) {
        SheetContainer {
            Column(
                modifier = Modifier.heightIn(200.dp, 600.dp).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                UploadInfoPageInternal(uploadViewModel)
            }
        }
    }
}

@Composable
private fun UploadInfoPageInternal(uploadViewModel: UploadDetailViewModel) {
    val data by uploadViewModel.data.collectAsState(null)
    UploadInfoTitle(data)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LinearProgressIndicator(progress = {
            getUploadProgress(data)
        }, modifier = Modifier.weight(1f))
        TransferProgressLabel(data.getPercent())
    }
    UploadInfoTable(data)
}

private fun getUploadProgress(info: UploadInfo?): Float {
    info ?: return 0f
    return (info.progress.toFloat() / if (info.total == 0L) 1 else info.total).coerceIn(
        0f,
        1f,
    )
}

@Composable
private fun UploadInfoTitle(data: UploadInfo?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(data?.name ?: "-", modifier = Modifier.weight(1f))
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            UploadStatusButton(data)
        }
    }
}

@Composable
private fun UploadInfoTable(uploadInfo: UploadInfo?) {
    val status = uploadInfo?.let { uploadStatusLabel(it.status) }.orEmpty()
    val tableData =
        if (uploadInfo == null) {
            emptyList()
        } else {
            listOf(
                stringResource(Res.string.transfer_field_path) to uploadInfo.path,
                stringResource(Res.string.transfer_field_hash) to uploadInfo.pathHash,
                stringResource(Res.string.transfer_field_size) to HumanReadable.fileSize(uploadInfo.total),
                stringResource(Res.string.transfer_field_status) to status,
                stringResource(Res.string.transfer_field_message) to uploadInfo.message,
                stringResource(Res.string.transfer_field_name) to uploadInfo.name,
                stringResource(Res.string.transfer_field_content_type) to uploadInfo.contentType,
            )
        }
    InfoTable(tableData)
}

@Composable
internal fun TransferProgressLabel(percent: String) {
    Text(percent, Modifier.width(72.dp), maxLines = 1, textAlign = TextAlign.End)
}

fun UploadInfo?.getPercent(): String {
    this ?: return "-"
    return "${
        (progress.toFloat() * 100 / if (total == 0L) 1 else total).roundToDecimalPlaces(
            2,
        )
    } %"
}

class ProgressPreviewProvider : PreviewParameterProvider<String> {
    override val values: Sequence<String>
        get() = sequenceOf("0 %", "50 %", "89.99 %", "100.0 %")
}

@Preview
@Composable
fun FixedProgress(@PreviewParameter(ProgressPreviewProvider::class) percent: String) {
    AppTheme {
        Text(percent, modifier = Modifier.width(60.dp))
    }
}
