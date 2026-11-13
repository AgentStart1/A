/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.ROOM_NAME_LENGTH
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectStatus
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll

object Rooms : BaseTable() {
    val name = varchar("name", ROOM_NAME_LENGTH).index()
    val icon = customPrimaryKey("icon").nullable()
    val creator = customPrimaryKey("creator").index()
    val communityId = customPrimaryKey("community_id").index().nullable()
    val status = objectStatus("status")
}

fun Room.Companion.wrapRow(row: ResultRow): Room =
    with(Rooms) {
        Room(
            row[id],
            row[createdTime],
            row[Aids.value],
            row[name],
            row[creator],
            row[icon],
            row[communityId],
            row[status],
        )
    }

fun Room.Companion.findRoomById(id: PrimaryKey) =
    Rooms.selectAll().where {
        Rooms.id eq id
    }
