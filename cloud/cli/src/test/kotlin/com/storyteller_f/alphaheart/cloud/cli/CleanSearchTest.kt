/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.cli

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CombinedDatabase
import com.storyteller_f.alphaheart.backend.core.CustomConfig
import com.storyteller_f.alphaheart.backend.core.service.NameService
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.lucene.LuceneCommunitySearchService
import com.storyteller_f.alphaheart.backend.lucene.LuceneFileSearchService
import com.storyteller_f.alphaheart.backend.lucene.LuceneMemberSearchService
import com.storyteller_f.alphaheart.backend.lucene.LuceneRoomSearchService
import com.storyteller_f.alphaheart.backend.lucene.LuceneTopicSearchService
import com.storyteller_f.alphaheart.backend.lucene.LuceneUserSearchService
import com.storyteller_f.services.lucene.api.LuceneRpc
import com.storyteller_f.services.lucene.api.RpcLuceneDocument
import com.storyteller_f.services.lucene.api.RpcLuceneQuery
import com.storyteller_f.services.lucene.api.RpcLuceneResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CleanSearchTest {
    @Test
    fun `clean removes every business search index`() =
        runTest {
            val rpc = PopulatedIndexes()
            cleanSearchIndexes(CleaningBackend(rpc))
            assertEquals(emptySet(), rpc.remaining.value)
        }
}

private class CleaningBackend(rpc: LuceneRpc) : Backend {
    override val topicSearchService = LuceneTopicSearchService(rpc)
    override val userSearchService = LuceneUserSearchService(rpc)
    override val roomSearchService = LuceneRoomSearchService(rpc)
    override val communitySearchService = LuceneCommunitySearchService(rpc)
    override val memberSearchService = LuceneMemberSearchService(rpc)
    override val fileSearchService = LuceneFileSearchService(rpc)
    override val customConfig: CustomConfig get() = error("Unused")
    override val objectStorageService: ObjectStorageService get() = error("Unused")
    override val nameService: NameService get() = error("Unused")
    override val database: CombinedDatabase get() = error("Unused")
}

private class PopulatedIndexes : LuceneRpc {
    val remaining = MutableStateFlow(setOf("topic", "user", "room", "community", "member", "file"))
    override suspend fun health() = "ok"
    override suspend fun replace(index: String, query: RpcLuceneQuery, documents: List<RpcLuceneDocument>): Unit =
        error("Unexpected replacement")
    override suspend fun clean(index: String) {
        remaining.update { it - index }
    }
    override suspend fun save(index: String, documents: List<RpcLuceneDocument>): Unit = error("Unused")
    override suspend fun delete(index: String, query: RpcLuceneQuery): Unit = error("Unused")
    override suspend fun search(index: String, query: RpcLuceneQuery): RpcLuceneResult = error("Unused")
}
