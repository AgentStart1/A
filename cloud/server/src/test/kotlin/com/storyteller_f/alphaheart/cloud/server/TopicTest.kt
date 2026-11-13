/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.addReaction
import com.storyteller_f.alphaheart.client.core.addReadLog
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.createTopicSnapshot
import com.storyteller_f.alphaheart.client.core.decryptCreatedTopic
import com.storyteller_f.alphaheart.client.core.deleteReaction
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.getCommunityTopics
import com.storyteller_f.alphaheart.client.core.getReactions
import com.storyteller_f.alphaheart.client.core.getRecommendTopics
import com.storyteller_f.alphaheart.client.core.getRoomInfo
import com.storyteller_f.alphaheart.client.core.getRoomMembersPublicKeys
import com.storyteller_f.alphaheart.client.core.getRoomTopics
import com.storyteller_f.alphaheart.client.core.getTopicInfo
import com.storyteller_f.alphaheart.client.core.getUserTopics
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.pinTopic
import com.storyteller_f.alphaheart.client.core.searchCommunityTopics
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.alphaheart.client.core.unpinTopic
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.obj.UpdateUserRead
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.sha256
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.writeString
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Suppress("LongMethod")
class TopicTest {
    @Test
    fun `test community topic search and pagination`() =
        test {
            attachSession {
                val communityId = createCommunity(NewCommunity("aid", "name")).getOrThrow().id
                val lastTopic = createTopic(ObjectType.COMMUNITY, communityId, "hello world").getOrThrow()
                createTopic(ObjectType.COMMUNITY, communityId, "sysroot").getOrThrow()
                val firstTopic = createTopic(ObjectType.COMMUNITY, communityId, "best world").getOrThrow()
                // 使用新的专门方法替换废弃的 searchTopics
                val topics = searchCommunityTopics(communityId, 1, "world").getOrThrow()
                assertEquals(2, topics.pagination?.total)
                assertEquals(1, topics.data.size)
                val topics2 = searchCommunityTopics(communityId, 1, "world", firstTopic.id.toString()).getOrThrow()
                assertEquals(lastTopic.id, topics2.data.first().id)
            }
        }

    @Test
    fun `test community topic author is not null`() =
        test {
            attachSession {
                val communityId = createCommunity(NewCommunity("aid", "name")).getOrThrow().id
                createTopic(ObjectType.COMMUNITY, communityId, "hello world").getOrThrow()
                createTopic(ObjectType.COMMUNITY, communityId, "best world").getOrThrow()
                // 使用新的专门方法替换废弃的 searchTopics
                searchCommunityTopics(communityId, 10, "world").getOrThrow().data.forEach {
                    assertNotNull(it.authorInfo)
                }
                getCommunityTopics(
                    communityId,
                    paginationQuery = PaginationQuery(size = 10),
                ).getOrThrow().data.forEach {
                    assertNotNull(it.authorInfo)
                }
            }
        }

