/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.NewUser
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.TransferAuthKey
import com.storyteller_f.alphaheart.client.core.addFavorite
import com.storyteller_f.alphaheart.client.core.addReaction
import com.storyteller_f.alphaheart.client.core.addSubscription
import com.storyteller_f.alphaheart.client.core.addUser
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getAllCommunities
import com.storyteller_f.alphaheart.client.core.getAllFiles
import com.storyteller_f.alphaheart.client.core.getAllPrivateRooms
import com.storyteller_f.alphaheart.client.core.getAllPublicRooms
import com.storyteller_f.alphaheart.client.core.getAllTitles
import com.storyteller_f.alphaheart.client.core.getAllTopics
import com.storyteller_f.alphaheart.client.core.getAllUsers
import com.storyteller_f.alphaheart.client.core.getCommunityMembers
import com.storyteller_f.alphaheart.client.core.getFileRefs
import com.storyteller_f.alphaheart.client.core.getRoomFiles
import com.storyteller_f.alphaheart.client.core.getRoomMembers
import com.storyteller_f.alphaheart.client.core.getRoomMembersPublicKeys
import com.storyteller_f.alphaheart.client.core.getTopicTopics
import com.storyteller_f.alphaheart.client.core.getUserById
import com.storyteller_f.alphaheart.client.core.getUserComments
import com.storyteller_f.alphaheart.client.core.getUserFavorites
import com.storyteller_f.alphaheart.client.core.getUserFiles
import com.storyteller_f.alphaheart.client.core.getUserInfo
import com.storyteller_f.alphaheart.client.core.getUserJoinedCommunities
import com.storyteller_f.alphaheart.client.core.getUserJoinedRooms
import com.storyteller_f.alphaheart.client.core.getUserLogs
import com.storyteller_f.alphaheart.client.core.getUserOverview
import com.storyteller_f.alphaheart.client.core.getUserReactions
import com.storyteller_f.alphaheart.client.core.getUserReceivedTitles
import com.storyteller_f.alphaheart.client.core.getUserSubscriptions
import com.storyteller_f.alphaheart.client.core.getUserUploadRecords
import com.storyteller_f.alphaheart.client.core.initChunkUpload
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.overview
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminTest {
    @Test
    fun `test panel login`() =
        test {
            val panelTuple =
                attachPanelSession {
                    assertListSize(0, getAllUsers(PaginationQuery()))
                }
            attachSession()
            loginPanelSession(panelTuple) {
                assertListSize(1, getAllUsers(PaginationQuery()))
            }
        }

    @Test
    fun `test overview user count`() =
        test {
            val outerTuple =
                attachPanelSession {
                    assertEquals(0, overview().getOrThrow().userCount)
                }
            attachSession()
            loginPanelSession(outerTuple) {
                assertEquals(1, overview().getOrThrow().userCount)
            }
        }

    @Test
    fun `test overview community count`() =
        test {
            val outerTuple =
                attachPanelSession {
                    assertEquals(0, overview().getOrThrow().communityCount)
                }
            attachSession {
                createCommunity(NewCommunity("test", "test")).getOrThrow()
            }
            loginPanelSession(outerTuple) {
                assertEquals(1, overview().getOrThrow().communityCount)
            }
        }

    @Test
    fun `test overview topic count`() =
        test {
            val outerTuple =
                attachPanelSession {
                    assertEquals(0, overview().getOrThrow().topicCount)
                }
            attachSession {
                createTopic(ObjectType.USER, it.uid, "test").getOrThrow()
            }
            loginPanelSession(outerTuple) {
                assertEquals(1, overview().getOrThrow().topicCount)
            }
        }

    @Test
    fun `test overview private room count`() =
        test {
            val outerTuple =
                attachPanelSession {
                    assertEquals(0, overview().getOrThrow().privateRoomCount)
                }
            attachSession {
                createRoom(NewRoom("test", "test")).getOrThrow()
            }
            loginPanelSession(outerTuple) {
                assertEquals(1, overview().getOrThrow().privateRoomCount)
            }
        }

    @Test
    fun `test overview public room count`() =
        test {
            val outerTuple =
                attachPanelSession {
                    assertEquals(0, overview().getOrThrow().communityRoomCount)
                }
            attachSession {
                val communityInfo = createCommunity(NewCommunity("test", "test")).getOrThrow()
                val communityId = communityInfo.id
                createRoom(NewRoom("test", "test1", communityId = communityId)).getOrThrow()
            }
            loginPanelSession(outerTuple) {
                assertEquals(1, overview().getOrThrow().communityRoomCount)
            }
        }

    @Test
    fun `test overview file`() =
        test {
            val outerTuple =
                attachPanelSession {
                    val panelOverview = overview().getOrThrow()
                    assertEquals(0, panelOverview.fileCount)
                    assertEquals(0, panelOverview.fileVolume)
                }
            attachSession {
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("hello")).getOrThrow()
            }
            loginPanelSession(outerTuple) {
                val panelOverview = overview().getOrThrow()
                assertEquals(1, panelOverview.fileCount)
                assertEquals(5, panelOverview.fileVolume)
            }
        }

    @Test
    fun `test add user`() =
        test {
            val (privateKey, derPrivateKey, publicKey) =
                getAlgo(AlgoType.P256).run {
                    val privateKey = generatePemKeyPair().getOrThrow().first
                    val derPriKey = getDerPrivateKey(privateKey).getOrThrow()
                    val derPubKey = getDerPublicKeyFromPrivateKey(privateKey).getOrThrow()
                    Triple(privateKey, derPriKey, derPubKey)
                }
            val userInfo =
                attachPanelSession {
                    addUser(NewUser(authKey = TransferAuthKey.P256(publicKey))).getOrThrow()
                }.custom
            getAppSignInSession(
                com.storyteller_f.alphaheart.client.core.AuthKey.P256(
                    privateKey,
                    derPrivateKey,
                    publicKey,
                ),
                onReceive = { _, _, _ ->
                },
            ) {
                val info = getUserInfo(it.uid).getOrThrow()
                assertEquals(info.id, userInfo.id)
            }
        }

    @Test
    fun `admin list communities`() =
        test {
            val outer =
                attachPanelSession {
                    assertListSize(0, getAllCommunities(PaginationQuery()))
                }
            attachSession {
                createCommunity(NewCommunity("c1", "c1")).getOrThrow()
            }
            loginPanelSession(outer) {
                assertListSize(1, getAllCommunities(PaginationQuery()))
            }
        }

    @Test
    fun `admin list public rooms`() =
        test {
            val outer =
                attachPanelSession {
                    assertListSize(0, getAllPublicRooms(PaginationQuery()))
                }
            attachSession { session ->
                val c = createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                createRoom(NewRoom("r1", "desc", communityId = c.id)).getOrThrow()
            }
            loginPanelSession(outer) {
                assertListSize(1, getAllPublicRooms(PaginationQuery()))
            }
        }

    @Test
    fun `admin list private rooms`() =
        test {
            val outer =
                attachPanelSession {
                    assertListSize(0, getAllPrivateRooms(PaginationQuery()))
                }
            attachSession {
                createRoom(NewRoom("r1", "desc")).getOrThrow()
            }
            loginPanelSession(outer) {
                assertListSize(1, getAllPrivateRooms(PaginationQuery()))
            }
        }

    @Test
    fun `admin list topics`() =
        test {
            val outer =
                attachPanelSession {
                    assertListSize(0, getAllTopics(PaginationQuery()))
                }
            attachSession {
                createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
            }
            loginPanelSession(outer) {
                assertListSize(1, getAllTopics(PaginationQuery()))
            }
        }

    @Test
    fun `admin list titles`() =
        test {
            val outer =
                attachPanelSession {
                    assertListSize(0, getAllTitles(PaginationQuery()))
                }
            attachSession { session ->
                val c = createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                createTitle(
                    NewTitle(
                        "c KOL",
                        com.storyteller_f.shared.model.TitleType.REGULAR,
                        session.uid,
                        c.id,
                        ObjectType.COMMUNITY,
                        "hello",
                    ),
                ).getOrThrow()
            }
            loginPanelSession(outer) {
                assertListSize(1, getAllTitles(PaginationQuery()))
            }
        }

    @Test
    fun `admin list files`() =
        test {
            val outer =
                attachPanelSession {
                    assertListSize(0, getAllFiles(PaginationQuery()))
                }
            attachSession {
                upload(it.uid ob ObjectType.USER, getUploadDataFromText("hello")).getOrThrow()
            }
            loginPanelSession(outer) {
                assertListSize(1, getAllFiles(PaginationQuery()))
            }
        }

    @Test
    fun `admin user detail by id`() =
        test {
            val outer = attachPanelSession()
            val uid =
                attachSession {
                }.uid
            loginPanelSession(outer) {
                val info = getUserById(uid).getOrThrow()
                assertEquals(uid, info.id)
            }
        }

    @Test
    fun `admin user joined communities`() =
        test {
            val outer = attachPanelSession()
            val uid =
                attachSession {
                    createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                }.uid
            loginPanelSession(outer) {
                assertListSize(
                    1,
                    getUserJoinedCommunities(uid, CustomApi.Users.JoinedCommunities.UserCommunitiesQuery()),
                )
            }
        }

    @Test
    fun `admin user joined rooms`() =
        test {
            val outer = attachPanelSession()
            val uid =
                attachSession {
                    createRoom(NewRoom("r1", "desc")).getOrThrow()
                }.uid
            loginPanelSession(outer) {
                assertListSize(1, getUserJoinedRooms(uid, PaginationQuery()))
            }
        }

    @Test
    fun `admin user received titles`() =
        test {
            val outer = attachPanelSession()
            val uid =
                attachSession {
                    createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                }.uid
            attachSession {
                val c = createCommunity(NewCommunity("c1", "c2")).getOrThrow()
                createTitle(
                    NewTitle(
                        "c KOL",
                        com.storyteller_f.shared.model.TitleType.REGULAR,
                        uid,
                        c.id,
                        ObjectType.COMMUNITY,
                        "hello",
                    ),
                ).getOrThrow()
            }
            loginPanelSession(outer) {
                assertListSize(
                    1,
                    getUserReceivedTitles(
                        uid,
                        CustomApi.Users.Id.Titles.TitleQuery(searchType = TitleSearchType.RECEIVER),
                    ),
                )
            }
        }

    @Test
    fun `admin user files`() =
        test {
            val outer = attachPanelSession {}
            val uid =
                attachSession {
                    val uid = it.uid
                    upload(uid ob ObjectType.USER, getUploadDataFromText("hello")).getOrThrow()
                }.uid
            loginPanelSession(outer) {
                assertListSize(1, getUserFiles(uid, PaginationQuery()))
            }
        }

    @Test
    fun `admin user logs after join community`() =
        test {
            val outer = attachPanelSession {}
            val outerTuple =
                attachSession {
                    val c = createCommunity(NewCommunity("lc1", "lc1")).getOrThrow()
                    val communityId = c.id
                    joinCommunity(communityId).getOrThrow().id
                }
            val uid = outerTuple.uid
            val communityId = outerTuple.custom
            loginPanelSession(outer) {
                val logs = getUserLogs(uid, PaginationQuery()).getOrThrow().data
                kotlin.test.assertTrue(
                    logs.any { it.objectType == ObjectType.COMMUNITY && it.objectId == communityId },
                    "Expect community-related log for user $uid",
                )
            }
        }

    @Test
    fun `admin community members`() =
        test {
            val outer = attachPanelSession {}
            val communityId =
                attachSession {
                    val c = createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                    joinCommunity(c.id).getOrThrow()
                    c.id
                }.custom
            loginPanelSession(outer) {
                assertListSize(1, getCommunityMembers(communityId, PaginationQuery()))
            }
        }

    @Test
    fun `admin room members`() =
        test {
            val outer = attachPanelSession {}
            val roomId =
                attachSession {
                    val r = createRoom(NewRoom("r1", "desc")).getOrThrow()
                    r.id
                }.custom
            loginPanelSession(outer) {
                assertListSize(1, getRoomMembers(roomId, PaginationQuery()))
            }
        }

    @Test
    fun `admin room files`() =
        test {
            val outer = attachPanelSession {}
            val roomId =
                attachSession {
                    val r = createRoom(NewRoom("r1", "desc")).getOrThrow()
                    upload(r.id ob ObjectType.ROOM, getUploadDataFromText("hello room file")).getOrThrow()
                    r.id
                }.custom
            loginPanelSession(outer) {
                assertListSize(1, getRoomFiles(roomId, PaginationQuery()))
            }
        }

    @Test
    fun `admin user upload records`() =
        test {
            val outer = attachPanelSession {}
            val uid =
                attachSession {
                    val uid = it.uid
                    val checksums =
                        com.storyteller_f.shared.utils.calculateFileChecksums(
                            kotlinx.io.Buffer().apply { write("hello".encodeToByteArray()) },
                        )
                    initChunkUpload(
                        uid ob ObjectType.USER,
                        "hello.txt",
                        5,
                        io.ktor.http.ContentType.Text.Plain,
                        5,
                        checksums.sha256,
                        checksums.crc32c,
                    ).getOrThrow()
                }.uid
            loginPanelSession(outer) {
                val records = getUserUploadRecords(uid, PaginationQuery()).getOrThrow().data
                assertEquals(1, records.size)
                assertEquals(
                    "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
                    records.single().sha256,
                )
                assertEquals("mnG7TA==", records.single().crc32c)
            }
        }

    @Test
    fun `admin get user topics`() =
        test {
            val outer = attachPanelSession()
            val uid =
                attachSession {
                    createTopic(ObjectType.USER, it.uid, "user topic").getOrThrow()
                }.uid
            loginPanelSession(outer) {
                val topics = getAllTopics(PaginationQuery()).getOrThrow().data
                assertEquals(1, topics.size)
                assertEquals(ObjectType.USER, topics[0].rootType)
                assertEquals(uid, topics[0].rootId)
            }
        }

    @Test
    fun `admin get community topics`() =
        test {
            val outer = attachPanelSession()
            val communityId =
                attachSession {
                    val c = createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                    createTopic(ObjectType.COMMUNITY, c.id, "community topic").getOrThrow()
                    c.id
                }.custom
            loginPanelSession(outer) {
                val topics = getAllTopics(PaginationQuery()).getOrThrow().data
                assertEquals(1, topics.size)
                assertEquals(ObjectType.COMMUNITY, topics[0].rootType)
                assertEquals(communityId, topics[0].rootId)
            }
        }

    @Test
    fun `admin get room topics`() =
        test {
            val outer = attachPanelSession()
            val receivedFrame = mutableListOf<RoomFrame>()
            val roomId =
                attachSession(onReceive = { frame, _, _ ->
                    receivedFrame.add(frame)
                }) {
                    val c = createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                    joinCommunity(c.id).getOrThrow()
                    val r = createRoom(NewRoom("r1", "desc", communityId = c.id)).getOrThrow()
                    createTopicInRoomAndWait(receivedFrame) {
                        sendMessage(
                            ObjectTuple(r.id, ObjectType.ROOM),
                            r.isPrivate,
                            "hello",
                            getRoomMembersPublicKeys(r.id, PaginationQuery(null, size = 10)).getOrThrow().data,
                        )
                    }
                    r.id
                }.custom
            loginPanelSession(outer) {
                val topics = getAllTopics(PaginationQuery()).getOrThrow().data
                assertEquals(1, topics.size)
                assertEquals(ObjectType.ROOM, topics[0].rootType)
                assertEquals(roomId, topics[0].rootId)
            }
        }

    @Test
    fun `admin get topic sub topics`() =
        test {
            val outer = attachPanelSession()
            val topicId =
                attachSession {
                    val parentTopic = createTopic(ObjectType.USER, it.uid, "parent topic").getOrThrow()
                    createTopic(ObjectType.TOPIC, parentTopic.id, "sub topic 1").getOrThrow()
                    createTopic(ObjectType.TOPIC, parentTopic.id, "sub topic 2").getOrThrow()
                    parentTopic.id
                }.custom
            loginPanelSession(outer) {
                val subTopics = getTopicTopics(topicId, TopicPinSearch.UNSPECIFIED, PaginationQuery()).getOrThrow().data
                assertEquals(2, subTopics.size)
                // Sub topics inherit root from parent
                assertEquals(ObjectType.USER, subTopics[0].rootType)
                assertEquals(topicId, subTopics[0].parentId)
                assertEquals(ObjectType.TOPIC, subTopics[0].parentType)
                assertEquals(ObjectType.USER, subTopics[1].rootType)
                assertEquals(topicId, subTopics[1].parentId)
                assertEquals(ObjectType.TOPIC, subTopics[1].parentType)
            }
        }

    @Test
    fun `admin get all topic types`() =
        test {
            val outer = attachPanelSession()
            val receivedFrame = mutableListOf<RoomFrame>()
            attachSession(onReceive = { frame, _, _ ->
                receivedFrame.add(frame)
            }) { session ->
                // Create user topic
                createTopic(ObjectType.USER, session.uid, "user topic").getOrThrow()

                // Create community topic
                val c = createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                createTopic(ObjectType.COMMUNITY, c.id, "community topic").getOrThrow()

                // Create community room topic
                joinCommunity(c.id).getOrThrow()
                val r = createRoom(NewRoom("r1", "desc", communityId = c.id)).getOrThrow()
                createTopicInRoomAndWait(receivedFrame) {
                    sendMessage(
                        ObjectTuple(r.id, ObjectType.ROOM),
                        r.isPrivate,
                        "hello",
                        getRoomMembersPublicKeys(r.id, PaginationQuery(null, size = 10)).getOrThrow().data,
                    )
                }

                // Create topic sub topic
                val parentTopic = createTopic(ObjectType.USER, session.uid, "parent topic").getOrThrow()
                createTopic(ObjectType.TOPIC, parentTopic.id, "sub topic").getOrThrow()
            }
            loginPanelSession(outer) {
                val allTopics = getAllTopics(PaginationQuery()).getOrThrow().data
                assertEquals(5, allTopics.size)

                // Verify we have all types
                val rootTypes = allTopics.map { it.rootType }.toSet()
                kotlin.test.assertTrue(rootTypes.contains(ObjectType.USER))
                kotlin.test.assertTrue(rootTypes.contains(ObjectType.COMMUNITY))
                kotlin.test.assertTrue(rootTypes.contains(ObjectType.ROOM))

                // Verify sub topic
                val subTopics = allTopics.filter { it.parentType == ObjectType.TOPIC }
                assertEquals(1, subTopics.size)
            }
        }

    @Test
    fun `admin get user reactions`() =
        test {
            val outer = attachPanelSession()
            val emoji1 = "😀"
            val emoji2 = "👍"
            val userTuple =
                attachSession { session ->
                    // Create topics and add reactions
                    val topic1 = createTopic(ObjectType.USER, session.uid, "topic 1").getOrThrow()
                    val topic2 = createTopic(ObjectType.USER, session.uid, "topic 2").getOrThrow()

                    // Add reactions to different topics
                    addReaction(topic1.id, emoji1).getOrThrow()
                    addReaction(topic2.id, emoji2).getOrThrow()
                }

            loginPanelSession(outer) {
                val reactions = getUserReactions(userTuple.uid, PaginationQuery()).getOrThrow().data
                assertEquals(2, reactions.size)

                // Verify reaction info
                val emojis = reactions.map { it.emoji }.toSet()
                kotlin.test.assertTrue(emojis.contains(emoji1))
                kotlin.test.assertTrue(emojis.contains(emoji2))

                // Verify all reactions belong to the user
                reactions.forEach { reaction ->
                    assertEquals(userTuple.uid, reaction.uid)
                }
            }
        }
}

