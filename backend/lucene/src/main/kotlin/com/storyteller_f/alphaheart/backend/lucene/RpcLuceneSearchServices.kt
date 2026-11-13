/*
 * This is a private project. All rights reserved.
 */
package com.storyteller_f.alphaheart.backend.lucene

import com.storyteller_f.alphaheart.backend.core.ManagedRpcService
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.RpcServiceProvider
import com.storyteller_f.alphaheart.backend.core.error.SearchError
import com.storyteller_f.alphaheart.backend.core.service.CommunityDocument
import com.storyteller_f.alphaheart.backend.core.service.CommunityDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.CommunitySearchService
import com.storyteller_f.alphaheart.backend.core.service.CommunitySearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.FileDocument
import com.storyteller_f.alphaheart.backend.core.service.FileDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.FileSearchService
import com.storyteller_f.alphaheart.backend.core.service.FileSearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.MemberDocument
import com.storyteller_f.alphaheart.backend.core.service.MemberDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.MemberSearchService
import com.storyteller_f.alphaheart.backend.core.service.MemberSearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.RoomDocument
import com.storyteller_f.alphaheart.backend.core.service.RoomDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.RoomSearchService
import com.storyteller_f.alphaheart.backend.core.service.RoomSearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.core.service.TopicDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchService
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.UserDocument
import com.storyteller_f.alphaheart.backend.core.service.UserDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.UserSearchService
import com.storyteller_f.alphaheart.backend.core.service.UserSearchServiceFactory
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.services.lucene.api.LuceneRpc
import com.storyteller_f.services.lucene.api.RpcLuceneQuery
import com.storyteller_f.services.lucene.api.RpcLuceneResult
import com.storyteller_f.services.lucene.api.RpcLuceneSort
import com.storyteller_f.services.lucene.api.RpcLuceneSortType
import com.storyteller_f.services.lucene.api.RpcLuceneStoredDocument
import com.storyteller_f.services.lucene.api.RpcLuceneTextQuery
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.rpc.withService
import java.util.concurrent.ConcurrentHashMap

private inline fun <T> rpcResult(block: () -> T): CustomResult<T, SearchError> =
    captureFailure(
        SearchError::from,
        block,
    )
private fun OffsetFetch.query() = RpcLuceneQuery(offset = cursor?.value ?: 0, size = size)
private fun RpcLuceneQuery.withText(word: String, vararg fields: String) =
    copy(
        text = listOf(RpcLuceneTextQuery(word, fields.toList())),
    )
private fun <T> RpcLuceneResult.toPage(convert: (RpcLuceneStoredDocument) -> T) =
    PaginationResult(documents.map(convert), total)

class LuceneTopicSearchService(private val connection: RpcServiceProvider<LuceneRpc>) : TopicSearchService {
    constructor(rpc: LuceneRpc) : this(RpcServiceProvider { rpc })
    override suspend fun saveDocument(documents: List<TopicDocument>) =
        rpcResult {
            connection.acquire().save(
                "topic",
                documents.map { LuceneTopicDocument(it).save() },
            )
        }
    override suspend fun getDocuments(idList: List<PrimaryKey>) =
        rpcResult {
            if (idList.isEmpty()) return@rpcResult emptyList<TopicDocument?>()
            val documents =
                connection.acquire().search(
                    "topic",
                    RpcLuceneQuery(mustLongSet = mapOf(DOCUMENT_ID_FIELD to idList), size = idList.size),
                ).documents.map(LuceneTopicDocument.Companion::restore).associateBy { it.id }
            idList.map(documents::get)
        }
    override suspend fun clean() = rpcResult { connection.acquire().clean("topic") }
    override suspend fun searchDocument(search: TopicDocumentSearch) =
        rpcResult {
            val query =
                when (search) {
                    is TopicDocumentSearch.Recommend ->
                        search.fetch.query().copy(
                            mustLongSet = mapOf("parentId" to search.communities),
                            mustNotLong = mapOf("author" to search.uid),
                            sort = listOf(RpcLuceneSort(DOCUMENT_ID_FIELD, RpcLuceneSortType.LONG, descending = true)),
                        )

                    is TopicDocumentSearch.RecommendNotLogin ->
                        search.fetch.query().copy(
                            mustKeyword = mapOf("parentType" to ObjectType.COMMUNITY.name),
                            sort = listOf(RpcLuceneSort(DOCUMENT_ID_FIELD, RpcLuceneSortType.LONG, descending = true)),
                        )

                    is TopicDocumentSearch.AllCommunityRoot ->
                        search.fetch.query().copy(
                            mustKeyword = mapOf("parentType" to ObjectType.COMMUNITY.name),
                        ).withText(search.word, "content")

                    is TopicDocumentSearch.Topics ->
                        search.fetch.query().copy(
                            mustLong = mapOf("parentId" to search.parentId),
                        ).withText(search.word, "content")

                    is TopicDocumentSearch.All -> search.fetch.query().withText(search.word, "content")
                }
            if ((
                    search is TopicDocumentSearch.AllCommunityRoot || search is TopicDocumentSearch.Topics ||
                        search is TopicDocumentSearch.All
                    ) &&
                query.text.single().word.isEmpty()
            ) {
                PaginationResult(
                    emptyList(),
                    0,
                )
            } else {
                connection.acquire().search("topic", query).toPage(LuceneTopicDocument.Companion::restore)
            }
        }
}

