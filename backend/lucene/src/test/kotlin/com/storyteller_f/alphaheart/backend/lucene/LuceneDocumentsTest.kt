/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.lucene

import com.storyteller_f.alphaheart.backend.core.service.CommunityDocument
import com.storyteller_f.alphaheart.backend.core.service.FileDocument
import com.storyteller_f.alphaheart.backend.core.service.MemberDocument
import com.storyteller_f.alphaheart.backend.core.service.RoomDocument
import com.storyteller_f.alphaheart.backend.core.service.TopicDocument
import com.storyteller_f.alphaheart.backend.core.service.UserDocument
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.services.lucene.api.LuceneRpc
import com.storyteller_f.services.lucene.api.RpcLuceneDocument
import com.storyteller_f.services.lucene.api.RpcLuceneQuery
import com.storyteller_f.services.lucene.api.RpcLuceneResult
import com.storyteller_f.services.lucene.api.RpcLuceneStoredDocument
import com.storyteller_f.services.lucene.api.RpcLuceneValue
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LuceneDocumentsTest {
    @Test
    fun `business documents round trip through typed fields`() {
        val topic = TopicDocument(1, "content", 2, "COMMUNITY", 3, "ROOM", 4)
        val user = UserDocument(1, "nickname", "aid")
        val room = RoomDocument(1, "room", "aid", 2)
        val community = CommunityDocument(1, "community", "aid", 2)
        val member = MemberDocument(1, 2, 3, ObjectType.ROOM, "nickname", "room", 4)
        val file = FileDocument(1, "file", 2)
        assertEquals(topic, LuceneTopicDocument.restore(LuceneTopicDocument(topic).save().stored()))
        assertEquals(user, LuceneUserDocument.restore(LuceneUserDocument(user).save().stored()))
        assertEquals(room, LuceneRoomDocument.restore(LuceneRoomDocument(room).save().stored()))
        assertEquals(community, LuceneCommunityDocument.restore(LuceneCommunityDocument(community).save().stored()))
        assertEquals(member, LuceneMemberDocument.restore(LuceneMemberDocument(member).save().stored()))
        assertEquals(file, LuceneFileDocument.restore(LuceneFileDocument(file).save().stored()))
    }

    @Test
    fun `missing optional fields preserve null values`() {
        val user = UserDocument(1, "nickname", null)
        val emptyAid = user.copy(aid = "")
        val room = RoomDocument(1, "room", "aid")
        val member = MemberDocument(1, 2, 3, ObjectType.COMMUNITY, "nickname", "community")
        assertEquals(user, LuceneUserDocument.restore(LuceneUserDocument(user).save().stored()))
        assertEquals(emptyAid, LuceneUserDocument.restore(LuceneUserDocument(emptyAid).save().stored()))
        assertEquals(room, LuceneRoomDocument.restore(LuceneRoomDocument(room).save().stored()))
        assertEquals(member, LuceneMemberDocument.restore(LuceneMemberDocument(member).save().stored()))
    }

    @Test
    fun `invalid stored field types are rejected`() {
        val document =
            RpcLuceneStoredDocument(
                mapOf(
                    "nickname" to listOf(RpcLuceneValue.Text("nickname")),
                    DOCUMENT_ID_FIELD to listOf(RpcLuceneValue.LongNumber(1)),
                    "aid" to listOf(RpcLuceneValue.LongNumber(2)),
                ),
            )
        assertFailsWith<ClassCastException> { LuceneUserDocument.restore(document) }
        assertFailsWith<NoSuchElementException> { LuceneUserDocument.restore(document.copy(fields = emptyMap())) }
    }

    @Test
    fun `topic lookup uses explicit query fields`() =
        runBlocking {
            val first = TopicDocument(1, "first", 2, "COMMUNITY", 3, "ROOM", 4)
            val second = first.copy(id = 2, content = "second")
            val ids = listOf(2L, 99L, 1L, 2L)
            val rpc =
                object : LuceneRpc {
                    override suspend fun health() = "ok"
                    override suspend fun save(index: String, documents: List<RpcLuceneDocument>): Unit =
                        error(
                            "unexpected save",
                        )
                    override suspend fun replace(
                        index: String,
                        query: RpcLuceneQuery,
                        documents: List<RpcLuceneDocument>,
                    ): Unit = error("Unexpected replacement")
                    override suspend fun clean(index: String): Unit = error("unexpected clean")
                    override suspend fun delete(index: String, query: RpcLuceneQuery): Unit = error("unexpected delete")
                    override suspend fun search(index: String, query: RpcLuceneQuery): RpcLuceneResult {
                        assertEquals("topic", index)
                        assertEquals(mapOf(DOCUMENT_ID_FIELD to ids), query.mustLongSet)
                        assertEquals(ids.size, query.size)
                        return RpcLuceneResult(
                            listOf(
                                LuceneTopicDocument(first).save().stored(),
                                LuceneTopicDocument(second).save().stored(),
                            ),
                            2,
                        )
                    }
                }
            val service = LuceneTopicSearchService(rpc)
            assertEquals(listOf(second, null, first, second), service.getDocuments(ids).getOrThrow())
            assertEquals(emptyList(), service.getDocuments(emptyList()).getOrThrow())
        }

    private fun RpcLuceneDocument.stored() =
        RpcLuceneStoredDocument(
            fields.filter { it.stored }.groupBy { it.name }.mapValues { (_, fields) -> fields.map { it.value } },
        )
}
