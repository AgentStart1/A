/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.UserFavorite
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import org.jetbrains.exposed.v1.core.ResultRow

object UserFavorites : BaseTable() {
    val uid = customPrimaryKey("uid")
    val objectId = customPrimaryKey("object_id")
    val objectType = objectType("object_type")

    init {
        uniqueIndex("favorite-main", uid, objectId)
    }
}

fun UserFavorite.Companion.wrapRow(resultRow: ResultRow): UserFavorite =
    UserFavorite(
        resultRow[UserFavorites.id],
        resultRow[UserFavorites.uid],
        resultRow[UserFavorites.objectId],
        resultRow[UserFavorites.objectType],
        resultRow[UserFavorites.createdTime],
    )