    @Test
    fun `test community topic has comment and comment count`() =
        test {
            attachSession {
                val communityId = createCommunity(NewCommunity("aid", "name")).getOrThrow().id
                val topicId = createTopic(ObjectType.COMMUNITY, communityId, "hello world").getOrThrow().id
                createTopic(ObjectType.TOPIC, topicId, "best world").getOrThrow()
                // 使用新的专门方法替换废弃的 searchTopics
                val data = searchCommunityTopics(communityId, 10, "world").getOrThrow().data
                data.forEach {
                    assertNotNull(it.hasComment)
                    assertEquals(1, it.commentCount)
                }
                getCommunityTopics(
                    communityId,
                    paginationQuery = PaginationQuery(size = 10),
                ).getOrThrow().data.forEach {
                    assertNotNull(it.hasComment)
                    assertEquals(1, it.commentCount)
                }
            }
        }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun `test topic snapshot`() =
        test {
            attachSession { session ->
                val tmpFile = File("src/test/resources/avatar1.png")
                val size = tmpFile.length()
                val info =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromStream(size, tmpFile),
                    ).getOrThrow().data.first()
                val communityId = createCommunity(NewCommunity("name", "aid")).getOrThrow().id
                val topicInfo =
                    createTopic(
                        ObjectType.COMMUNITY,
                        communityId,
                        """hello
                |
                |![png](${info.name})
                        """.trimMargin(),
                    ).getOrThrow()
                val fileInfo = createTopicSnapshot(topicInfo.id).getOrThrow()
                val file = File("build/test/tmp/${Uuid.random()}.pdf")
                downloadFile(file, fileInfo)
                Loader.loadPDF(file).use { document ->
                    val stripper = PDFTextStripper()
                    assertContains(stripper.getText(document), "hello")
                }
            }
        }

    @Test
    fun `test add reaction`() =
        test {
            val emoji = "\uD83D\uDE00"
            attachSession {
                val c = createCommunity(NewCommunity("name", "aid")).getOrThrow()
                val topicInfo = createTopic(ObjectType.COMMUNITY, c.id, "hello").getOrThrow()
                val reaction = addReaction(topicInfo.id, emoji).getOrThrow()
                assertEquals(emoji, reaction.emoji)
                assertTrue(reaction.hasReacted)
                assertEquals(1, reaction.count)
            }
        }

    @Test
    fun `repeated reaction is idempotent`() =
        test {
            val emoji = "\uD83D\uDE00"
            attachSession {
                val c = createCommunity(NewCommunity("name", "aid")).getOrThrow()
                val topicInfo = createTopic(ObjectType.COMMUNITY, c.id, "hello").getOrThrow()
                repeat(4) { addReaction(topicInfo.id, emoji).getOrThrow() }
                val reaction = addReaction(topicInfo.id, emoji).getOrThrow()
                assertEquals(1, reaction.count)
                assertTrue(reaction.hasReacted)
                assertListSize(1, getReactions(topicInfo.id, 10))
            }
        }

    @Test
    fun `test delete reaction`() =
        test {
            val emoji = "\uD83D\uDE00"
            attachSession {
                val c = createCommunity(NewCommunity("name", "aid")).getOrThrow()
                val topicInfo = createTopic(ObjectType.COMMUNITY, c.id, "hello").getOrThrow()
                // 测试幂等
                val old = addReaction(topicInfo.id, emoji).getOrThrow()
                assertEquals(1, old.count)
                assertTrue(old.hasReacted)
                assertListSize(1, getReactions(topicInfo.id, 10))
                val reactionInfo = deleteReaction(emoji, topicInfo.id).getOrThrow()
                assertEquals(0, reactionInfo.count)
                assertFalse(reactionInfo.hasReacted)
                assertListSize(0, getReactions(topicInfo.id, 10))
            }
        }

    @Test
    fun `test reaction permission check`() =
        test {
            val emoji = "\uD83D\uDE00"
            val session =
                attachSession {
                    val communityInfo = createCommunity(NewCommunity("name", "aid")).getOrThrow()
                    val topicInfo = createTopic(ObjectType.COMMUNITY, communityInfo.id, "hello").getOrThrow()
                    addReaction(topicInfo.id, emoji).getOrThrow()
                    topicInfo
                }
            val topicId = session.custom.id
            attachSession {
                assertFails {
                    addReaction(topicId, emoji).getOrThrow()
                }
                joinCommunity(session.custom.rootId)
                addReaction(topicId, emoji).getOrThrow()
            }
        }

    @Test
    fun `test file in user topic`() =
        test {
            attachSession { session ->
                val string = "hello"
                val fileInfo =
                    upload(
                        ObjectTuple(session.uid, ObjectType.USER),
                        getUploadDataFromText(string),
                    ).getOrThrow().data.first()
                val info = createTopic(ObjectType.USER, session.uid, "![hello.txt](${fileInfo.name})").getOrThrow()
                val plain = info.content as TopicContent.Plain
                assertEquals(fileInfo.fullName, plain.fileInfos.first().fullName)
                val topicInfo = getTopicInfo(info.id).getOrThrow()
                assertEquals(fileInfo.fullName, (topicInfo.content as TopicContent.Plain).fileInfos.first().fullName)
            }
        }

    @Test
    fun `test create user topic`() =
        test {
            attachSession {
                createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
                // 查询单个topic
                assertListSize(1, getUserTopics(it.uid, paginationQuery = PaginationQuery(null, null, size = 10)))
                createTopic(ObjectType.USER, it.uid, "test").getOrThrow()
                // 查询多个topic
                assertListSize(2, getUserTopics(it.uid, paginationQuery = PaginationQuery(null, null, size = 10)))
            }
        }

    @Test
    fun `test forbid use api send message in room`() =
        test {
            val (communityId, publicRoomId) =
                attachSession {
                    val communityId = createCommunity(NewCommunity("test1", "test1")).getOrThrow().id
                    val publicRoomId = createRoom(NewRoom("room1", "room1", communityId = communityId)).getOrThrow().id
                    communityId to publicRoomId
                }.custom
            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(publicRoomId).getOrThrow()
                assertFails {
                    createTopic(ObjectType.ROOM, publicRoomId, "forbid use api add topic to room").getOrThrow()
                }
            }
        }

    @Test
    fun `test community room permission check`() =
        test {
            val (communityId, publicRoomId) =
                attachSession {
                    val communityId = createCommunity(NewCommunity("test1", "test1")).getOrThrow().id
                    val publicRoomId = createRoom(NewRoom("room1", "room1", communityId = communityId)).getOrThrow().id
                    communityId to publicRoomId
                }.custom
            val receivedFrame = mutableListOf<RoomFrame>()
            attachSession(onReceive = { roomFrame, _, _ ->
                receivedFrame.add(roomFrame)
            }) {
                val roomInfo = getRoomInfo(publicRoomId).getOrThrow()
                createTopicInRoomAndWait(receivedFrame) {
                    sendMessage(ObjectTuple(roomInfo.id, ObjectType.ROOM), roomInfo.isPrivate, "test", emptyList())
                }
                assertTrue(receivedFrame.first() is RoomFrame.Error)
                joinCommunity(communityId).getOrThrow()
                joinRoom(publicRoomId).getOrThrow()
                createTopicInRoomAndWait(receivedFrame) {
                    sendMessage(ObjectTuple(roomInfo.id, ObjectType.ROOM), roomInfo.isPrivate, "test", emptyList())
                }
                assertListSize(1, getRoomTopics(publicRoomId, paginationQuery = PaginationQuery(size = 10)))
            }
            receivedFrame.clear()
        }

    @Test
    fun `test create topic in private room`() =
        test {
            val user2 = attachSession()
            val user1 =
                attachSession {
                    val id = createRoom(NewRoom("room-create-topic", "room-create-topic")).getOrThrow().id
                    createTitle(NewTitle("join", TitleType.JOIN, user2.uid, id, ObjectType.ROOM, ""))
                    id
                }
            val privateRoomId = user1.custom
            val receivedFrame = mutableListOf<RoomFrame>()
            loginSession(user2, { roomFrame, _, _ ->
                receivedFrame.add(roomFrame)
            }) {
                joinRoom(privateRoomId).getOrThrow()
                val roomInfo2 = getRoomInfo(privateRoomId).getOrThrow()
                val keys = getRoomMembersPublicKeys(privateRoomId, PaginationQuery(null, size = 10)).getOrThrow().data
                createTopicInRoomAndWait(receivedFrame) {
                    sendMessage(ObjectTuple(roomInfo2.id, ObjectType.ROOM), roomInfo2.isPrivate, "hello", keys)
                }
                assertNotNull((receivedFrame.first() as RoomFrame.NewTopicInfo).topicInfo.authorInfo)
            }
            receivedFrame.clear()
        }

    @Test
    fun `test create in user`() =
        test {
            attachSession {
                createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
            }
        }

    @Test
    fun `test recommend`() =
        test {
            attachSession {
                val id = createCommunity(NewCommunity("c1", "c1")).getOrThrow().id
                repeat(4) {
                    createTopic(ObjectType.COMMUNITY, id, "hello $it").getOrThrow()
                }
                id
            }.custom
            val custom2 =
                attachSession {
                    val id = createCommunity(NewCommunity("c2", "c2")).getOrThrow().id
                    createTopic(ObjectType.COMMUNITY, id, "hello 2").getOrThrow()
                    id
                }.custom
            attachSession {
                joinCommunity(custom2).getOrThrow()
                createTopic(ObjectType.COMMUNITY, custom2, "only").getOrThrow()
                assertListSize(1, getRecommendTopics(PaginationQuery(null, size = 10)))
            }
        }

    @Test
    fun `test none session recommend`() =
        test {
            attachSession {
                val id = createCommunity(NewCommunity("c1", "c1")).getOrThrow().id
                repeat(4) {
                    createTopic(ObjectType.COMMUNITY, id, "hello $it").getOrThrow()
                }
            }
            attachSession {
                val id = createCommunity(NewCommunity("c2", "c2")).getOrThrow().id
                createTopic(ObjectType.COMMUNITY, id, "hello 2").getOrThrow()
                id
            }
            noneSession {
                assertEquals(5, getRecommendTopics(PaginationQuery(null, size = 10)).getOrThrow().data.size)
            }
        }

    @Test
    fun `test room last read`() {
        val receivedFrame = mutableListOf<RoomFrame>()
        test {
            attachSession(onReceive = { roomFrame, _, _ ->
                receivedFrame.add(roomFrame)
            }) {
                val roomInfo = createRoom(NewRoom("r1", "r1")).getOrThrow()
                val keys = getRoomMembersPublicKeys(roomInfo.id, PaginationQuery(null, size = 10)).getOrThrow().data
                createTopicInRoomAndWait(receivedFrame) {
                    sendMessage(roomInfo.tuple(), true, "hello", keys)
                }
                val topicId = (receivedFrame.first() as RoomFrame.NewTopicInfo).topicInfo.id
                addReadLog(UpdateUserRead(roomInfo.tuple(), topicId)).getOrThrow()
                assertEquals(topicId, getRoomInfo(roomInfo.id).getOrThrow().lastRead)
                receivedFrame.clear()
            }
        }
    }

    @Test
    fun `test community last read`() =
        test {
            attachSession {
                val communityInfo = createCommunity(NewCommunity("r1", "r1")).getOrThrow()
                val topic = createTopic(ObjectType.COMMUNITY, communityInfo.id, "hello").getOrThrow()
                addReadLog(UpdateUserRead(communityInfo.tuple(), topic.id)).getOrThrow()
                assertEquals(topic.id, getCommunityInfo(communityInfo.id).getOrThrow().lastRead)
            }
        }

    @Test
    fun `reading comment updates topic last read`() =
        test {
            attachSession {
                val communityInfo = createCommunity(NewCommunity("r1", "r1")).getOrThrow()
                val topic = createTopic(ObjectType.COMMUNITY, communityInfo.id, "hello").getOrThrow()
                val subTopic = createTopic(ObjectType.TOPIC, topic.id, "world").getOrThrow()
                addReadLog(UpdateUserRead(topic.tuple(), subTopic.id)).getOrThrow()
                assertEquals(subTopic.id, getTopicInfo(topic.id).getOrThrow().lastRead)
            }
        }

    @Test
    fun `pinning community topic marks it pinned`() =
        test {
            attachSession {
                val community = createCommunity(NewCommunity("r1", "r1")).getOrThrow()
                val topic = createTopic(ObjectType.COMMUNITY, community.id, "hello").getOrThrow()
                assertTrue(pinTopic(topic.id).getOrThrow().isPin)
            }
        }

    @Test
    fun `unpinning community topic clears pin`() =
        test {
            attachSession {
                val community = createCommunity(NewCommunity("r1", "r1")).getOrThrow()
                val topic = createTopic(ObjectType.COMMUNITY, community.id, "hello").getOrThrow()
                pinTopic(topic.id).getOrThrow()
                assertFalse(unpinTopic(topic.id).getOrThrow().isPin)
            }
        }

    @Test
    fun `pinning user topic marks it pinned`() =
        test {
            attachSession {
                val topic = createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
                assertTrue(pinTopic(topic.id).getOrThrow().isPin)
            }
        }

    @Test
    fun `unpinning user topic clears pin`() =
        test {
            attachSession {
                val topic = createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
                pinTopic(topic.id).getOrThrow()
                assertFalse(unpinTopic(topic.id).getOrThrow().isPin)
            }
        }

    @Test
    fun `test topic level`() =
        test {
            attachSession {
                val topicInfo = createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
                assertEquals(1, topicInfo.level)
            }
        }

    @Test
    fun `test community topic latest topic`() =
        test {
            attachSession {
                val communityInfo = createCommunity(NewCommunity("r1", "r1")).getOrThrow()
                val topic = createTopic(ObjectType.COMMUNITY, communityInfo.id, "hello").getOrThrow()
                val info = getCommunityInfo(communityInfo.id).getOrThrow()
                assertEquals(topic.id, info.latestTopic)
            }
        }

    @Test
    fun `test other user can decrypt`() =
        test {
            listOf(
                com.storyteller_f.shared.model.AlgoType.DILITHIUM,
                com.storyteller_f.shared.model.AlgoType.P256,
            ).forEach { algo ->
                val user2 = attachSession(algo = algo) {}
                val user1 =
                    attachSession(algo = algo) {
                        val id = createRoom(NewRoom("room2-${algo.name}", "room2-${algo.name}")).getOrThrow().id
                        createTitle(NewTitle("join", TitleType.JOIN, user2.uid, id, ObjectType.ROOM, ""))
                        id
                    }
                val privateRoomId = user1.custom
                val receivedFrame = mutableListOf<RoomFrame>()
                loginSession(user2, onReceive = { roomFrame, _, _ ->
                    receivedFrame.add(roomFrame)
                }) {
                    joinRoom(privateRoomId).getOrThrow()
                    val roomInfo2 = getRoomInfo(privateRoomId).getOrThrow()
                    val keys =
                        getRoomMembersPublicKeys(
                            privateRoomId,
                            PaginationQuery(null, size = 10),
                        ).getOrThrow().data
                    createTopicInRoomAndWait(receivedFrame) {
                        sendMessage(ObjectTuple(roomInfo2.id, ObjectType.ROOM), roomInfo2.isPrivate, "hello", keys)
                    }
                    val first = receivedFrame.first() as RoomFrame.NewTopicInfo
                    assertEquals(
                        "hello",
                        (
                            decryptCreatedTopic(
                                first.topicInfo,
                                this,
                            ).content as TopicContent.Plain
                            ).plain,
                    )
                }
                receivedFrame.clear()
            }
        }

    @Test
    fun `test P256 and Dilithium users in community room`() =
        test {
            val user1 =
                attachSession(algo = com.storyteller_f.shared.model.AlgoType.P256) {
                    val communityId = createCommunity(NewCommunity("p256-c", "p256-c")).getOrThrow().id
                    val roomId = createRoom(NewRoom("p256-r", "p256-r", communityId = communityId)).getOrThrow().id
                    communityId to roomId
                }
            val (communityId, publicRoomId) = user1.custom
            val user2 =
                attachSession(algo = com.storyteller_f.shared.model.AlgoType.DILITHIUM) {
                    joinCommunity(communityId).getOrThrow()
                    joinRoom(publicRoomId).getOrThrow()
                }
            val receivedFrame1 = mutableListOf<RoomFrame>()
            val receivedFrame2 = mutableListOf<RoomFrame>()

            loginSession(user1, onReceive = { roomFrame, _, _ ->
                receivedFrame1.add(roomFrame)
            }) {
                val roomInfo = getRoomInfo(publicRoomId).getOrThrow()
                val keys = getRoomMembersPublicKeys(publicRoomId, PaginationQuery(null, size = 10)).getOrThrow().data
                createTopicInRoomAndWait(receivedFrame1) {
                    sendMessage(ObjectTuple(roomInfo.id, ObjectType.ROOM), roomInfo.isPrivate, "hello from p256", keys)
                }
            }
            loginSession(user2, onReceive = { roomFrame, _, _ ->
                receivedFrame2.add(roomFrame)
            }) {
                val roomInfo = getRoomInfo(publicRoomId).getOrThrow()
                val keys = getRoomMembersPublicKeys(publicRoomId, PaginationQuery(null, size = 10)).getOrThrow().data
                createTopicInRoomAndWait(receivedFrame2) {
                    sendMessage(
                        ObjectTuple(roomInfo.id, ObjectType.ROOM),
                        roomInfo.isPrivate,
                        "hello from dilithium",
                        keys,
                    )
                }
            }
        }

    @Test
    fun `test P256 and Dilithium users in private room`() =
        test {
            val user2 = attachSession(algo = com.storyteller_f.shared.model.AlgoType.DILITHIUM) {}
            val user1 =
                attachSession(algo = com.storyteller_f.shared.model.AlgoType.P256) {
                    val id = createRoom(NewRoom("p256-r-private", "p256-r-private")).getOrThrow().id
                    createTitle(NewTitle("join", TitleType.JOIN, user2.uid, id, ObjectType.ROOM, ""))
                    id
                }
            val privateRoomId = user1.custom

            loginSession(user2) {
                joinRoom(privateRoomId).getOrThrow()
            }

            val receivedFrame1 = mutableListOf<RoomFrame>()
            val receivedFrame2 = mutableListOf<RoomFrame>()

            loginSession(user1, onReceive = { roomFrame, _, _ ->
                receivedFrame1.add(roomFrame)
            }) {
                val roomInfo = getRoomInfo(privateRoomId).getOrThrow()
                val keys = getRoomMembersPublicKeys(privateRoomId, PaginationQuery(null, size = 10)).getOrThrow().data
                createTopicInRoomAndWait(receivedFrame1) {
                    sendMessage(ObjectTuple(roomInfo.id, ObjectType.ROOM), roomInfo.isPrivate, "hello from p256", keys)
                }
            }
            loginSession(user2, onReceive = { roomFrame, _, _ ->
                receivedFrame2.add(roomFrame)
            }) {
                val roomInfo = getRoomInfo(privateRoomId).getOrThrow()
                val keys = getRoomMembersPublicKeys(privateRoomId, PaginationQuery(null, size = 10)).getOrThrow().data
                createTopicInRoomAndWait(receivedFrame2) {
                    sendMessage(
                        ObjectTuple(roomInfo.id, ObjectType.ROOM),
                        roomInfo.isPrivate,
                        "hello from dilithium",
                        keys,
                    )
                }
            }
        }
}