internal class AdminUserActivityTest {
    @Test
    fun `admin get user comments`() =
        test {
            val outer = attachPanelSession()
            val userTuple =
                attachSession { session ->
                    // Create parent topics
                    val userTopic = createTopic(ObjectType.USER, session.uid, "user topic").getOrThrow()
                    val c = createCommunity(NewCommunity("c1", "c1")).getOrThrow()
                    val communityTopic = createTopic(ObjectType.COMMUNITY, c.id, "community topic").getOrThrow()

                    // Create sub topics (comments/replies to other topics)
                    createTopic(ObjectType.TOPIC, userTopic.id, "reply to user topic").getOrThrow()
                    createTopic(ObjectType.TOPIC, communityTopic.id, "reply to community topic").getOrThrow()
                    createTopic(ObjectType.TOPIC, userTopic.id, "another reply to user topic").getOrThrow()
                }

            loginPanelSession(outer) {
                val comments = getUserComments(userTuple.uid, PaginationQuery()).getOrThrow().data
                assertEquals(3, comments.size)

                // Verify all comments are authored by the user
                comments.forEach { comment ->
                    assertEquals(userTuple.uid, comment.author)
                }

                // Verify all are sub topics (comments)
                comments.forEach { comment ->
                    assertEquals(ObjectType.TOPIC, comment.parentType)
                }

                // Verify different root types exist
                val rootTypes = comments.map { comment -> comment.rootType }.toSet()
                kotlin.test.assertTrue(rootTypes.contains(ObjectType.USER))
                kotlin.test.assertTrue(rootTypes.contains(ObjectType.COMMUNITY))
            }
        }

