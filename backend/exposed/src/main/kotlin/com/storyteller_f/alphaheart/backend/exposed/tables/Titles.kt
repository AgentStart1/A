/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.Title
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectStatus
import com.storyteller_f.alphaheart.backend.exposed.objectType
import com.storyteller_f.alphaheart.backend.exposed.titleStatus
import com.storyteller_f.alphaheart.backend.exposed.titleType
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.datetime.datetime

object Titles : BaseTable() {
    val creator = customPrimaryKey("creator").index()
    val receiver = customPrimaryKey("receiver").index()
    val type = titleType("type").index()
    val scopeId = customPrimaryKey("scope_id").index()
    val scopeType = objectType("scope_type")
    val titleStatus = titleStatus("title_status")
    val expiresAt = datetime("expires_at").nullable()
    val status = objectStatus("status")
    val name = varchar("name", 20).index()
    val descriptionTopicId = customPrimaryKey("description_topic_id")

    init {
        index("creator", false, creator, type, scopeId)
        index("receiver", false, receiver, type, scopeId)
        index("creator-status", false, creator, titleStatus, type, scopeId)
        index("receiver-status", false, receiver, titleStatus, type, scopeId)
    }
}

fun Title.Companion.wrapRow(row: ResultRow): Title =
    Title(
        row[Titles.id],
        row[Titles.createdTime],
        row[Titles.name],
        row[Titles.creator],
        row[Titles.receiver],
        row[Titles.type],
        row[Titles.scopeId],
        row[Titles.scopeType],
        row[Titles.titleStatus],
        row[Titles.descriptionTopicId],
        row[Titles.expiresAt],
        row[Titles.status],
    )
