/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.UserFavoriteInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

class UserFavorite(
    val id: PrimaryKey,
    val uid: PrimaryKey,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
    val createdTime: LocalDateTime,
) {
    fun objectTuple(): ObjectTuple = ObjectTuple(objectId, objectType)

    companion object
}

fun UserFavorite.toUserFavoriteInfo(): UserFavoriteInfo = UserFavoriteInfo(id, uid, objectId, objectType, createdTime)
