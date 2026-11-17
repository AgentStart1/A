/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.types.UploadRecord
import com.storyteller_f.alphaheart.backend.core.types.toUploadRecordInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.ContainerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.model.A_FILE_DEFAULT_BUCKET
import com.storyteller_f.shared.model.UploadRecordInfo
import com.storyteller_f.shared.type.UploadRecordStatus

internal suspend fun Backend.loadUploadQuotaSnapshot(
    record: UploadRecord?,
): CustomResult<UploadRecordInfo?, ContainerError> =
    when {
        record == null -> CustomResult.success(null)

        record.status != UploadRecordStatus.PENDING ->
            CustomResult.success(buildUploadQuotaSnapshot(record, emptyList()))

        else ->
            objectStorageService.list(A_FILE_DEFAULT_BUCKET, "chunks/${record.id}/")
                .map { chunks -> buildUploadQuotaSnapshot(record, chunks.map { it.fullName }) }
                .mapError(ContainerError::fromError)
    }

internal fun buildUploadQuotaSnapshot(record: UploadRecord, chunkNames: List<String>): UploadRecordInfo {
    val received =
        when (record.status) {
            UploadRecordStatus.SUCCESS -> record.total
            UploadRecordStatus.PENDING -> receivedChunkBytes(record, chunkNames)
            else -> record.progress
        }
    return record.toUploadRecordInfo().copy(progress = received)
}

private fun receivedChunkBytes(record: UploadRecord, chunkNames: List<String>): Long {
    require(record.total >= 0 && record.chunkSize > 0) { "Invalid upload size" }
    if (record.total == 0L) return 0L
    val lastIndex = (record.total - 1) / record.chunkSize
    val prefix = "chunks/${record.id}/chunk_"
    return chunkNames.asSequence()
        .filter { it.startsWith(prefix) }
        .mapNotNull { it.removePrefix(prefix).toLongOrNull() }
        .filter { it in 0L..lastIndex }
        .distinct()
        .sumOf { index -> minOf(record.chunkSize, record.total - index * record.chunkSize) }
}
