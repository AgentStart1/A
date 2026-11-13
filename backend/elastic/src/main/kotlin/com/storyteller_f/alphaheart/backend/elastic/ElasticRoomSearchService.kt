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
import com.storyteller_f.alphaheart.backend.core.service.RoomDocument
import com.storyteller_f.alphaheart.backend.core.service.RoomDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.RoomSearchService
import com.storyteller_f.alphaheart.backend.core.service.RoomSearchServiceFactory
import com.storyteller_f.result.CustomResult
import io.github.aakira.napier.Napier

class ElasticRoomSearchService(connection: ElasticConnection) :
    Elastic(connection),
    RoomSearchService {
    companion object {
        const val INDEX_NAME = "rooms"
    }

    override suspend fun saveDocument(documents: List<RoomDocument>): CustomResult<Unit, SearchError> =
        useElasticClient {
            saveDocumentList(connection, documents, INDEX_NAME)
        }

    override suspend fun clean(): CustomResult<Unit, SearchError> =
        useElasticClient {
            cleanAll(INDEX_NAME)
        }

    override suspend fun searchDocument(
        roomDocumentSearch: RoomDocumentSearch,
    ): CustomResult<PaginationResult<RoomDocument>, SearchError> {
        if (roomDocumentSearch is RoomDocumentSearch.Keyword && roomDocumentSearch.words.isEmpty()) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        val request = buildSearchRequest(roomDocumentSearch)
        Napier.i {
            "elastic search query $request"
        }
        return useElasticClient {
            searchDocumentList(request, RoomDocument::class.java)
        }
    }

    private fun buildSearchRequest(roomDocumentSearch: RoomDocumentSearch): SearchRequest =
        SearchRequest.of { s ->
            s.index(INDEX_NAME).apply {
                when (roomDocumentSearch) {
                    is RoomDocumentSearch.Keyword -> {
                        val fetch = roomDocumentSearch.fetch
                        from(fetch.cursor?.value ?: 0)
                        size(fetch.size)
                        val word = roomDocumentSearch.words
                        query { q ->
                            q.bool { b ->
                                val keyword = preprocessUserInputKeyword(word)
                                b.must { m ->
                                    m.bool { nestedBool ->
                                        nestedBool.prioritizedFields(keyword, "aid", "name")
                                    }
                                }
                                // 添加对communityId的过滤
                                roomDocumentSearch.communityId?.let { communityId ->
                                    b.filter { f ->
                                        f.term { t ->
                                            t.field("communityId").value(communityId)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

class ElasticRoomSearchServiceFactory : RoomSearchServiceFactory {
    override fun match(env: MergedEnv): Boolean = env["SEARCH_SERVICE"] == "elastic"

    override fun build(env: MergedEnv): RoomSearchService =
        buildElasticSearchService(env) {
            ElasticRoomSearchService(it)
        }
}
