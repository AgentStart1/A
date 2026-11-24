/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalClientFileProvider
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.abort_upload
import com.storyteller_f.alphaheart.app.common.UploadDetailViewModel
import com.storyteller_f.alphaheart.app.common.getUploadViewModel
import com.storyteller_f.alphaheart.app.components.ReadingSheet
import com.storyteller_f.alphaheart.app.file.ProgressPreviewProvider
import com.storyteller_f.alphaheart.app.file.UploadItemPreviewProvider
import com.storyteller_f.alphaheart.app.file.getPercent
import com.storyteller_f.alphaheart.app.file.getUploadProgress
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
import com.storyteller_f.alphaheart.app.transfer_upload_details
import com.storyteller_f.alphaheart.app.transfer_upload_progress
import com.storyteller_f.alphaheart.app.ui.theme.AppTheme
import com.storyteller_f.alphaheart.app.upload_actions
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.IconActionMenu
import com.storyteller_f.alphaheart.client.compose_core.components.InfoTable
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAdaptiveOverlay
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
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

@Preview
@Composable
fun UploadItem(@PreviewParameter(UploadItemPreviewProvider::class) uploadInfo: UploadInfo?) {
    uploadInfo ?: return
    var showSheet by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    val statusLabel = uploadStatusLabel(uploadInfo.status)
    val openActions = uploadInfo.recordId?.let { { showActions = true } }
    UploadItemRow(uploadInfo, statusLabel, { showSheet = true }, openActions) {
        UploadStatusButton(uploadInfo)
    }

    if (showActions && uploadInfo.recordId != null) {
        val dismiss = { showActions = false }
        ReadingAdaptiveOverlay(dismiss, ReadingOverlaySize.COMPACT) {
            ReadingActionMenuSurface(
                onDismiss = dismiss,
                heading = {
                    Text(stringResource(Res.string.upload_actions), style = MaterialTheme.typography.titleMedium)
                },
            ) {
                IconActionMenu { CancelUploadButton(uploadInfo, dismiss) }
            }
        }
    }

    UploadInfoPage(showSheet, uploadInfo.pathHash, uploadInfo.owner) {
        showSheet = false
    }
}

@Composable
internal fun UploadItemRow(
    uploadInfo: UploadInfo,
    statusLabel: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    statusAction: @Composable () -> Unit,
) {
    val rowDescription =
        listOf(
            uploadInfo.name,
            uploadInfo.contentType,
            HumanReadable.fileSize(uploadInfo.total),
            statusLabel,
        ).joinToString(", ")
    val progressDescription =
        stringResource(Res.string.transfer_upload_progress, uploadInfo.name, uploadInfo.progress, uploadInfo.total)
    TransferRecordContent(
        name = uploadInfo.name,
        status = statusLabel,
        progress = { getUploadProgress(uploadInfo) },
        percent = uploadInfo.getPercent(),
        modifier =
        Modifier.fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = rowDescription }
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .appiumSemantics(
                description = rowDescription,
                text = rowDescription,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(vertical = 20.dp),
        progressModifier = Modifier.appiumSemantics(description = progressDescription),
        statusAction = statusAction,
    )
}

@Composable
internal fun TransferRecordContent(
    name: String,
    status: String,
    progress: () -> Float,
    percent: String,
    modifier: Modifier,
    progressModifier: Modifier = Modifier,
    statusAction: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Text(
                    status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { statusAction() }
        }
        TransferProgress(progress, percent, progressModifier)
    }
}

@Composable
private fun CancelUploadButton(uploadInfo: UploadInfo, onDismiss: () -> Unit) {
    val fileProvider = LocalClientFileProvider.current
    val globalDialogController = LocalGlobalDialog.current
    val current = LocalUiViewModel.current
    val state by current.instance.collectAsState()
    val modelStorage = state.database
    val abort: () -> Unit = {
        onDismiss()
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
    ButtonNav(actionIcon(ActionGlyph.SQUARE), stringResource(Res.string.abort_upload), onClick = abort)
}

@Composable
private fun UploadStatusButton(file: UploadInfo?) {
    file ?: return
    val provider = LocalClientFileProvider.current
    val globalDialogController = LocalGlobalDialog.current
    if (file.status == UploadStatus.SUCCESS) {
        Icon(actionIcon(ActionGlyph.CHECK), uploadStatusLabel(file.status))
        return
    }
    val (label, icon) =
        when (file.status) {
            UploadStatus.UPLOADING -> stringResource(Res.string.transfer_pause) to actionIcon(ActionGlyph.PAUSE)
            UploadStatus.FAILED -> stringResource(Res.string.transfer_retry) to actionIcon(ActionGlyph.REFRESH_CW)
            else -> stringResource(Res.string.transfer_resume) to actionIcon(ActionGlyph.PLAY)
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

@Composable
fun UploadInfoPage(showSheet: Boolean, pathHash: String, owner: ObjectTuple, hideSheet: () -> Unit) {
    if (!showSheet || LocalInspectionMode.current) return
    val uploadViewModel = getUploadViewModel(pathHash, owner)
    ReadingSheet(stringResource(Res.string.transfer_upload_details), hideSheet) {
        UploadInfoPageInternal(uploadViewModel)
    }
}

@Composable
private fun UploadInfoPageInternal(uploadViewModel: UploadDetailViewModel) {
    val data by uploadViewModel.data.collectAsState(null)
    UploadInfoTitle(data)
    TransferProgress({ getUploadProgress(data) }, data.getPercent())
    UploadInfoTable(data)
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
    InfoTable(tableData, singleColumn = true)
}

@Composable
internal fun TransferProgress(progress: () -> Float, percent: String, modifier: Modifier = Modifier) {
    LinearProgressIndicator(progress = progress, modifier = modifier.fillMaxWidth(), drawStopIndicator = {})
    Text(percent, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview
@Composable
fun FixedProgress(@PreviewParameter(ProgressPreviewProvider::class) percent: String) {
    AppTheme {
        Text(percent, modifier = Modifier.width(60.dp))
    }
}