    @Test
    fun `admin user overview counts`() =
        test {
            val outer = attachPanelSession()
            val userTuple =
                attachSession { session ->
                    val t1 = createTopic(ObjectType.USER, session.uid, "topic for counts").getOrThrow()
                    addReaction(t1.id, "😀").getOrThrow()
                    addReaction(t1.id, "👍").getOrThrow()
                    createTopic(ObjectType.TOPIC, t1.id, "reply 1").getOrThrow()
                    createTopic(ObjectType.TOPIC, t1.id, "reply 2").getOrThrow()
                }
            loginPanelSession(outer) {
                val overview = getUserOverview(userTuple.uid).getOrThrow()
                assertEquals(2, overview.reactionRecordCount)
                assertEquals(2, overview.commentCount)
            }
        }

    @Test
    fun `admin get file refs for topic with media`() =
        test {
            val outer = attachPanelSession()
            val result =
                attachSession { session ->
                    // Upload a file to user
                    val fileInfo =
                        upload(
                            session.uid ob ObjectType.USER,
                            getUploadDataFromText("test file"),
                        ).getOrThrow().data.first()

                    // Create a topic that references the file
                    val topic =
                        createTopic(
                            ObjectType.USER,
                            session.uid,
                            "Topic with media: ![image](${fileInfo.name})",
                        ).getOrThrow()

                    fileInfo.id to topic.id
                }.custom

            val (fileId, topicId) = result

            loginPanelSession(outer) {
                val refs = getFileRefs(fileId, PaginationQuery()).getOrThrow().data
                assertEquals(1, refs.size)

                val ref = refs[0]
                kotlin.test.assertNotEquals(0L, ref.id) // 验证id字段存在且有效
                assertEquals(topicId, ref.objectId)
                assertEquals(ObjectType.TOPIC, ref.objectType)
                assertEquals(fileId, ref.fileId) // 验证fileId字段
            }
        }

