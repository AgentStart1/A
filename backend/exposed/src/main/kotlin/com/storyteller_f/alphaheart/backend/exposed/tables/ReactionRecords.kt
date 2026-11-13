/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.Reaction
import com.storyteller_f.alphaheart.backend.core.types.ReactionRecord
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.emoji
import com.storyteller_f.alphaheart.backend.exposed.objectType
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.exposed.v1.core.*

object ReactionRecords : BaseTable() {
    val uid = customPrimaryKey("uid")
    val objectId = customPrimaryKey("object_id").index()
    val objectType = objectType("object_type")
    val emoji = emoji()

    init {
        index("reactions-main", true, objectId, emoji, uid)
    }
}

fun ReactionRecord.Companion.wrapRow(resultRow: ResultRow): ReactionRecord =
    with(ReactionRecords) {
        ReactionRecord(
            resultRow[uid],
            resultRow[objectId],
            resultRow[objectType],
            resultRow[emoji],
            resultRow[id],
            resultRow[createdTime],
        )
    }
object Reactions : Table() {
    val objectId = customPrimaryKey("object_id")
    val objectType = objectType("object_type")
    val emoji = emoji()
    val count = long("count")
    val lastReactionId = customPrimaryKey("last_reaction_id")
    override val primaryKey = PrimaryKey(emoji)

    init {
        index("reaction-stats-main", false, objectId, count, lastReactionId)
        index("reaction-stats-unique", true, objectId, emoji)
    }
}

fun Reaction.Companion.wrapRow(resultRow: ResultRow): Reaction =
    with(Reactions) {
        Reaction(
            resultRow[objectId],
            resultRow[objectType],
            resultRow[emoji],
            resultRow[count],
            resultRow[lastReactionId],
        )
    }
