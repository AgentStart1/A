/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.elastic

import co.elastic.clients.elasticsearch._types.FieldValue
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch.core.SearchRequest
import com.storyteller_f.alphaheart.backend.core.ElasticConnection
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.preprocessUserInputKeyword
import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.core.service.TopicDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchService
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchServiceFactory
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import io.github.aakira.napier.Napier

class ElasticTopicSearchService(connection: ElasticConnection) :
    Elastic(connection),
    TopicSearchService {
    companion object {
        private const val INDEX_NAME = "topics"
    }

    override suspend fun saveDocument(documents: List<TopicDocument>): CustomResult<Unit, SearchError> {
        if (documents.isEmpty()) return CustomResult.Success(Unit)
        return useElasticClient {
            saveDocumentList(connection, documents, INDEX_NAME)
        }
    }

    override suspend fun getDocuments(idList: List<PrimaryKey>): CustomResult<List<TopicDocument?>, SearchError> {
        if (idList.isEmpty()) return CustomResult.success(emptyList())
        return useElasticClient {
            getDocumentList(idList, INDEX_NAME, TopicDocument::class.java)
        }
    }

    override suspend fun clean(): CustomResult<Unit, SearchError> =
        useElasticClient {
            cleanAll(INDEX_NAME)
        }

    override suspend fun searchDocument(
        topicDocumentSearch: TopicDocumentSearch,
    ): CustomResult<PaginationResult<TopicDocument>, SearchError> {
        if (topicDocumentSearch is TopicDocumentSearch.AllCommunityRoot && topicDocumentSearch.word.isEmpty()) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        if (topicDocumentSearch is TopicDocumentSearch.Topics && topicDocumentSearch.word.isEmpty()) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        if (topicDocumentSearch is TopicDocumentSearch.All && topicDocumentSearch.word.isEmpty()) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        val request = buildSearchRequest(topicDocumentSearch)
        Napier.i {
            "elastic search query $request"
        }
        return useElasticClient {
            searchDocumentList(request, TopicDocument::class.java)
        }
    }

    private fun buildSearchRequest(topicDocumentSearch: TopicDocumentSearch): SearchRequest =
        SearchRequest.of { s ->
            s.index(INDEX_NAME).apply {
                when (topicDocumentSearch) {
                    is TopicDocumentSearch.Recommend -> {
                        buildTopicRecommendSearchRequest(topicDocumentSearch)
                    }

                    is TopicDocumentSearch.RecommendNotLogin -> {
                        buildTopicRecommendNotLoginSearchRequest(topicDocumentSearch)
                    }

                    is TopicDocumentSearch.AllCommunityRoot -> {
                        buildCommunityRootSearchRequest(topicDocumentSearch)
                    }

                    is TopicDocumentSearch.Topics -> {
                        buildTopicSearchRequest(topicDocumentSearch)
                    }

                    is TopicDocumentSearch.All -> {
                        buildAllTopicSearchRequest(topicDocumentSearch)
                    }
                }
            }
        }

    private fun SearchRequest.Builder.buildAllTopicSearchRequest(topicDocumentSearch: TopicDocumentSearch.All) {
        val fetch = topicDocumentSearch.fetch
        from(fetch.cursor?.value ?: 0)
        size(fetch.size)
        val word = topicDocumentSearch.word
        query { q ->
            q.bool { b ->
                val keyword = preprocessUserInputKeyword(word)
                b.prioritizedField(keyword, "content")
            }
        }
    }

    private fun SearchRequest.Builder.buildTopicSearchRequest(topicDocumentSearch: TopicDocumentSearch.Topics) {
        val fetch = topicDocumentSearch.fetch
        from(fetch.cursor?.value ?: 0)
        size(fetch.size)
        val word = topicDocumentSearch.word
        query { q ->
            q.bool { b ->
                b.filter { f ->
                    f.term { t ->
                        t.field("parentId").value(topicDocumentSearch.parentId)
                    }
                }
                val keyword = preprocessUserInputKeyword(word)
                b.prioritizedField(keyword, "content")
            }
        }
    }

    private fun SearchRequest.Builder.buildCommunityRootSearchRequest(
        topicDocumentSearch: TopicDocumentSearch.AllCommunityRoot,
    ) {
        val fetch = topicDocumentSearch.fetch
        from(fetch.cursor?.value ?: 0)
        size(fetch.size)
        val word = topicDocumentSearch.word
        query { q ->
            q.bool { b ->
                b.filter { f ->
                    f.term { t ->
                        t.field("parentType.keyword").value(ObjectType.COMMUNITY.name)
                    }
                }
                val keyword = preprocessUserInputKeyword(word)
                b.prioritizedField(keyword, "content")
            }
        }
    }

    private fun SearchRequest.Builder.buildTopicRecommendNotLoginSearchRequest(
        topicDocumentSearch: TopicDocumentSearch.RecommendNotLogin,
    ) {
        val fetch = topicDocumentSearch.fetch
        from(fetch.cursor?.value ?: 0)
        size(fetch.size)
        query { q ->
            q.term { t ->
                t.field("parentType.keyword").value(ObjectType.COMMUNITY.name)
            }
        }
    }

    private fun SearchRequest.Builder.buildTopicRecommendSearchRequest(
        topicDocumentSearch: TopicDocumentSearch.Recommend,
    ) {
        val fetch = topicDocumentSearch.fetch
        from(fetch.cursor?.value ?: 0)
        size(fetch.size)
        query { q ->
            q.bool { b ->
                b.filter { f ->
                    f.terms { t ->
                        t.field("parentId").terms { builder ->
                            builder.value(
                                topicDocumentSearch.communities.map { id ->
                                    FieldValue.of(id)
                                },
                            )
                        }
                    }
                }.filter { f ->
                    f.term { t ->
                        t.field("author").value(topicDocumentSearch.uid)
                    }
                }
            }
        }
        sort { sort ->
            sort.field { f ->
                f.field("id").order(SortOrder.Desc)
            }
        }
    }
}

class ElasticTopicSearchServiceFactory : TopicSearchServiceFactory {
    override fun match(env: MergedEnv): Boolean = env["SEARCH_SERVICE"] == "elastic"

    override fun build(env: MergedEnv): TopicSearchService =
        buildElasticSearchService(env) {
            ElasticTopicSearchService(it)
        }
}
