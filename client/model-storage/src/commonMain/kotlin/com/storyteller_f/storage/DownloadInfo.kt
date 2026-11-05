/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.storage

import com.storyteller_f.shared.model.FileInfo
import kotlinx.serialization.Serializable

enum class DownloadStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    PAUSED,
    DOWNLOADED,
    PROCESSED,
    DOWNLOAD_FAILED,
    PROCESS_FAILED,
}

@Serializable
data class DownloadInfo(
    val id: Long,
    val fileInfo: FileInfo,
    val status: DownloadStatus,
    val message: String,
    val path: String,
    val progress: Long,
    val total: Long,
    val processedGeneration: Long = 0,
)
