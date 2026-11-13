/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.UserTopicRead
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.datetime

object UserTopicReads : Table() {
    val uid = customPrimaryKey("uid")
    val updatedAt = datetime("updated_at")
    val objectId = customPrimaryKey("object_id")
    val objectType = objectType("object_type")
    val topicId = customPrimaryKey("topic_id")
    override val primaryKey = PrimaryKey(uid, objectId)

    init {
        index("user-topic-reads-main", true, uid, objectId)
    }
}

fun UserTopicRead.Companion.wrapRow(resultRow: ResultRow): UserTopicRead =
    with(UserTopicReads) {
        UserTopicRead(
            resultRow[uid],
            resultRow[updatedAt],
            resultRow[objectId],
            resultRow[objectType],
            resultRow[topicId],
        )
    }
