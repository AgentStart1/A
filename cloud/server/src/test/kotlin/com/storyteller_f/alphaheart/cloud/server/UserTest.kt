/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.addChildAccount
import com.storyteller_f.alphaheart.client.core.addFavorite
import com.storyteller_f.alphaheart.client.core.addReadLog
import com.storyteller_f.alphaheart.client.core.addSubscription
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getChildAccounts
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.getFavorites
import com.storyteller_f.alphaheart.client.core.getRoomInfo
import com.storyteller_f.alphaheart.client.core.getRoomMembersPublicKeys
import com.storyteller_f.alphaheart.client.core.getSubscriptions
import com.storyteller_f.alphaheart.client.core.getTopicInfo
import com.storyteller_f.alphaheart.client.core.getUserInfo
import com.storyteller_f.alphaheart.client.core.getUserOverview
import com.storyteller_f.alphaheart.client.core.hasUnreadRooms
import com.storyteller_f.alphaheart.client.core.removeFavorite
import com.storyteller_f.alphaheart.client.core.removeSubscription
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.alphaheart.client.core.updateUserInfo
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.obj.UpdateUserBody
import com.storyteller_f.shared.obj.UpdateUserRead
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.sha256
import io.ktor.http.ContentType
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserTest {
    @Test
    fun `test get user`() =
        test {
            attachSession {
                val uid = it.uid
                assertNotNull(uid)
                val aid = getUserInfo(uid).getOrThrow().aid
                assertNull(aid)
            }
        }

    @Test
    fun `test update user aid`() =
        test {
            attachSession {
                val updateRow = updateUserInfo(UpdateUserBody(aid = "aid")).getOrThrow()
                assertEquals(updateRow.aid, "aid")
            }
        }

    @Test
    fun `test update user nickname`() =
        test {
            attachSession {
                updateUserInfo(UpdateUserBody(nickname = "test")).getOrThrow()
                assertEquals("test", getUserInfo(it.uid).getOrThrow().nickname)
            }
        }

    @Test
    fun `test update user avatar`() =
        test {
            attachSession {
                val stream = ClassLoader.getSystemResourceAsStream("avatar1.png")!!.buffered()
                val bytes = stream.readBytes()
                val info = upload(it.uid ob ObjectType.USER, getUploadDataFromBytes(bytes)).getOrThrow().data.first()
                assertEquals("avatar1.png", updateUserInfo(UpdateUserBody(avatar = info.id)).getOrThrow().avatar!!.name)
            }
        }

    @Test
    fun `test login`() =
        test {
            val session = attachSession()
            loginSession(session) {
                assertEquals(session.uid, it.uid)
            }
        }

    @Test
    fun `child account creation supports algorithm pairs`() =
        test {
            forEachChildAccount { host, child ->
                loginSession(host) {
                    val response = getChildAccounts(null, 10).getOrThrow()
                    assertEquals(1, response.pagination?.total)
                    val account = response.data.single()
                    assertEquals(host.uid, account.hostId)
                    assertEquals(child.uid, account.id)
                    assertFalse(account.hasUnreadRoomMessage)
                    assertFalse(getUserOverview().getOrThrow().hasUnreadChildRoomMessage)
                }
            }
        }

    @Test
    fun `child account can sign in`() =
        test {
            forEachChildAccount { _, child ->
                loginSession(child) { assertEquals(child.uid, it.uid) }
            }
        }

    @Test
    fun `child account can create user topic`() =
        test {
            forEachChildAccount { _, child ->
                loginSession(child) {
                    val topic = createTopic(ObjectType.USER, it.uid, "child user topic").getOrThrow()
                    assertEquals(child.uid, topic.rootId)
                    assertEquals(ObjectType.USER, topic.rootType)
                }
            }
        }

    @Test
    fun `child account can create community topic`() =
        test {
            forEachChildAccount { _, child ->
                loginSession(child) {
                    val community = createCommunity(NewCommunity("child community", "c${child.uid}")).getOrThrow()
                    val topic = createTopic(ObjectType.COMMUNITY, community.id, "child community topic").getOrThrow()
                    assertEquals(community.id, topic.rootId)
                    assertEquals(ObjectType.COMMUNITY, topic.rootType)
                }
            }
        }

    @Test
    fun `child account can send public room topic`() =
        test {
            forEachChildAccount { _, child ->
                val frames = mutableListOf<RoomFrame>()
                loginSession(child, onReceive = { frame, _, _ -> frames.add(frame) }) {
                    val community = createCommunity(NewCommunity("child community", "c${child.uid}")).getOrThrow()
                    createRoomTopic("child room", "r${child.uid}", "public topic", community.id, frames)
                    assertTrue(frames.single() is RoomFrame.NewTopicInfo)
                }
            }
        }

    @Test
    fun `child room activity updates host unread state`() =
        test {
            forEachChildAccount { host, child ->
                val frames = mutableListOf<RoomFrame>()
                loginSession(child, onReceive = { frame, _, _ -> frames.add(frame) }) {
                    createRoomTopic("child room", "r${child.uid}", "private topic", null, frames)
                    assertTrue(frames.single() is RoomFrame.NewTopicInfo)
                }
                loginSession(host) {
                    assertTrue(getChildAccounts(null, 10).getOrThrow().data.single().hasUnreadRoomMessage)
                    assertTrue(getUserOverview().getOrThrow().hasUnreadChildRoomMessage)
                }
            }
        }

    private suspend fun TestMate.forEachChildAccount(
        block: suspend TestMate.(SessionOuterTuple<Unit>, SessionOuterTuple<Unit>) -> Unit,
    ) {
        val algorithms =
            listOf(
                com.storyteller_f.shared.model.AlgoType.P256,
                com.storyteller_f.shared.model.AlgoType.DILITHIUM,
            )
        algorithms.forEach { hostAlgo ->
            algorithms.forEach { childAlgo ->
                val host = attachSession(hostAlgo) { addChildAccount(childAlgo).getOrThrow() }
                val account = host.custom
                val key = buildChildAuthKey(hostAlgo, host.authKey, account)
                block(SessionOuterTuple(host.authKey, host.uid, Unit), SessionOuterTuple(key, account.id, Unit))
            }
        }
    }

    private suspend fun buildChildAuthKey(
        hostAlgo: com.storyteller_f.shared.model.AlgoType,
        hostAuthKey: com.storyteller_f.alphaheart.client.core.AuthKey,
        childAccountInfo: com.storyteller_f.shared.model.ChildAccountInfo,
    ): com.storyteller_f.alphaheart.client.core.AuthKey {
        val hostAddress =
            com.storyteller_f.shared.getAlgo(hostAlgo).calcAddress(
                if (hostAuthKey is com.storyteller_f.alphaheart.client.core.AuthKey.Dilithium) {
                    hostAuthKey.derPublicKey
                } else {
                    (hostAuthKey as com.storyteller_f.alphaheart.client.core.AuthKey.P256).derPublicKey
                },
            ).getOrThrow()

        val hostUserPass =
            com.storyteller_f.alphaheart.client.core.RawUserPass(
                com.storyteller_f.alphaheart.client.core.RawUserPassInfo(hostAddress, hostAuthKey),
            )
        val (decrypted, decryptedEnc) =
            hostUserPass.decryptChildAccount(
                childAccountInfo.encryptedPrivateKey,
                childAccountInfo.encryptedAesKey,
                childAccountInfo.algoType,
                childAccountInfo.encryptedEncryptionPrivateKey,
            ).getOrThrow()

        val algoImpl = com.storyteller_f.shared.getAlgo(childAccountInfo.algoType)
        val pem = algoImpl.getPemPrivateKeyFromDer(decrypted).getOrThrow()
        val publicKey = algoImpl.getDerPublicKeyFromPrivateKey(pem).getOrThrow()
        return if (childAccountInfo.algoType == com.storyteller_f.shared.model.AlgoType.DILITHIUM) {
            com.storyteller_f.alphaheart.client.core.AuthKey.Dilithium(
                pemPrivateKey = pem,
                derPrivateKey = decrypted,
                derPublicKey = publicKey,
                pemEncryptionPrivateKey = "",
                derEncryptionPrivateKey = decryptedEnc.orEmpty(),
                derEncryptionPublicKey = "",
            )
        } else {
            com.storyteller_f.alphaheart.client.core.AuthKey.P256(
                pemPrivateKey = pem,
                derPrivateKey = decrypted,
                derPublicKey = publicKey,
            )
        }
    }

    private suspend fun com.storyteller_f.alphaheart.client.core.UserSessionManager.createRoomTopic(
        roomName: String,
        roomAid: String,
        topicName: String,
        communityId: Long?,
        receivedFrame: MutableList<com.storyteller_f.shared.obj.RoomFrame>,
    ) {
        val roomId =
            createRoom(
                NewRoom(name = roomName, aid = roomAid, communityId = communityId),
            ).getOrThrow().id
        val roomInfo = getRoomInfo(roomId).getOrThrow()
        val keys =
            getRoomMembersPublicKeys(
                roomId,
                com.storyteller_f.alphaheart.api.PaginationQuery(null, size = 10),
            ).getOrThrow().data
        createTopicInRoomAndWait(receivedFrame) {
            sendMessage(
                com.storyteller_f.shared.obj.ObjectTuple(roomInfo.id, ObjectType.ROOM),
                roomInfo.isPrivate,
                topicName,
                keys,
            )
        }
    }

    @Test
    fun `test add favorite`() =
        test {
            val outerTuple =
                attachSession {
                    createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
                }
            attachSession {
                val topicId = outerTuple.custom.id
                addFavorite(NewFavorite(ObjectType.TOPIC, topicId)).getOrThrow()
                assertListTotalSize(1, getFavorites(PaginationQuery()))

                val topicInfo = getTopicInfo(topicId).getOrThrow()
                assertNotNull(topicInfo.favoriteId)
            }
        }

    @Test
    fun `test remove favorite`() =
        test {
            val outerTuple =
                attachSession {
                    createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
                }
            attachSession {
                val topicId = outerTuple.custom.id
                addFavorite(NewFavorite(ObjectType.TOPIC, topicId)).getOrThrow()

                removeFavorite(topicId, ObjectType.TOPIC).getOrThrow()

                assertListTotalSize(0, getFavorites(PaginationQuery()))
            }
        }

    @Test
    fun `test add subscription`() =
        test {
            val outerTuple =
                attachSession {
                    createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
                }
            attachSession {
                val topicId = outerTuple.custom.id
                addSubscription(NewSubscription(topicId, ObjectType.TOPIC)).getOrThrow()
                assertListTotalSize(1, getSubscriptions(PaginationQuery()))

                val topicInfo = getTopicInfo(topicId).getOrThrow()
                assertNotNull(topicInfo.subscriptionId)
            }
        }

    @Test
    fun `test remove subscription`() =
        test {
            val outerTuple =
                attachSession {
                    createTopic(ObjectType.USER, it.uid, "hello").getOrThrow()
                }
            attachSession {
                val topicId = outerTuple.custom.id
                addSubscription(NewSubscription(topicId, ObjectType.TOPIC)).getOrThrow()

                removeSubscription(topicId, ObjectType.TOPIC).getOrThrow()

                assertListTotalSize(0, getSubscriptions(PaginationQuery()))
            }
        }

    @Test
    fun `test add user favorite`() =
        test {
            val targetUid = attachSession {}.uid
            attachSession {
                addFavorite(NewFavorite(ObjectType.USER, targetUid)).getOrThrow()
                assertListTotalSize(1, getFavorites(PaginationQuery()))

                val userInfo = getUserInfo(targetUid).getOrThrow()
                assertNotNull(userInfo.favoriteId)
            }
        }

    @Test
    fun `test remove user favorite`() =
        test {
            val targetUid = attachSession {}.uid
            attachSession {
                addFavorite(NewFavorite(ObjectType.USER, targetUid)).getOrThrow()

                removeFavorite(targetUid, ObjectType.USER).getOrThrow()
                assertListTotalSize(0, getFavorites(PaginationQuery()))
            }
        }

    @Test
    fun `test add community favorite`() =
        test {
            val communityId =
                attachSession {
                    createCommunity(NewCommunity("test", "test")).getOrThrow().id
                }.custom
            attachSession {
                addFavorite(NewFavorite(ObjectType.COMMUNITY, communityId)).getOrThrow()
                assertListTotalSize(1, getFavorites(PaginationQuery()))

                val communityInfo = getCommunityInfo(communityId).getOrThrow()
                assertNotNull(communityInfo.favoriteId)
            }
        }

    @Test
    fun `test remove community favorite`() =
        test {
            val communityId =
                attachSession {
                    createCommunity(NewCommunity("test", "test")).getOrThrow().id
                }.custom
            attachSession {
                addFavorite(NewFavorite(ObjectType.COMMUNITY, communityId)).getOrThrow()

                removeFavorite(communityId, ObjectType.COMMUNITY).getOrThrow()
                assertListTotalSize(0, getFavorites(PaginationQuery()))
            }
        }

    @Test
    fun `test add room favorite`() =
        test {
            val roomId =
                attachSession {
                    createRoom(NewRoom("test", "test")).getOrThrow().id
                }.custom
            attachSession {
                addFavorite(NewFavorite(ObjectType.ROOM, roomId)).getOrThrow()
                assertListTotalSize(1, getFavorites(PaginationQuery()))

                val roomInfo = getRoomInfo(roomId).getOrThrow()
                assertNotNull(roomInfo.favoriteId)
            }
        }

    @Test
    fun `test remove room favorite`() =
        test {
            val roomId =
                attachSession {
                    createRoom(NewRoom("test", "test")).getOrThrow().id
                }.custom
            attachSession {
                addFavorite(NewFavorite(ObjectType.ROOM, roomId)).getOrThrow()

                removeFavorite(roomId, ObjectType.ROOM).getOrThrow()
                assertListTotalSize(0, getFavorites(PaginationQuery()))
            }
        }

    @Test
    fun `test add user subscription`() =
        test {
            val targetUid = attachSession {}.uid
            attachSession {
                addSubscription(NewSubscription(targetUid, ObjectType.USER)).getOrThrow()
                assertListTotalSize(1, getSubscriptions(PaginationQuery()))

                val userInfo = getUserInfo(targetUid).getOrThrow()
                assertNotNull(userInfo.subscriptionId)
            }
        }

    @Test
    fun `test remove user subscription`() =
        test {
            val targetUid = attachSession {}.uid
            attachSession {
                addSubscription(NewSubscription(targetUid, ObjectType.USER)).getOrThrow()

                removeSubscription(targetUid, ObjectType.USER).getOrThrow()
                assertListTotalSize(0, getSubscriptions(PaginationQuery()))
            }
        }

    @Test
    fun `test add community subscription`() =
        test {
            val communityId =
                attachSession {
                    createCommunity(NewCommunity("test", "test")).getOrThrow().id
                }.custom
            attachSession {
                addSubscription(NewSubscription(communityId, ObjectType.COMMUNITY)).getOrThrow()
                assertListTotalSize(1, getSubscriptions(PaginationQuery()))

                val communityInfo = getCommunityInfo(communityId).getOrThrow()
                assertNotNull(communityInfo.subscriptionId)
            }
        }

    @Test
    fun `test remove community subscription`() =
        test {
            val communityId =
                attachSession {
                    createCommunity(NewCommunity("test", "test")).getOrThrow().id
                }.custom
            attachSession {
                addSubscription(NewSubscription(communityId, ObjectType.COMMUNITY)).getOrThrow()

                removeSubscription(communityId, ObjectType.COMMUNITY).getOrThrow()
                assertListTotalSize(0, getSubscriptions(PaginationQuery()))
            }
        }

    @Test
    fun `test add room subscription`() =
        test {
            val roomId =
                attachSession {
                    createRoom(NewRoom("test", "test")).getOrThrow().id
                }.custom
            attachSession {
                addSubscription(NewSubscription(roomId, ObjectType.ROOM)).getOrThrow()
                assertListTotalSize(1, getSubscriptions(PaginationQuery()))

                val roomInfo = getRoomInfo(roomId).getOrThrow()
                assertNotNull(roomInfo.subscriptionId)
            }
        }

    @Test
    fun `test remove room subscription`() =
        test {
            val roomId =
                attachSession {
                    createRoom(NewRoom("test", "test")).getOrThrow().id
                }.custom
            attachSession {
                addSubscription(NewSubscription(roomId, ObjectType.ROOM)).getOrThrow()

                removeSubscription(roomId, ObjectType.ROOM).getOrThrow()
                assertListTotalSize(0, getSubscriptions(PaginationQuery()))
            }
        }

    @Test
    fun `test has unread rooms`() =
        test {
            val receivedFrame = mutableListOf<RoomFrame>()
            attachSession(onReceive = { roomFrame, _, _ ->
                receivedFrame.add(roomFrame)
            }) {
                val roomInfo = createRoom(NewRoom("test", "test")).getOrThrow()
                val keys = getRoomMembersPublicKeys(roomInfo.id, PaginationQuery(null, size = 10)).getOrThrow().data
                createTopicInRoomAndWait(receivedFrame) {
                    sendMessage(roomInfo.tuple(), true, "hello", keys)
                }
                val topicId = (receivedFrame.first() as RoomFrame.NewTopicInfo).topicInfo.id

                val response = hasUnreadRooms().getOrThrow()
                assertTrue(response.hasUnread)
                assertTrue(response.unreadCount > 0)

                addReadLog(UpdateUserRead(ObjectTuple(roomInfo.id, ObjectType.ROOM), topicId)).getOrThrow()

                val responseAfterRead = hasUnreadRooms().getOrThrow()
                assertFalse(responseAfterRead.hasUnread)
                assertEquals(0, responseAfterRead.unreadCount)
            }
        }
}

suspend fun getUploadDataFromBytes(bytes: ByteArray) =
    UploadData(
        bytes.size.toLong(),
        "avatar1.png",
        ContentType.parse("image/png"),
        sha256(
            Buffer().apply {
                write(bytes)
            }.peek(),
        ),
    ) {
        Buffer().apply {
            write(bytes)
        }
    }
