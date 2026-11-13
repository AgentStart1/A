/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.PrimaryKeyIdentifiable
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey

data class RoomDocument(
    override val id: PrimaryKey,
    val name: String,
    val aid: String,
    val communityId: PrimaryKey? = null,
) : PrimaryKeyIdentifiable {
    val objectType: ObjectType = ObjectType.ROOM

    companion object {
        fun fromRoom(room: Room): RoomDocument = RoomDocument(room.id, room.name, room.aid, room.communityId)
    }
}

sealed interface RoomDocumentSearch {
    data class Keyword(val words: String, val communityId: PrimaryKey? = null, val fetch: OffsetFetch) :
        RoomDocumentSearch
}

interface RoomSearchService {
    suspend fun saveDocument(documents: List<RoomDocument>): CustomResult<Unit, SearchError>

    suspend fun clean(): CustomResult<Unit, SearchError>

    suspend fun searchDocument(
        roomDocumentSearch: RoomDocumentSearch,
    ): CustomResult<PaginationResult<RoomDocument>, SearchError>
}

interface RoomSearchServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun build(env: MergedEnv): RoomSearchService
}
