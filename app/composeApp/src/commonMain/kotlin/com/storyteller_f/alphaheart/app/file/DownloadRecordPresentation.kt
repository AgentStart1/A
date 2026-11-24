/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.file

import androidx.compose.foundation.layout.size
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.now
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadStatus
import kotlin.math.pow
import kotlin.math.round

class DownloadRecordItemPreviewProvider : PreviewParameterProvider<DownloadInfo?> {
    override val values: Sequence<DownloadInfo?> =
        sequenceOf(
            DownloadInfo(
                id = 1000L,
                fileInfo =
                FileInfo(
                    id = 1L,
                    url = "https://example.com/files/sample1.png",
                    fullName = "images/sample1.png",
                    contentType = "image/png",
                    size = 5_000_000,
                    name = "sample1.png",
                    owner = 1001L,
                    ownerType = ObjectType.USER,
                    lastModified = now(),
                    dimension = null,
                ),
                status = DownloadStatus.DOWNLOADING,
                message = "正在下载…",
                path = "/downloads/sample1.png",
                progress = 2_500_000,
                total = 5_000_000,
            ),
            DownloadInfo(
                id = 2000L,
                fileInfo =
                FileInfo(
                    id = 2L,
                    url = "https://example.com/files/sample2.mp4",
                    fullName = "videos/sample2.mp4",
                    contentType = "video/mp4",
                    size = 120_000_000,
                    name = "sample2.mp4",
                    owner = 1002L,
                    ownerType = ObjectType.USER,
                    lastModified = now(),
                    dimension = null,
                ),
                status = DownloadStatus.DOWNLOADED,
                message = "下载完成",
                path = "/downloads/sample2.mp4",
                progress = 120_000_000,
                total = 120_000_000,
            ),
            DownloadInfo(
                id = 3000L,
                fileInfo =
                FileInfo(
                    id = 3L,
                    url = "https://example.com/files/sample3.pdf",
                    fullName = "docs/sample3.pdf",
                    contentType = FileInfo.PDF_CONTENT_TYPE,
                    size = 2_000_000,
                    name = "sample3.pdf",
                    owner = 1003L,
                    ownerType = ObjectType.USER,
                    lastModified = now(),
                    dimension = null,
                ),
                status = DownloadStatus.DOWNLOAD_FAILED,
                message = "网络错误，稍后重试",
                path = "/downloads/sample3.pdf",
                progress = 500_000,
                total = 2_000_000,
            ),
            DownloadInfo(
                id = 5000L,
                fileInfo =
                FileInfo(
                    id = 5L,
                    url = "https://example.com/files/sample5.jpg",
                    fullName = "images/sample5.jpg",
                    contentType = "image/jpeg",
                    size = 3_500_000,
                    name = "sample5.jpg",
                    owner = 1005L,
                    ownerType = ObjectType.USER,
                    lastModified = now(),
                    dimension = null,
                ),
                status = DownloadStatus.PROCESS_FAILED,
                message = "处理失败：文件损坏",
                path = "/downloads/sample5.jpg",
                progress = 3_500_000,
                total = 3_500_000,
            ),
        )
}

internal fun getDownloadProgress(d: DownloadInfo): Float =
    (d.progress.toFloat() / if (d.total == 0L) 1 else d.total).coerceIn(
        0f,
        1f,
    )

fun DownloadInfo.getPercent(): String = "${(getDownloadProgress(this) * 100).roundToDecimalPlaces(2)} %"

fun Float.roundToDecimalPlaces(decimals: Int): Float {
    val multiplier = 10.0f.pow(decimals)
    return round(this * multiplier) / multiplier
}
