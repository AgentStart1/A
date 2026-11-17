/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.buildEncryptedTopicContent
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicInfo
import com.storyteller_f.shared.model.TopicReplyPreview
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserPubKeyInfo
import com.storyteller_f.shared.type.ObjectType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

internal class ReplyDecryptionTest {
    @Test
    fun encryptedRepliesKeepTheirOrderAuthorsAndParentMetadata() =
        runTest {
            val fixture = replyFixture()
            val manager = replyManager(fixture.pass)
            try {
                val original = fixture.detail
                val result = processEncryptedTopic(listOf(original), manager).single()
                assertEquals("parent", assertIs<TopicContent.Plain>(result.content).plain)
                assertEquals("reply", assertIs<TopicContent.Plain>(result.subTopics.first().content).plain)
                assertEquals(listOf(11L, 12L), result.subTopics.map { it.id })
                assertEquals(original.subTopics.map { it.authorInfo }, result.subTopics.map { it.authorInfo })
                assertEquals(original.subTopics.last(), result.subTopics.last())
                assertEquals(original.authorInfo, result.authorInfo)
                assertEquals(original.roomInfo, result.roomInfo)
                assertIs<TopicContent.Encrypted>(original.subTopics.first().content)
            } finally {
                manager.client.close()
            }
        }

    @Test
    fun absentSessionHidesBothParentAndEncryptedReply() =
        runTest {
            val fixture = replyFixture()
            val manager = replyManager(null)
            try {
                val result = processEncryptedTopic(listOf(fixture.detail), manager).single()
                assertIs<TopicContent.Nil>(result.content)
                assertIs<TopicContent.Nil>(result.subTopics.first().content)
                assertEquals(fixture.detail.subTopics.last(), result.subTopics.last())
            } finally {
                manager.client.close()
            }
        }

    @Test
    fun multipleDetailsKeepTheirRepliesWithTheCorrectParent() =
        runTest {
            val fixture = replyFixture()
            val manager = replyManager(fixture.pass)
            val preview = fixture.detail.subTopics.first()
            val otherReply = preview.copy(topicInfo = preview.topicInfo.copy(id = 21, parentId = 20))
            val other =
                fixture.detail.copy(
                    topicInfo = fixture.detail.topicInfo.copy(id = 20, content = TopicContent.Plain("other parent")),
                    subTopics = persistentListOf(otherReply),
                )
            try {
                val result = processEncryptedTopic(listOf(fixture.detail, other), manager)
                assertEquals(listOf(10L, 20L), result.map { it.id })
                assertEquals(
                    listOf(listOf(11L, 12L), listOf(21L)),
                    result.map { it.subTopics.map { reply -> reply.id } },
                )
                assertEquals("other parent", assertIs<TopicContent.Plain>(result.last().content).plain)
                assertEquals("reply", assertIs<TopicContent.Plain>(result.last().subTopics.single().content).plain)
            } finally {
                manager.client.close()
            }
        }

    @Test
    fun failedReplyDecryptionRetainsTheParentAndOtherReply() =
        runTest {
            val fixture = replyFixture()
            val manager = replyManager(fixture.pass)
            val preview = fixture.detail.subTopics.first()
            val content = assertIs<TopicContent.Encrypted>(preview.content)
            try {
                for (invalid in listOf(content.copy(encryptedKey = emptyMap()), content.copy(encrypted = "00"))) {
                    val replies =
                        persistentListOf(
                            preview.copy(topicInfo = preview.topicInfo.copy(content = invalid)),
                            fixture.detail.subTopics.last(),
                        )
                    val detail = fixture.detail.copy(subTopics = replies)
                    val result = processEncryptedTopic(listOf(detail), manager).single()
                    assertEquals("parent", assertIs<TopicContent.Plain>(result.content).plain)
                    assertIs<TopicContent.DecryptFailed>(result.subTopics.first().content)
                    assertEquals(detail.subTopics.last(), result.subTopics.last())
                }
            } finally {
                manager.client.close()
            }
        }

    @Test
    fun replyDecryptionPropagatesCallerCancellation() =
        runTest {
            val fixture = replyFixture()
            val cancellation = CancellationException("cancel reply decryption")
            val pass =
                object : UserPass by fixture.pass {
                    override suspend fun decrypt(
                        encrypted: ByteArray,
                        encryptedAesKey: ByteArray,
                    ): CustomResult<String, CredentialError> = throw cancellation
                }
            val manager = replyManager(pass)
            val detail =
                fixture.detail.copy(topicInfo = fixture.detail.topicInfo.copy(content = TopicContent.Plain("parent")))
            try {
                val thrown =
                    assertFailsWith<CancellationException> {
                        processEncryptedTopic(listOf(detail), manager)
                    }
                assertSame(cancellation, thrown)
            } finally {
                manager.client.close()
            }
        }
}

private suspend fun replyFixture(): ReplyFixture {
    loadCryptoLibIfNeed()
    val auth = getAuthKey(AlgoType.P256)
    val pass = RawUserPass(RawUserPassInfo("synthetic", auth))
    val recipients = listOf(UserPubKeyInfo(1, auth.derPublicKey))
    val parent = buildEncryptedTopicContent("parent", recipients)
    val reply = buildEncryptedTopicContent("reply", recipients)
    val author = UserInfo.EMPTY.copy(id = 1, nickname = "Synthetic author")
    val base = TopicInfo.EMPTY.copy(rootId = 77, rootType = ObjectType.ROOM, isEncrypted = true)
    val encryptedReply = TopicReplyPreview(base.copy(id = 11, parentId = 10, content = reply), author)
    val plainReply = base.copy(id = 12, parentId = 10, content = TopicContent.Plain("plain reply"))
    val replies = persistentListOf(encryptedReply, TopicReplyPreview(plainReply, null))
    return ReplyFixture(
        pass,
        TopicDetail.EMPTY.copy(
            topicInfo = base.copy(id = 10, content = parent),
            authorInfo = author,
            subTopics = replies,
            roomInfo = RoomInfo.EMPTY.copy(id = 77),
        ),
    )
}

private data class ReplyFixture(val pass: UserPass, val detail: TopicDetail)

private fun replyManager(pass: UserPass?): SimpleUserSessionManager {
    val client = HttpClient(MockEngine) { engine { addHandler { error("Unexpected network request") } } }
    val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY.copy(id = 1)) }
    val holder = ConstPassHolder(pass)
    val socket = WebSocketClientImpl(model, holder, { _, _ -> error("Unexpected socket connection") }, { _, _ -> })
    return SimpleUserSessionManager(client, socket, model, AcceptAllCookiesStorage(), holder)
}
