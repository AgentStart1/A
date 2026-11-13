/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.MEDIA_NAME_LENGTH
import com.storyteller_f.alphaheart.backend.core.types.FileRef
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import org.jetbrains.exposed.v1.core.ResultRow

object FileRefs : BaseTable() {
    val objectId = customPrimaryKey("object_id")
    val objectType = objectType("object_type")
    val author = customPrimaryKey("owner")
    val mediaName = varchar("media_name", MEDIA_NAME_LENGTH)
    val fileId = customPrimaryKey("file_id")

    init {
        index(true, fileId, objectId)
    }
}

fun FileRef.Companion.wrapRow(resultRow: ResultRow): FileRef =
    with(FileRefs) {
        FileRef(
            resultRow[id],
            resultRow[createdTime],
            resultRow[objectId],
            resultRow[objectType],
            resultRow[author],
            resultRow[mediaName],
            resultRow[fileId],
        )
    }
