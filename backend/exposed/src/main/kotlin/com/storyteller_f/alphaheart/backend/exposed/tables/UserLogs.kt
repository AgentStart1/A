/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.UserLog
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import com.storyteller_f.shared.model.UserLogType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table

fun Table.userLogType(name: String) = enumerationByName<UserLogType>(name, 30)

object UserLogs : BaseTable() {
    val uid = customPrimaryKey("uid")
    val type = userLogType("type")
    val objectId = customPrimaryKey("object_id")
    val objectType = objectType("object_type")

    init {
        index("user-logs-main", false, uid, type, objectId)
    }
}

fun UserLog.Companion.wrapRow(resultRow: ResultRow): UserLog =
    with(UserLogs) {
        UserLog(
            resultRow[id],
            resultRow[createdTime],
            resultRow[uid],
            resultRow[type],
            resultRow[objectId],
            resultRow[objectType],
        )
    }
