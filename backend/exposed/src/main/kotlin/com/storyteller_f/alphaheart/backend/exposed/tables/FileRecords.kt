/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.FileRecord
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectStatus
import com.storyteller_f.alphaheart.backend.exposed.objectType
import org.jetbrains.exposed.v1.core.*

object FileRecords : BaseTable() {
    val name = varchar("name", 200)
    val fullName = varchar("full_name", 200).index()
    val sha256 = varchar("sha256", 64)
    val duration = long("duration")
    val width = integer("width")
    val height = integer("height")
    val owner = customPrimaryKey("owner")
    val ownerType = objectType("owner_type")
    val contentType = varchar("content_type", 50)
    val size = long("size")
    val status = objectStatus("status")

    init {
        index("files-main", true, owner, name)
        index("files-size", false, owner, size)
        index("files-type", false, owner, contentType, size)
        index("files-full-name", false, fullName)
    }
}

fun FileRecord.Companion.wrapRow(resultRow: ResultRow): FileRecord =
    with(FileRecords) {
        FileRecord(
            resultRow[id],
            resultRow[createdTime],
            resultRow[name],
            resultRow[duration],
            resultRow[width],
            resultRow[height],
            resultRow[owner],
            resultRow[ownerType],
            resultRow[contentType],
            resultRow[size],
            resultRow[fullName],
            resultRow[sha256],
            resultRow[status],
        )
    }