class LuceneUserSearchService(private val connection: RpcServiceProvider<LuceneRpc>) : UserSearchService {
    constructor(rpc: LuceneRpc) : this(RpcServiceProvider { rpc })
    override suspend fun saveDocument(documents: List<UserDocument>) =
        rpcResult {
            connection.acquire().save(
                "user",
                documents.map { LuceneUserDocument(it).save() },
            )
        }
    override suspend fun clean() = rpcResult { connection.acquire().clean("user") }
    override suspend fun searchDocument(search: UserDocumentSearch) =
        rpcResult {
            when (search) {
                is UserDocumentSearch.Keyword ->
                    if (search.word.isEmpty()) {
                        PaginationResult(
                            emptyList(),
                            0,
                        )
                    } else {
                        connection.acquire().search(
                            "user",
                            search.fetch.query().withText(search.word, "aid", "nickname"),
                        ).toPage(LuceneUserDocument.Companion::restore)
                    }
            }
        }
}

class LuceneRoomSearchService(private val connection: RpcServiceProvider<LuceneRpc>) : RoomSearchService {
    constructor(rpc: LuceneRpc) : this(RpcServiceProvider { rpc })
    override suspend fun saveDocument(documents: List<RoomDocument>) =
        rpcResult {
            if (documents.isEmpty()) return@rpcResult Unit
            connection.acquire().replace(
                "room",
                RpcLuceneQuery(mustLongSet = mapOf(DOCUMENT_ID_FIELD to documents.map { it.id })),
                documents.map { LuceneRoomDocument(it).save() },
            )
        }
    override suspend fun clean() = rpcResult { connection.acquire().clean("room") }
    override suspend fun searchDocument(search: RoomDocumentSearch) =
        rpcResult {
            when (search) {
                is RoomDocumentSearch.Keyword ->
                    if (search.words.isEmpty()) {
                        PaginationResult(
                            emptyList(),
                            0,
                        )
                    } else {
                        connection.acquire().search(
                            "room",
                            search.fetch.query().copy(
                                mustLong = search.communityId?.let { mapOf("communityId" to it) }.orEmpty(),
                            ).withText(search.words, "aid", "name"),
                        ).toPage(LuceneRoomDocument.Companion::restore)
                    }
            }
        }
}

class LuceneCommunitySearchService(private val connection: RpcServiceProvider<LuceneRpc>) : CommunitySearchService {
    constructor(rpc: LuceneRpc) : this(RpcServiceProvider { rpc })
    override suspend fun saveDocument(documents: List<CommunityDocument>) =
        rpcResult {
            connection.acquire().save(
                "community",
                documents.map { LuceneCommunityDocument(it).save() },
            )
        }
    override suspend fun clean() = rpcResult { connection.acquire().clean("community") }
    override suspend fun searchDocument(search: CommunityDocumentSearch) =
        rpcResult {
            when (search) {
                is CommunityDocumentSearch.Keyword ->
                    if (search.keyword.isEmpty()) {
                        PaginationResult(
                            emptyList(),
                            0,
                        )
                    } else {
                        connection.acquire().search(
                            "community",
                            search.fetch.query().withText(search.keyword, "aid", "name"),
                        ).toPage(LuceneCommunityDocument.Companion::restore)
                    }
            }
        }
}