private suspend fun getUploadDataFromStream(size: Long, tmpFile: File) =
    UploadData(
        size,
        "avatar1.png",
        ContentType.defaultForFileExtension("png"),
        sha256(
            Buffer().apply {
                write(tmpFile.readBytes())
            }.peek(),
        ),
    ) {
        tmpFile.inputStream().asInput()
    }

suspend fun getUploadDataFromText(string: String, fileName: String = "hello.txt") =
    UploadData(
        string.length.toLong(),
        fileName,
        ContentType.defaultForFileExtension("txt"),
        sha256(
            Buffer().apply {
                writeString(string)
            }.peek(),
        ),
    ) {
        Buffer().apply {
            writeString(string)
        }
    }

suspend fun UploadData.calcSha256(): String = sha256(block())

suspend fun UserSessionManager.upload(
    objectTuple: ObjectTuple,
    data: UploadData,
    onUpload: suspend (Long, Long?) -> Unit = { _, _ -> },
) = upload(objectTuple, data, onUpload)

suspend fun UserSessionManager.createTopicInRoomAndWait(
    receivedFrame: MutableList<RoomFrame>,
    block: suspend DefaultClientWebSocketSession.() -> Unit,
) {
    val old = receivedFrame.size
    webSocketClient.useWebSocket(block).getOrThrow()
    while (true) {
        if (receivedFrame.size == old + 1) {
            break
        }
        withContext(Dispatchers.IO) {
            delay(100)
        }
    }
}
