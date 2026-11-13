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
import com.storyteller_f.alphaheart.backend.core.service.UserDocument
import com.storyteller_f.alphaheart.backend.core.service.UserDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.UserSearchService
import com.storyteller_f.alphaheart.backend.core.service.UserSearchServiceFactory
import com.storyteller_f.result.CustomResult
import io.github.aakira.napier.Napier

class ElasticUserSearchService(connection: ElasticConnection) :
    Elastic(connection),
    UserSearchService {
    companion object {
        const val INDEX_NAME = "users"
    }
    override suspend fun saveDocument(documents: List<UserDocument>): CustomResult<Unit, SearchError> =
        useElasticClient {
            saveDocumentList(connection, documents, INDEX_NAME)
        }

    override suspend fun clean(): CustomResult<Unit, SearchError> =
        useElasticClient {
            cleanAll(INDEX_NAME)
        }

    override suspend fun searchDocument(
        userDocumentSearch: UserDocumentSearch,
    ): CustomResult<PaginationResult<UserDocument>, SearchError> {
        if (userDocumentSearch is UserDocumentSearch.Keyword && userDocumentSearch.word.isEmpty()) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        val request = buildSearchRequest(userDocumentSearch)
        Napier.i {
            "elastic search query $request"
        }
        return useElasticClient {
            searchDocumentList(request, UserDocument::class.java)
        }
    }

    private fun buildSearchRequest(userDocumentSearch: UserDocumentSearch): SearchRequest =
        SearchRequest.of { s ->
            s.index(INDEX_NAME).apply {
                when (userDocumentSearch) {
                    is UserDocumentSearch.Keyword -> {
                        val fetch = userDocumentSearch.fetch
                        from(fetch.cursor?.value ?: 0)
                        size(fetch.size)
                        val word = userDocumentSearch.word
                        query { q ->
                            q.bool { b ->
                                val keyword = preprocessUserInputKeyword(word)
                                b.prioritizedFields(keyword, "aid", "nickname")
                            }
                        }
                    }
                }
            }
        }
}

class ElasticUserSearchServiceFactory : UserSearchServiceFactory {
    override fun match(env: MergedEnv): Boolean = env["SEARCH_SERVICE"] == "elastic"

    override fun build(env: MergedEnv): UserSearchService =
        buildElasticSearchService(env) {
            ElasticUserSearchService(it)
        }
}
