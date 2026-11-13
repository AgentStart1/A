/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.PrimaryKeyIdentifiable
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey

data class MemberDocument(
    override val id: PrimaryKey,
    val uid: PrimaryKey,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
    val nickname: String,
    val objectName: String,
    val communityId: PrimaryKey? = null,
) : PrimaryKeyIdentifiable {
    companion object {
        fun fromUserInfo(
            id: PrimaryKey,
            userInfo: UserInfo,
            objectId: PrimaryKey,
            objectType: ObjectType,
            objectName: String,
            communityId: PrimaryKey? = null,
        ): MemberDocument =
            MemberDocument(
                id = id,
                uid = userInfo.id,
                objectId = objectId,
                objectType = objectType,
                nickname = userInfo.nickname,
                objectName = objectName,
                communityId = communityId,
            )
    }
}

sealed interface MemberDocumentSearch {
    data class Keyword(val objectId: PrimaryKey? = null, val nickname: String, val fetch: OffsetFetch) :
        MemberDocumentSearch

    data class CommunityMembers(val uid: PrimaryKey, val objectName: String, val fetch: OffsetFetch) :
        MemberDocumentSearch

    data class RoomMembers(
        val uid: PrimaryKey,
        val objectName: String,
        val communityId: PrimaryKey? = null,
        val fetch: OffsetFetch,
    ) : MemberDocumentSearch
}

interface MemberSearchService {
    suspend fun saveDocument(documents: List<MemberDocument>): CustomResult<Unit, SearchError>

    suspend fun deleteDocument(uid: PrimaryKey, objectId: PrimaryKey): CustomResult<Unit, SearchError>

    suspend fun clean(): CustomResult<Unit, SearchError>

    suspend fun searchDocument(
        memberDocumentSearch: MemberDocumentSearch,
    ): CustomResult<PaginationResult<MemberDocument>, SearchError>
}

interface MemberSearchServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun build(env: MergedEnv): MemberSearchService
}
