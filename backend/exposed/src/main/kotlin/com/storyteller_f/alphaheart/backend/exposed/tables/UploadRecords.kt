/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.UploadRecord
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import com.storyteller_f.alphaheart.backend.exposed.uploadRecordStatus
import org.jetbrains.exposed.v1.core.ResultRow

object UploadRecords : BaseTable() {
    val objectId = customPrimaryKey("object_id")
    val objectType = objectType("object_type")
    val status = uploadRecordStatus("status")
    val total = long("total")
    val progress = long("progress")
    val name = varchar("name", 100)
    val chunkSize = long("chunk_size")
    val sha256 = varchar("sha256", 64)
    val crc32c = varchar("crc32c", 8)
}

fun UploadRecord.Companion.wrapRow(resultRow: ResultRow): UploadRecord =
    with(UploadRecords) {
        UploadRecord(
            resultRow[id],
            resultRow[createdTime],
            resultRow[objectId],
            resultRow[objectType],
            resultRow[status],
            resultRow[total],
            resultRow[progress],
            resultRow[name],
            resultRow[chunkSize],
            resultRow[sha256],
            resultRow[crc32c],
        )
    }
