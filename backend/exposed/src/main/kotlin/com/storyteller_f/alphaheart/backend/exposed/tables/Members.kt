/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.memberStatus
import com.storyteller_f.alphaheart.backend.exposed.objectType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.r2dbc.batchInsert

object Members : BaseTable() {
    val uid = customPrimaryKey("uid").index()
    val objectId = customPrimaryKey("object_id").index()
    val objectType = objectType("object_type")
    val status = memberStatus("status")
    val joinedTime = datetime("joined_time").nullable()
    val invitedTime = datetime("invited_time").nullable()

    init {
        uniqueIndex("member-main", objectId, uid)
        index("member-uid", false, uid, objectId)
    }
}

fun Member.Companion.wrapRow(row: ResultRow): Member =
    with(Members) {
        Member(
            row[id],
            row[uid],
            row[objectId],
            row[objectType],
            row[createdTime],
            row[status],
            row[joinedTime],
            row[invitedTime],
        )
    }

suspend fun batchAddMembers(members: List<Member>): List<ResultRow> =
    Members.batchInsert(members) {
        this[Members.joinedTime] = it.joinedTime
        this[Members.createdTime] = it.createdTime
        this[Members.invitedTime] = it.invitedTime
        this[Members.id] = it.id
        this[Members.uid] = it.uid
        this[Members.objectId] = it.objectId
        this[Members.objectType] = it.objectType
        this[Members.status] = it.status
    }
