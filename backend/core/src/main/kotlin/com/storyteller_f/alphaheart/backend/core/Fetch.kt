/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.storyteller_f.shared.obj.ReactionCursorKey
import com.storyteller_f.shared.type.PrimaryKey

sealed interface ObjectFetch {
    data class AidFetch(val aid: String) : ObjectFetch

    data class IdFetch(val id: PrimaryKey) : ObjectFetch
}

sealed interface ObjectListFetch {
    data class AidListFetch(val aidList: List<String>) : ObjectListFetch
    data class IdListFetch(val idList: List<PrimaryKey>) : ObjectListFetch
}

fun idListFetch(idList: List<PrimaryKey>) = ObjectListFetch.IdListFetch(idList)
fun aidListFetch(aidList: List<String>) = ObjectListFetch.AidListFetch(aidList)

fun idFetch(id: PrimaryKey) = ObjectFetch.IdFetch(id)
fun aidFetch(aid: String) = ObjectFetch.AidFetch(aid)

sealed interface Cursor<T> {
    val value: T

    data class AscCursor<T>(override val value: T) : Cursor<T>

    data class DescCursor<T>(override val value: T) : Cursor<T>
}

interface Fetch {
    val size: Int
}

interface GenericFetch<T> : Fetch {
    val cursor: Cursor<T>?
}

data class PrimaryKeyFetch(override val cursor: Cursor<PrimaryKey>?, override val size: Int) :
    GenericFetch<PrimaryKey>

data class ReactionFetch(override val cursor: Cursor<ReactionCursorKey>?, override val size: Int) :
    GenericFetch<ReactionCursorKey>

data class OffsetFetch(override val cursor: Cursor<Int>?, override val size: Int) : GenericFetch<Int>

/** Returns [infos] in the order specified by [ids], omitting identifiers without a matching item. */
fun <T> fixedSort(infos: List<T>, ids: List<PrimaryKey>, key: (T) -> PrimaryKey): List<T> {
    val groupBy = infos.associateBy { key(it) }
    val processedTopics =
        ids.mapNotNull {
            groupBy[it]
        }
    return processedTopics
}
