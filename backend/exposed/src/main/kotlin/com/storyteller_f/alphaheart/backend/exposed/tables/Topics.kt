/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectStatus
import com.storyteller_f.alphaheart.backend.exposed.objectType
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.datetime

object Topics : BaseTable() {
    val author = customPrimaryKey("author").index()
    val parentId = customPrimaryKey("parent_id").index()
    val parentType = objectType("parent_type")
    val rootId = customPrimaryKey("root_id").index()
    val rootType = objectType("root_type")
    val pinned = bool("pinned").default(false)
    val status = objectStatus("status")
    val lastModifiedTime = datetime("last_modified_time").nullable()
    val content = blob("content")
    val isEncrypted = bool("is_encrypted")
    val level = integer("level")

    init {
        index("user_comments", false, author, parentType)
    }
}

fun Topic.Companion.wrapRow(row: ResultRow): Topic =
    with(Topics) {
        Topic(
            row[id],
            row[createdTime],
            row[author],
            row[parentId],
            row[parentType],
            row[rootId],
            row[rootType],
            row[content].bytes,
            row[isEncrypted],
            row[level],
            row[pinned],
            row[lastModifiedTime],
            row.getOrNull(Aids.value),
            row[status],
        )
    }
