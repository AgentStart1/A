/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.file

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.storyteller_f.alphaheart.app.utils.ClientFile
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UploadStatus

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

fun ClientFile.getUploadDataFromClipFile(sha256: String) =
    UploadData(
        size,
        name,
        contentType,
        sha256,
    ) {
        source()
    }

internal fun getUploadProgress(info: UploadInfo?): Float {
    info ?: return 0f
    return (info.progress.toFloat() / if (info.total == 0L) 1 else info.total).coerceIn(
        0f,
        1f,
    )
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
