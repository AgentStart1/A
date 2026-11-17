/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.transfer_cancelled
import com.storyteller_f.alphaheart.app.transfer_completed
import com.storyteller_f.alphaheart.app.transfer_download_failed
import com.storyteller_f.alphaheart.app.transfer_downloaded
import com.storyteller_f.alphaheart.app.transfer_downloading
import com.storyteller_f.alphaheart.app.transfer_failed
import com.storyteller_f.alphaheart.app.transfer_not_downloaded
import com.storyteller_f.alphaheart.app.transfer_paused
import com.storyteller_f.alphaheart.app.transfer_pending
import com.storyteller_f.alphaheart.app.transfer_process_failed
import com.storyteller_f.alphaheart.app.transfer_uploading
import com.storyteller_f.alphaheart.app.transfer_waiting
import com.storyteller_f.shared.type.UploadRecordStatus
import com.storyteller_f.storage.DownloadStatus
import com.storyteller_f.storage.UploadStatus
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun uploadStatusLabel(status: UploadStatus): String =
    stringResource(
        when (status) {
            UploadStatus.NOT_UPLOADING -> Res.string.transfer_waiting
            UploadStatus.UPLOADING -> Res.string.transfer_uploading
            UploadStatus.PAUSED -> Res.string.transfer_paused
            UploadStatus.FAILED -> Res.string.transfer_failed
            UploadStatus.SUCCESS -> Res.string.transfer_completed
        },
    )

@Composable
internal fun uploadRecordStatusLabel(status: UploadRecordStatus): String =
    stringResource(
        when (status) {
            UploadRecordStatus.PENDING -> Res.string.transfer_pending
            UploadRecordStatus.SUCCESS -> Res.string.transfer_completed
            UploadRecordStatus.FAILED -> Res.string.transfer_failed
            UploadRecordStatus.ABORTED -> Res.string.transfer_cancelled
        },
    )

@Composable
internal fun downloadStatusLabel(status: DownloadStatus): String =
    stringResource(
        when (status) {
            DownloadStatus.NOT_DOWNLOADED -> Res.string.transfer_not_downloaded
            DownloadStatus.DOWNLOADING -> Res.string.transfer_downloading
            DownloadStatus.PAUSED -> Res.string.transfer_paused
            DownloadStatus.DOWNLOADED -> Res.string.transfer_downloaded
            DownloadStatus.PROCESSED -> Res.string.transfer_completed
            DownloadStatus.DOWNLOAD_FAILED -> Res.string.transfer_download_failed
            DownloadStatus.PROCESS_FAILED -> Res.string.transfer_process_failed
        },
    )
