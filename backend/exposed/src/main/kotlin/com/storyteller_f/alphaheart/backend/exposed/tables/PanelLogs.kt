/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.PanelLog
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.exposed.v1.core.ResultRow

object PanelLogs : BaseTable() {
    val adminId = customPrimaryKey("admin_id").index()
    val targetId = customPrimaryKey("target_id").index()
    val objectType = objectType("object_type").default(ObjectType.USER)
    val action = text("action")

    init {
        index("panel-logs-target", false, targetId, objectType)
    }
}

fun PanelLog.Companion.wrapRow(row: ResultRow): PanelLog =
    with(PanelLogs) {
        PanelLog(
            row[id],
            row[adminId],
            row[targetId],
            row[objectType],
            row[action],
            row[createdTime],
        )
    }
