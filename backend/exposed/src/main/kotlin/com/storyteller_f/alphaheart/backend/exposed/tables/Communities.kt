/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.COMMUNITY_NAME_LENGTH
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.types.Community
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.memberPolicy
import com.storyteller_f.alphaheart.backend.exposed.objectStatus
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.onFailure
import com.storyteller_f.shared.model.FontSettings
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.*

object Communities : BaseTable() {
    val name = varchar("name", COMMUNITY_NAME_LENGTH).index()
    val icon = customPrimaryKey("icon").nullable()
    val poster = customPrimaryKey("poster").index().nullable()
    val owner = customPrimaryKey("owner").index()
    val fontSettings = text("font_settings").nullable()
    val memberPolicy = memberPolicy("member_policy")
    val status = objectStatus("status")
}

private val json = Json { ignoreUnknownKeys = true }

fun Community.Companion.wrapRow(row: ResultRow): Community =
    with(Communities) {
        val fontSettingsJson = row[fontSettings]
        val fontSettings =
            fontSettingsJson?.let {
                captureFailure(DatabaseError::from) { json.decodeFromString<FontSettings>(it) }
                    .onFailure { Napier.e("Failed to parse font_settings", it.cause) }
                    .getOrNull()
            }
        Community(
            row[id],
            row[createdTime],
            row[Aids.value],
            row[name],
            row[owner],
            row[memberPolicy],
            row[icon],
            row[poster],
            fontSettings,
            row[status],
        )
    }