class LuceneMemberSearchService(private val connection: RpcServiceProvider<LuceneRpc>) : MemberSearchService {
    constructor(rpc: LuceneRpc) : this(RpcServiceProvider { rpc })
    override suspend fun saveDocument(documents: List<MemberDocument>) =
        rpcResult {
            if (documents.isEmpty()) return@rpcResult Unit
            connection.acquire().replace(
                "member",
                RpcLuceneQuery(mustLongSet = mapOf(DOCUMENT_ID_FIELD to documents.map { it.id })),
                documents.map { LuceneMemberDocument(it).save() },
            )
        }
    override suspend fun deleteDocument(uid: PrimaryKey, objectId: PrimaryKey) =
        rpcResult {
            connection.acquire().delete(
                "member",
                RpcLuceneQuery(mustLong = mapOf("uid" to uid, "objectId" to objectId)),
            )
        }
    override suspend fun clean() = rpcResult { connection.acquire().clean("member") }
    override suspend fun searchDocument(search: MemberDocumentSearch) =
        rpcResult {
            val query =
                when (search) {
                    is MemberDocumentSearch.Keyword ->
                        search.fetch.query().copy(
                            mustLong = search.objectId?.let { mapOf("objectId" to it) }.orEmpty(),
                        ).withText(search.nickname, "nickname")

                    is MemberDocumentSearch.CommunityMembers ->
                        search.fetch.query().copy(
                            mustLong = mapOf("uid" to search.uid),
                            mustKeyword = mapOf("objectType" to ObjectType.COMMUNITY.name),
                        ).withText(search.objectName, "objectName")

                    is MemberDocumentSearch.RoomMembers ->
                        search.fetch.query().copy(
                            mustLong =
                            buildMap {
                                put("uid", search.uid)
                                search.communityId?.let { put("communityId", it) }
                            },
                            mustKeyword = mapOf("objectType" to ObjectType.ROOM.name),
                        ).withText(search.objectName, "objectName")
                }
            if (query.text.single().word.isBlank()) {
                PaginationResult(
                    emptyList(),
                    0,
                )
            } else {
                connection.acquire().search("member", query).toPage(LuceneMemberDocument.Companion::restore)
            }
        }
}

class LuceneFileSearchService(private val connection: RpcServiceProvider<LuceneRpc>) : FileSearchService {
    constructor(rpc: LuceneRpc) : this(RpcServiceProvider { rpc })
    override suspend fun saveDocument(documents: List<FileDocument>) =
        rpcResult {
            connection.acquire().save(
                "file",
                documents.map { LuceneFileDocument(it).save() },
            )
        }
    override suspend fun clean() = rpcResult { connection.acquire().clean("file") }
    override suspend fun searchDocument(search: FileDocumentSearch) =
        rpcResult {
            when (search) {
                is FileDocumentSearch.Keyword ->
                    if (search.word.isEmpty()) {
                        PaginationResult(
                            emptyList(),
                            0,
                        )
                    } else {
                        connection.acquire().search(
                            "file",
                            search.fetch.query().copy(
                                mustLong = search.ownerId?.let { mapOf("ownerId" to it) }.orEmpty(),
                            ).withText(search.word, FILE_NAME_EXACT_FIELD, "name"),
                        ).toPage(LuceneFileDocument.Companion::restore)
                    }
            }
        }
}

private object LuceneRpcConnections {
    private val connections = ConcurrentHashMap<String, ManagedRpcService<LuceneRpc>>()
    fun service(env: MergedEnv): RpcServiceProvider<LuceneRpc> {
        val rpcUrl = env["LUCENE_RPC_URL"] ?: error("LUCENE_RPC_URL is empty")
        return connections.computeIfAbsent(rpcUrl) {
            ManagedRpcService(rpcUrl, { it.withService<LuceneRpc>() }, { it.health() })
        }
    }
}
open class LuceneSearchServiceFactory {
    protected fun service(env: MergedEnv) =
        LuceneRpcConnections.service(
            env,
        )
        ; protected fun matches(env: MergedEnv) = env["SEARCH_SERVICE"] == "lucene"
}
class LuceneTopicSearchServiceFactory :
    LuceneSearchServiceFactory(),
    TopicSearchServiceFactory {
    override fun match(env: MergedEnv) =
        matches(
            env,
        )
        ; override fun build(env: MergedEnv): TopicSearchService = LuceneTopicSearchService(service(env))
}
class LuceneUserSearchServiceFactory :
    LuceneSearchServiceFactory(),
    UserSearchServiceFactory {
    override fun match(env: MergedEnv) =
        matches(
            env,
        )
        ; override fun build(env: MergedEnv): UserSearchService = LuceneUserSearchService(service(env))
}
class LuceneRoomSearchServiceFactory :
    LuceneSearchServiceFactory(),
    RoomSearchServiceFactory {
    override fun match(env: MergedEnv) =
        matches(
            env,
        )
        ; override fun build(env: MergedEnv): RoomSearchService = LuceneRoomSearchService(service(env))
}
class LuceneCommunitySearchServiceFactory :
    LuceneSearchServiceFactory(),
    CommunitySearchServiceFactory {
    override fun match(env: MergedEnv) =
        matches(
            env,
        )
        ; override fun build(env: MergedEnv): CommunitySearchService = LuceneCommunitySearchService(service(env))
}
class LuceneMemberSearchServiceFactory :
    LuceneSearchServiceFactory(),
    MemberSearchServiceFactory {
    override fun match(env: MergedEnv) =
        matches(
            env,
        )
        ; override fun build(env: MergedEnv): MemberSearchService = LuceneMemberSearchService(service(env))
}
class LuceneFileSearchServiceFactory :
    LuceneSearchServiceFactory(),
    FileSearchServiceFactory {
    override fun match(env: MergedEnv) =
        matches(
            env,
        )
        ; override fun build(env: MergedEnv): FileSearchService = LuceneFileSearchService(service(env))
}
