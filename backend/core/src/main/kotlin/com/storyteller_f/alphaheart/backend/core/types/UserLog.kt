/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.UserLogInfo
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class UserLog(
    val id: PrimaryKey,
    val createdTime: LocalDateTime,
    val uid: PrimaryKey,
    val type: UserLogType,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
) {
    companion object
}

fun UserLog.toUserLogInfo() =
    UserLogInfo(
        id = id,
        uid = uid,
        type = type,
        objectId = objectId,
        objectType = objectType,
        createdTime = createdTime.date,
    )
