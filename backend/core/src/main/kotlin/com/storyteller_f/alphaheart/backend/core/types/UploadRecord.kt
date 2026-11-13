/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.UploadRecordInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UploadRecordStatus
import kotlinx.datetime.LocalDateTime

data class UploadRecord(
    val id: PrimaryKey,
    val createdTime: LocalDateTime,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
    val status: UploadRecordStatus,
    val total: Long,
    val progress: Long,
    val name: String,
    val chunkSize: Long,
    val sha256: String,
    val crc32c: String,
) {
    init {
        com.storyteller_f.shared.model.FileChecksums(sha256, crc32c)
    }

    companion object
}

fun UploadRecord.toUploadRecordInfo(): UploadRecordInfo =
    UploadRecordInfo(id, createdTime, objectId, objectType, status, total, progress, name, chunkSize, sha256, crc32c)
