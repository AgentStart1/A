/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.types.Community
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.PrimaryKeyIdentifiable
import com.storyteller_f.shared.type.PrimaryKey

data class CommunityDocument(override val id: PrimaryKey, val name: String, val aid: String, val owner: PrimaryKey) :
    PrimaryKeyIdentifiable {
    companion object {
        fun fromCommunity(community: Community): CommunityDocument =
            CommunityDocument(
                community.id,
                community.name,
                community.aid,
                community.owner,
            )
    }
}

sealed interface CommunityDocumentSearch {
    data class Keyword(val keyword: String, val fetch: OffsetFetch) : CommunityDocumentSearch
}

interface CommunitySearchService {
    suspend fun saveDocument(documents: List<CommunityDocument>): CustomResult<Unit, SearchError>

    suspend fun clean(): CustomResult<Unit, SearchError>

    suspend fun searchDocument(
        communityDocumentSearch: CommunityDocumentSearch,
    ): CustomResult<PaginationResult<CommunityDocument>, SearchError>
}

interface CommunitySearchServiceFactory {
    fun match(env: MergedEnv): Boolean
    fun build(env: MergedEnv): CommunitySearchService
}
