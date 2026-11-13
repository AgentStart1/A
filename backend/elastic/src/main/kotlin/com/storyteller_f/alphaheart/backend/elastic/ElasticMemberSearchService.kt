/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.elastic

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders
import co.elastic.clients.elasticsearch.core.SearchRequest
import com.storyteller_f.alphaheart.backend.core.ElasticConnection
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.preprocessUserInputKeyword
import com.storyteller_f.alphaheart.backend.core.service.MemberDocument
import com.storyteller_f.alphaheart.backend.core.service.MemberDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.MemberSearchService
import com.storyteller_f.alphaheart.backend.core.service.MemberSearchServiceFactory
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import io.github.aakira.napier.Napier
import kotlinx.coroutines.future.await

class ElasticMemberSearchService(connection: ElasticConnection) :
    Elastic(connection),
    MemberSearchService {
    companion object {
        const val INDEX_NAME = "members"
    }

    override suspend fun saveDocument(documents: List<MemberDocument>): CustomResult<Unit, SearchError> =
        useElasticClient {
            saveDocumentList(connection, documents, INDEX_NAME)
        }

    override suspend fun deleteDocument(uid: PrimaryKey, objectId: PrimaryKey): CustomResult<Unit, SearchError> =
        useElasticClient {
            val query =
                QueryBuilders.bool { b ->
                    b.must { m ->
                        m.term { t ->
                            t.field("uid").value(uid.toString())
                        }
                    }.must { m ->
                        m.term { t ->
                            t.field("objectId").value(objectId.toString())
                        }
                    }
                }

            val response =
                deleteByQuery { d ->
                    d.index(INDEX_NAME).query(query).refresh(connection.refresh)
                }.await()

            Napier.d {
                "elastic delete member document: query=$query, deleted=${response.deleted() ?: "<none>"}"
            }
        }

    override suspend fun clean(): CustomResult<Unit, SearchError> =
        useElasticClient {
            cleanAll(INDEX_NAME)
        }

    override suspend fun searchDocument(
        memberDocumentSearch: MemberDocumentSearch,
    ): CustomResult<PaginationResult<MemberDocument>, SearchError> {
        if (memberDocumentSearch is MemberDocumentSearch.Keyword && memberDocumentSearch.nickname.isEmpty()) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        if (memberDocumentSearch is MemberDocumentSearch.RoomMembers && memberDocumentSearch.objectName.isEmpty()) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        if (memberDocumentSearch is MemberDocumentSearch.CommunityMembers &&
            memberDocumentSearch.objectName.isEmpty()
        ) {
            return CustomResult.success(PaginationResult(emptyList(), 0))
        }
        val request = buildSearchRequest(memberDocumentSearch)
        Napier.i {
            "elastic search member query $request"
        }
        return useElasticClient {
            searchDocumentList(request, MemberDocument::class.java)
        }
    }

    private fun buildSearchRequest(memberDocumentSearch: MemberDocumentSearch): SearchRequest =
        SearchRequest.of { s ->
            s.index(INDEX_NAME).apply {
                when (memberDocumentSearch) {
                    is MemberDocumentSearch.Keyword -> {
                        buildMemberSearchRequest(memberDocumentSearch)
                    }

                    is MemberDocumentSearch.CommunityMembers -> {
                        buildCommunityMemberSearchRequest(memberDocumentSearch)
                    }

                    is MemberDocumentSearch.RoomMembers -> {
                        buildRoomMemberSearchRequest(memberDocumentSearch)
                    }
                }
            }
        }

    private fun SearchRequest.Builder.buildRoomMemberSearchRequest(
        memberDocumentSearch: MemberDocumentSearch.RoomMembers,
    ) {
        val fetch = memberDocumentSearch.fetch
        from(fetch.cursor?.value ?: 0)
        size(fetch.size)
        query { q ->
            q.bool { b ->
                // 按 uid 搜索（该用户加入的房间）
                b.filter { f ->
                    f.term { t ->
                        t.field("uid").value(memberDocumentSearch.uid.toString())
                    }
                }
                // 按 objectType 搜索（只搜索房间）
                b.filter { f ->
                    f.term { t ->
                        t.field("objectType.keyword").value(ObjectType.ROOM.name)
                    }
                }
                // 添加对communityId的过滤
                memberDocumentSearch.communityId?.let { communityId ->
                    b.filter { f ->
                        f.term { t ->
                            t.field("communityId").value(communityId.toString())
                        }
                    }
                }
                // 按房间名称搜索
                val keyword = preprocessUserInputKeyword(memberDocumentSearch.objectName)
                b.prioritizedField(keyword, "objectName")
            }
        }
    }

    private fun SearchRequest.Builder.buildCommunityMemberSearchRequest(
        memberDocumentSearch: MemberDocumentSearch.CommunityMembers,
    ) {
        val fetch = memberDocumentSearch.fetch
        from(fetch.cursor?.value ?: 0)
        size(fetch.size)
        query { q ->
            q.bool { b ->
                // 按 uid 搜索（该用户加入的社区）
                b.filter { f ->
                    f.term { t ->
                        t.field("uid").value(memberDocumentSearch.uid.toString())
                    }
                }
                // 按 objectType 搜索（只搜索社区）
                b.filter { f ->
                    f.term { t ->
                        t.field("objectType.keyword").value(ObjectType.COMMUNITY.name)
                    }
                }
                // 按社区名称搜索
                val keyword = preprocessUserInputKeyword(memberDocumentSearch.objectName)
                b.prioritizedField(keyword, "objectName")
            }
        }
    }

    private fun SearchRequest.Builder.buildMemberSearchRequest(memberDocumentSearch: MemberDocumentSearch.Keyword) {
        val fetch = memberDocumentSearch.fetch
        from(fetch.cursor?.value ?: 0)
        size(fetch.size)
        query { q ->
            q.bool { b ->
                // 按 objectId 搜索
                memberDocumentSearch.objectId?.let { objId ->
                    b.filter { f ->
                        f.term { t ->
                            t.field("objectId").value(objId.toString())
                        }
                    }
                }
                // 按 nickname 搜索
                val keyword = preprocessUserInputKeyword(memberDocumentSearch.nickname)
                b.prioritizedField(keyword, "nickname")
            }
        }
    }
}

class ElasticMemberSearchServiceFactory : MemberSearchServiceFactory {
    override fun match(env: MergedEnv): Boolean = env["SEARCH_SERVICE"] == "elastic"

    override fun build(env: MergedEnv): MemberSearchService =
        buildElasticSearchService(env) {
            ElasticMemberSearchService(it)
        }
}
