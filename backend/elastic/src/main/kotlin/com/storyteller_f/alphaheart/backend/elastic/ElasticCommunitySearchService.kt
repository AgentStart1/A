/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.elastic

import co.elastic.clients.elasticsearch.core.SearchRequest
import com.storyteller_f.alphaheart.backend.core.ElasticConnection
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.preprocessUserInputKeyword
import com.storyteller_f.alphaheart.backend.core.service.CommunityDocument
import com.storyteller_f.alphaheart.backend.core.service.CommunityDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.CommunitySearchService
import com.storyteller_f.alphaheart.backend.core.service.CommunitySearchServiceFactory
import com.storyteller_f.result.CustomResult
import io.github.aakira.napier.Napier

class ElasticCommunitySearchService(connection: ElasticConnection) :
    Elastic(connection),
    CommunitySearchService {
    companion object {
        private const val INDEX_NAME = "topics"
    }

    override suspend fun saveDocument(documents: List<CommunityDocument>): CustomResult<Unit, SearchError> {
        if (documents.isEmpty()) return CustomResult.Success(Unit)
        return useElasticClient {
            saveDocumentList(connection, documents, INDEX_NAME)
        }
    }

    override suspend fun clean(): CustomResult<Unit, SearchError> =
        useElasticClient {
            cleanAll(INDEX_NAME)
        }

    override suspend fun searchDocument(
        communityDocumentSearch: CommunityDocumentSearch,
    ): CustomResult<PaginationResult<CommunityDocument>, SearchError> {
        if (communityDocumentSearch is CommunityDocumentSearch.Keyword && communityDocumentSearch.keyword.isEmpty()) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        val request = buildSearchRequest(communityDocumentSearch)
        Napier.i {
            "elastic search query $request"
        }
        return useElasticClient {
            searchDocumentList(request, CommunityDocument::class.java)
        }
    }

    private fun buildSearchRequest(communityDocumentSearch: CommunityDocumentSearch): SearchRequest =
        SearchRequest.of { s ->
            s.index(INDEX_NAME).apply {
                when (communityDocumentSearch) {
                    is CommunityDocumentSearch.Keyword -> {
                        val fetch = communityDocumentSearch.fetch
                        from(fetch.cursor?.value ?: 0)
                        size(fetch.size)
                        val word = communityDocumentSearch.keyword
                        query { q ->
                            q.bool { b ->
                                val keyword = preprocessUserInputKeyword(word)
                                b.prioritizedFields(keyword, "aid", "name")
                            }
                        }
                    }
                }
            }
        }
}

class ElasticCommunitySearchServiceFactory : CommunitySearchServiceFactory {
    override fun match(env: MergedEnv): Boolean = env["SEARCH_SERVICE"] == "elastic"

    override fun build(env: MergedEnv): CommunitySearchService =
        buildElasticSearchService(env) {
            ElasticCommunitySearchService(it)
        }
}
