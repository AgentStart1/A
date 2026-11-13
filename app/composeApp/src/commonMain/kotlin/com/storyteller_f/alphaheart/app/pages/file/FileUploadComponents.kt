/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalClientFileProvider
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.common.UploadDetailViewModel
import com.storyteller_f.alphaheart.app.common.getUploadViewModel
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
import com.storyteller_f.storage.UploadCollection
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UploadStatus
import nl.jacobras.humanreadable.HumanReadable

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
    Row(
        modifier =
        Modifier
            .combinedClickable(
                onClick = { showSheet = true },
                onLongClick = { showDropdown = true },
            )
            .appiumSemantics(
                text = "${uploadInfo.name}, ${uploadInfo.status.name}",
                onClick = { showSheet = true },
                onLongClick = { showDropdown = true },
            )
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        UploadIcon(contentType = uploadInfo.contentType)

        Column(modifier = Modifier.weight(1f)) {
            Text(uploadInfo.name)
            Row {
                Text(HumanReadable.fileSize(uploadInfo.total))
                Spacer(modifier = Modifier.weight(1f))
                Text(uploadInfo.status.name)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { uploadInfo.progress.toFloat() / uploadInfo.total },
                    modifier =
                    Modifier.weight(1f).appiumSemantics(
                        description = "upload progress ${uploadInfo.name}: ${uploadInfo.progress}/${uploadInfo.total}",
                    ),
                )
                Text(uploadInfo.getPercent())
            }
        }

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

    UploadInfoPage(showSheet, sheetState, uploadInfo.pathHash) {
        showSheet = false
    }
}

@Composable
fun CancelUploadButton(uploadInfo: UploadInfo, updateDropdown: (Boolean) -> Unit) {
    val fileProvider = LocalClientFileProvider.current
    val globalDialogController = LocalGlobalDialog.current
    val current = LocalUiViewModel.current
    val state by current.instance.collectAsState()
    val modelStorage = state.database
    val myUid = state.sessionManager.model.uid ?: return
    val abort: () -> Unit = {
        updateDropdown(false)
        globalDialogController.launch {
            useResult {
                uploadInfo.recordId?.let {
                    request {
                        abortChunkUpload(it)
                    }.getOrThrow()
                }
                modelStorage.upload.delete(UploadCollection(myUid), uploadInfo.pathHash)
                fileProvider.getUploader()?.forget(uploadInfo.pathHash)
                CustomResult.success(Unit)
            }
        }
    }
    DropdownMenuItem(
        text = { Text("Abort Upload") },
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
        Icon(Icons.Default.Done, "done")
        return
    }
    val (label, icon) =
        when (file.status) {
            UploadStatus.UPLOADING -> "pause" to Icons.Default.Pause
            UploadStatus.FAILED -> "retry" to Icons.Default.Refresh
            else -> "resume" to Icons.Default.PlayCircle
        }
    val action: () -> Unit = {
        globalDialogController.launch {
            useResult {
                if (file.status == UploadStatus.UPLOADING) {
                    provider.getUploader()?.pause(file.pathHash)
                } else {
                    provider.getUploader()?.resume(file.pathHash)
                }
                CustomResult.success(Unit)
            }
        }
    }
    IconButton(action, Modifier.appiumSemantics(description = "$label upload ${file.name}", onClick = action)) {
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
fun UploadInfoPage(showSheet: Boolean, sheetState: SheetState, pathHash: String, hideSheet: () -> Unit) {
    if (LocalInspectionMode.current) return
    val my = LocalUserInfo.current ?: return
    val uploadViewModel = getUploadViewModel(pathHash, my.id)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LinearProgressIndicator(progress = {
            getUploadProgress(data)
        })
        Text(data.getPercent())
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
        UploadStatusButton(data)
    }
}

@Composable
private fun UploadInfoTable(uploadInfo: UploadInfo?) {
    val tableData =
        remember(uploadInfo) {
            buildList {
                uploadInfo?.path?.let { add("Path" to it) }
                uploadInfo?.pathHash?.let { add("Hash" to it) }
                uploadInfo?.total?.let { add("Size" to HumanReadable.fileSize(it)) }
                uploadInfo?.status?.name?.let { add("Status" to it) }
                uploadInfo?.message?.let { add("Message" to it) }
                uploadInfo?.name?.let { add("Name" to it) }
                uploadInfo?.contentType?.let { add("ContentType" to it) }
            }
        }
    InfoTable(tableData)
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