    @Test
    fun `admin get file refs for file with no references`() =
        test {
            val outer = attachPanelSession()
            val fileId =
                attachSession { session ->
                    // Upload a file without any references
                    val fileInfo =
                        upload(
                            session.uid ob ObjectType.USER,
                            getUploadDataFromText("unused file"),
                        ).getOrThrow().data.first()
                    fileInfo.id
                }.custom

            loginPanelSession(outer) {
                val refs = getFileRefs(fileId, PaginationQuery()).getOrThrow().data
                assertEquals(0, refs.size)
            }
        }

    @Test
    fun `admin gets multiple refs for one file`() =
        test {
            val outer = attachPanelSession()
            val result =
                attachSession { session ->
                    // Upload a file
                    val fileInfo =
                        upload(
                            session.uid ob ObjectType.USER,
                            getUploadDataFromText("shared file"),
                        ).getOrThrow().data.first()

                    // Create multiple topics that reference the same file
                    val topic1 =
                        createTopic(
                            ObjectType.USER,
                            session.uid,
                            "Topic 1 with media: ![image](${fileInfo.name})",
                        ).getOrThrow()

                    val topic2 =
                        createTopic(
                            ObjectType.USER,
                            session.uid,
                            "Topic 2 also uses: ![image](${fileInfo.name})",
                        ).getOrThrow()

                    Triple(fileInfo.id, topic1.id, topic2.id)
                }.custom

            val (fileId, topic1Id, topic2Id) = result

            loginPanelSession(outer) {
                val refs = getFileRefs(fileId, PaginationQuery()).getOrThrow().data
                assertEquals(2, refs.size)

                // 验证所有引用都有有效的 id
                refs.forEach { ref ->
                    kotlin.test.assertNotEquals(0L, ref.id)
                }

                // Verify both topics reference the file
                val objectIds = refs.map { ref -> ref.objectId }.toSet()
                kotlin.test.assertTrue(objectIds.contains(topic1Id))
                kotlin.test.assertTrue(objectIds.contains(topic2Id))

                // Verify all references are topics
                refs.forEach { ref ->
                    assertEquals(ObjectType.TOPIC, ref.objectType)
                    assertEquals(fileId, ref.fileId) // 验证fileId字段
                }
            }
        }

