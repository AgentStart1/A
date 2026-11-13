/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class ReactionRecord(
    val uid: PrimaryKey,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
    val emoji: String,
    val id: PrimaryKey,
    val createdTime: LocalDateTime,
) {
    companion object
}
