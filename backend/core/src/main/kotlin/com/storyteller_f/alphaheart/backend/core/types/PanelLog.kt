/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.PanelLogInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class PanelLog(
    val id: PrimaryKey,
    val adminId: PrimaryKey,
    val targetId: PrimaryKey,
    val objectType: ObjectType,
    val action: String,
    val createdTime: LocalDateTime,
) {
    companion object
}

fun PanelLog.toPanelLogInfo(): PanelLogInfo = PanelLogInfo(id, adminId, targetId, objectType, action, createdTime)