    @Test
    fun `admin get user favorites`() =
        test {
            val outer = attachPanelSession()
            val uid =
                attachSession { session ->
                    val topic = createTopic(ObjectType.USER, session.uid, "favorite topic").getOrThrow()
                    addFavorite(com.storyteller_f.alphaheart.api.NewFavorite(ObjectType.TOPIC, topic.id)).getOrThrow()
                    session.uid
                }.custom

            loginPanelSession(outer) {
                val favorites = getUserFavorites(uid, PaginationQuery()).getOrThrow().data
                assertEquals(1, favorites.size)
                assertEquals(ObjectType.TOPIC, favorites[0].objectType)
                assertEquals(uid, favorites[0].uid)
            }
        }

    @Test
    fun `admin get user subscriptions`() =
        test {
            val outer = attachPanelSession()
            val uid =
                attachSession { session ->
                    val topic = createTopic(ObjectType.USER, session.uid, "subscription topic").getOrThrow()
                    addSubscription(NewSubscription(topic.id, ObjectType.TOPIC)).getOrThrow()
                    session.uid
                }.custom

            loginPanelSession(outer) {
                val subscriptions = getUserSubscriptions(uid, PaginationQuery()).getOrThrow().data
                assertEquals(1, subscriptions.size)
                assertEquals(ObjectType.TOPIC, subscriptions[0].objectType)
                assertEquals(uid, subscriptions[0].uid)
            }
        }
}
