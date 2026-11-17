/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.backend.core.types.UploadRecord
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UploadRecordStatus
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class UploadQuotaSnapshotTest {
    @Test
    fun receivedChunksIncludeTheShortFinalChunkOnlyOnce() {
        val record = uploadRecord()
        val snapshot =
            buildUploadQuotaSnapshot(
                record,
                listOf(
                    "chunks/120/chunk_0",
                    "chunks/120/chunk_1",
                    "chunks/120/chunk_6",
                    "chunks/120/chunk_0",
                    "chunks/121/chunk_2",
                    "chunks/120/chunk_-1",
                    "chunks/120/chunk_7",
                    "chunks/120/chunk_invalid",
                ),
            )
        assertEquals(24L * 1024 * 1024, snapshot.progress)
        assertEquals(record.id, snapshot.id)
        assertEquals(record.sha256, snapshot.sha256)
        assertEquals(record.crc32c, snapshot.crc32c)
        assertEquals(0L, record.progress)
    }

    @Test
    fun pendingUploadWithoutReceivedChunksHasZeroProgress() {
        assertEquals(0L, buildUploadQuotaSnapshot(uploadRecord(), emptyList()).progress)
    }

    @Test
    fun completedUploadRetainsFullSizeAfterTemporaryChunksAreRemoved() {
        val record = uploadRecord().copy(status = UploadRecordStatus.SUCCESS)
        assertEquals(record.total, buildUploadQuotaSnapshot(record, emptyList()).progress)
    }
}

private fun uploadRecord() =
    UploadRecord(
        id = 120L,
        createdTime = LocalDateTime(2026, 11, 17, 0, 0),
        objectId = 1L,
        objectType = ObjectType.USER,
        status = UploadRecordStatus.PENDING,
        total = 64L * 1024 * 1024,
        progress = 0L,
        name = "synthetic.bin",
        chunkSize = 10L * 1024 * 1024,
        sha256 = "281e519df3077b557c6b03f5da83c4e8d397219259615dd7c3308f89cae8f2a6",
        crc32c = "hkV4iw==",
    )
