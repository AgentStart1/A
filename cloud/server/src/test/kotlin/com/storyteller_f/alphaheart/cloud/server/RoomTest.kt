/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.types.buildMemberForNotificationRoom
import com.storyteller_f.alphaheart.backend.core.types.buildUserNotificationRoom
import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.exitRoom
import com.storyteller_f.alphaheart.client.core.getCommunityRooms
import com.storyteller_f.alphaheart.client.core.getRoomInfo
import com.storyteller_f.alphaheart.client.core.getRoomInfoByAid
import com.storyteller_f.alphaheart.client.core.getUserRooms
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.searchCommunityRooms
import com.storyteller_f.alphaheart.client.core.searchCurrentUserRooms
import com.storyteller_f.alphaheart.client.core.searchRoomMembers
import com.storyteller_f.alphaheart.client.core.sendFrame
import com.storyteller_f.alphaheart.client.core.updateRoomInfo
import com.storyteller_f.alphaheart.cloud.core.service.checkRoomWritePermission
import com.storyteller_f.alphaheart.cloud.ws.rtcSession
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.obj.CustomAnswer
import com.storyteller_f.shared.obj.CustomOffer
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RoomTest {
    @Test
    fun `anonymous joined room detail requires login`() =
        test(mapOf("DISABLE_TEST_WS" to "true")) {
            val objectId =
                attachSession {
                    createPublicRoomForTest(
                        createCommunityForTest().id,
                        "room",
                        "Room",
                    ).id
                }.custom
            noneSession {
                val failure =
                    assertFailsWith<ServerErrorException> {
                        client.get("/rooms/$objectId?fillJoinInfo=true").body<RoomInfo>()
                    }
                assertEquals(HttpStatusCode.Unauthorized, failure.status)
            }
        }

    @Test
    fun `test notification room is readable but cannot send`() =
        test(mapOf("DISABLE_TEST_WS" to "true")) {
            val sender = attachSession()
            val recipient =
                attachSession {
                    val user =
                        requireNotNull(
                            workerBackend.database.user.getRawUser(ObjectFetch.IdFetch(it.uid)).getOrThrow(),
                        ).user
                    val room =
                        workerBackend.database.room.createRoom(
                            buildUserNotificationRoom(user, sender.uid),
                            buildMemberForNotificationRoom(user, sender.uid),
                        ).getOrThrow()
                    val info = getRoomInfo(room.id).getOrThrow()
                    assertTrue(!info.isJoined)
                    val members = workerBackend.database.container
                    val member = requireNotNull(members.getMember(room.id, it.uid).getOrThrow())
                    assertEquals(MemberStatus.INVITED, member.status)
                    assertEquals(null, member.joinedTime)
                    assertNotNull(member.invitedTime)
                    val creator = requireNotNull(members.getMember(room.id, sender.uid).getOrThrow())
                    assertEquals(MemberStatus.JOINED, creator.status)
                    assertNotNull(creator.joinedTime)
                    assertFails {
                        createTopic(ObjectType.ROOM, room.id, "Notification reply").getOrThrow()
                    }
                    room.id
                }
            assertNotNull(workerBackend.checkRoomWritePermission(recipient.custom, sender.uid).getOrThrow())
        }

    @Test
    fun `get room by id`() =
        test {
            val roomId =
                attachSession {
                    val id = createCommunityForTest().id
                    createPublicRoomForTest(id, "r1", "name1").id
                }.custom
            noneSession {
                assertEquals(1, getRoomInfo(roomId).getOrThrow().memberCount)
            }
        }

    @Test
    fun `get room by aid`() =
        test {
            val roomId =
                attachSession {
                    val id = createCommunityForTest().id
                    createPublicRoomForTest(id, "r1", "name1").id
                }.custom
            noneSession {
                assertEquals(roomId, getRoomInfoByAid("r1").getOrThrow().id)
            }
        }

    @Test
    fun `test joined private room count`() =
        test {
            val firstTuple =
                attachSession {
                    createPrivateRoomForTest().id
                }
            val privateRoomId = firstTuple.custom
            val secondTuple =
                attachSession {
                    assertFails {
                        joinRoom(privateRoomId).getOrThrow()
                    }
                }
            loginSession(firstTuple) {
                createJoinRoomTitleForTest(privateRoomId, secondTuple.uid)
            }
            loginSession(secondTuple) {
                joinRoom(privateRoomId).getOrThrow()
                expectedRoomCountForJoinedRoomList(1)
            }
        }

    @Test
    fun `joining public room updates joined list`() =
        test {
            val (communityId, rooms) = createCommunityRooms()
            val (publicRoom1Id, _) = rooms
            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(publicRoom1Id).getOrThrow()
                expectedRoomCountForJoinedRoomList(1)
            }
        }

    @Test
    fun `joining room preserves community room list`() =
        test {
            val (communityId, rooms) = createCommunityRooms()
            val (publicRoom1Id, _) = rooms
            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(publicRoom1Id).getOrThrow()
                expectedRoomCountForCommunityRoomList(2, communityId, JoinStatusSearch.UNSPECIFIED)
            }
        }

    @Test
    fun `exiting room removes only its membership`() =
        test {
            val (communityId, rooms) = createCommunityRooms()
            val (publicRoom1Id, publicRoom2Id) = rooms
            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(publicRoom1Id).getOrThrow()
                joinRoom(publicRoom2Id).getOrThrow()
                exitRoom(publicRoom1Id).getOrThrow()
                expectedRoomCountForJoinedRoomList(1)
                expectedRoomCountForCommunityRoomList(2, communityId, JoinStatusSearch.UNSPECIFIED)
            }
        }

    @Test
    fun `exiting room twice is idempotent`() =
        test {
            val (communityId, rooms) = createCommunityRooms()
            val (publicRoom1Id, publicRoom2Id) = rooms
            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(publicRoom1Id).getOrThrow()
                joinRoom(publicRoom2Id).getOrThrow()
                exitRoom(publicRoom1Id).getOrThrow()
                exitRoom(publicRoom1Id).getOrThrow()
                expectedRoomCountForJoinedRoomList(1)
            }
        }

    private suspend fun TestMate.createCommunityRooms(): Pair<PrimaryKey, List<PrimaryKey>> =
        attachSession {
            val communityId = createCommunityForTest().id
            val first = createPublicRoomForTest(communityId, "r1", "name1").id
            val second = createPublicRoomForTest(communityId, "r2", "name2").id
            communityId to listOf(first, second)
        }.custom

    @Test
    fun `test private room join`() =
        test {
            val sessionOuterTuple =
                attachSession {
                    createPrivateRoomForTest().id
                }
            val privateRoomId = sessionOuterTuple.custom
            val secondTuple =
                attachSession {
                    assertFails {
                        joinRoom(privateRoomId).getOrThrow()
                    }
                }
            loginSession(sessionOuterTuple) {
                createJoinRoomTitleForTest(privateRoomId, secondTuple.uid)
            }
            loginSession(secondTuple) {
                joinRoom(privateRoomId).getOrThrow()
                assertListSize(2, searchRoomMembers(privateRoomId, null, 10, ""))
            }
        }

    @Test
    fun `test expired private room join title cannot join`() =
        test {
            val user1 =
                attachSession {
                    createPrivateRoomForTest().id
                }
            val privateRoomId = user1.custom

            val user2 =
                attachSession {
                    assertFails {
                        joinRoom(privateRoomId).getOrThrow()
                    }
                }
            loginSession(user1) {
                createTitle(
                    NewTitle(
                        "join-expired",
                        TitleType.JOIN,
                        user2.uid,
                        privateRoomId,
                        ObjectType.ROOM,
                        "expired invite",
                        LocalDateTime.parse("2000-01-01T00:00:00"),
                    ),
                ).getOrThrow()
            }
            loginSession(user2) {
                assertFails {
                    joinRoom(privateRoomId).getOrThrow()
                }
            }
        }

    @Test
    fun `test public room join`() =
        test {
            val sessionOuterTuple =
                attachSession {
                    val communityId = createCommunityForTest().id
                    val publicRoomId = createPublicRoomForTest(communityId, "r1", "name1").id
                    communityId to publicRoomId
                }
            val communityId = sessionOuterTuple.custom.first
            val publicRoomId = sessionOuterTuple.custom.second
            attachSession {
                assertFails {
                    joinRoom(publicRoomId).getOrThrow()
                }
                joinCommunity(communityId).getOrThrow()
                joinRoom(publicRoomId).getOrThrow()
                // 检查幂等
                joinRoom(publicRoomId).getOrThrow()
            }
        }

    @Test
    fun `test update room`() =
        test {
            attachSession {
                val id = createCommunityForTest().id
                val roomId = createPublicRoomForTest(id, "r1", "name1").id
                updateRoomInfo(roomId, UpdateRoomBody("new-name")).getOrThrow()
                assertEquals("new-name", getRoomInfo(roomId).getOrThrow().name)
            }
        }

    @Test
    fun `test rtc`() =
        test {
            val firstUser = attachSession()
            val secondUser =
                attachSession {
                    val roomInfo = createRoom(NewRoom("test-rtc", "rtc")).getOrThrow()
                    createJoinRoomTitleForTest(roomInfo.id, firstUser.uid)
                    roomInfo
                }
            loginSession(firstUser) {
                joinRoom(secondUser.custom.id).getOrThrow()
            }
            val list = CopyOnWriteArrayList<RoomFrame>()
            coroutineScope {
                launch {
                    loginSession(secondUser, { frame, _, session ->
                        list.add(frame)
                        processRTCMessage(frame, session)
                    }) {
                        waitAndSend {
                            sendFrame(RoomFrame.StartCall(secondUser.custom.id))
                        }
                        waitRTCAnswer(list)
                    }
                }
                launch {
                    loginSession(firstUser, { frame, _, session ->
                        list.add(frame)
                        processRTCMessage(frame, session)
                    }) {
                        waitAndSend {
                            sendFrame(RoomFrame.StartCall(secondUser.custom.id))
                        }
                        waitRTCAnswer(list)
                    }
                }
            }
        }

    @Test
    fun `test rtc multi user`() =
        test {
            rtcSession.clear()
            val firstUser = attachSession()
            val thirdUser = attachSession()
            val secondUser =
                attachSession {
                    val roomInfo = createRoom(NewRoom("test-rtc-multi", "rtc-multi")).getOrThrow()
                    createJoinRoomTitleForTest(roomInfo.id, firstUser.uid)
                    createJoinRoomTitleForTest(roomInfo.id, thirdUser.uid)
                    roomInfo
                }
            loginSession(firstUser) {
                joinRoom(secondUser.custom.id).getOrThrow()
            }
            loginSession(thirdUser) {
                joinRoom(secondUser.custom.id).getOrThrow()
            }

            val roomId = secondUser.custom.id
            val list = runRtcCalls(roomId, secondUser, firstUser, thirdUser)

            assertTrue(list.count { it is RoomFrame.CreateAnswer } >= 3)
            assertTrue(list.count { it is RoomFrame.RespondAnswer } >= 3)
        }

    @Test
    fun `test rtc mute state sync`() =
        test {
            rtcSession.clear()
            val firstUser = attachSession()
            val secondUser =
                attachSession {
                    val roomInfo = createRoom(NewRoom("test-rtc-mute", "rtc-mute")).getOrThrow()
                    createJoinRoomTitleForTest(roomInfo.id, firstUser.uid)
                    roomInfo
                }
            loginSession(firstUser) {
                joinRoom(secondUser.custom.id).getOrThrow()
            }

            val roomId = secondUser.custom.id
            val list = CopyOnWriteArrayList<RoomFrame>()
            coroutineScope {
                launch {
                    loginSession(secondUser, { frame, _, session ->
                        list.add(frame)
                        processRTCMessage(frame, session)
                    }) {
                        waitAndSend {
                            sendFrame(RoomFrame.StartCall(roomId))
                        }
                        waitRTCAnswer(list)
                        waitAndSend {
                            sendFrame(RoomFrame.UpdateCallMediaState(roomId, audioMuted = true, videoMuted = false))
                        }
                        waitForRtcFrame(list) { frame ->
                            frame is RoomFrame.PeerMediaState && frame.uid == secondUser.uid && frame.audioMuted
                        }
                    }
                }
                launch {
                    loginSession(firstUser, { frame, _, session ->
                        list.add(frame)
                        processRTCMessage(frame, session)
                    }) {
                        waitAndSend {
                            sendFrame(RoomFrame.StartCall(roomId))
                        }
                        waitForRtcFrame(list) { frame ->
                            frame is RoomFrame.PeerMediaState && frame.uid == secondUser.uid && frame.audioMuted
                        }
                    }
                }
            }
        }
}

private suspend fun TestMate.runRtcCalls(
    roomId: PrimaryKey,
    vararg users: SessionOuterTuple<*>,
): CopyOnWriteArrayList<RoomFrame> {
    val list = CopyOnWriteArrayList<RoomFrame>()
    val answeredPairs = MutableStateFlow(emptySet<Set<PrimaryKey>>())
    val expectedPairs = users.size * (users.size - 1) / 2
    coroutineScope {
        users.map { tuple ->
            async {
                loginSession(tuple, { frame, _, session ->
                    list.add(frame)
                    if (frame is RoomFrame.RespondAnswer && frame.roomId == roomId) {
                        answeredPairs.update { it + setOf(setOf(tuple.uid, frame.uid)) }
                    }
                    processRTCMessage(frame, session)
                }) {
                    waitAndSend {
                        sendFrame(RoomFrame.StartCall(roomId))
                    }
                    // Duplicate answer messages do not imply every peer pair has negotiated.
                    // Keep all sockets alive until the three distinct pairs are complete.
                    withTimeout(30.seconds) { answeredPairs.first { it.size == expectedPairs } }
                }
            }
        }.awaitAll()
    }
    return list
}

suspend fun UserSessionManager.createPrivateRoomForTest(): RoomInfo = createRoom(NewRoom("name", "r3")).getOrThrow()

suspend fun UserSessionManager.createPublicRoomForTest(
    communityId: PrimaryKey,
    roomAid: String,
    roomName: String,
): RoomInfo = createRoom(NewRoom(roomName, roomAid, communityId = communityId)).getOrThrow()

suspend fun UserSessionManager.expectedRoomCountForJoinedRooms(
    expected: Int,
    word: String,
    nextRoomId: String? = null,
) {
    val result = searchCurrentUserRooms(word, 10, nextRoomId)
    assertListSize(expected, result)
}

suspend fun UserSessionManager.expectedRoomCountForAllRooms(
    expected: Int,
    word: String,
    joinStatusSearch: JoinStatusSearch = JoinStatusSearch.UNSPECIFIED,
    nextRoomId: String? = null,
    communityId: PrimaryKey? = null,
) {
    val result =
        if (communityId != null) {
            // 公开聊天室通过 communities/{id}/rooms/search 查询
            searchCommunityRooms(communityId, word, joinStatusSearch, 10, nextRoomId)
        } else {
            // 搜索本人已加入的房间使用 users/joined-rooms/search
            searchCurrentUserRooms(word, 10, nextRoomId)
        }
    assertListSize(expected, result)
}

suspend fun UserSessionManager.expectedRoomCountForJoinedRoomList(expected: Int, nextRoomId: String? = null) {
    val result = getUserRooms(PaginationQuery(nextRoomId, size = 10))
    assertListSize(expected, result)
}

/**
 * 用于搜索社区中的所有房间（不带关键词）.
 */
suspend fun UserSessionManager.expectedRoomCountForCommunityRoomList(
    expected: Int,
    communityId: PrimaryKey,
    joinStatusSearch: JoinStatusSearch = JoinStatusSearch.UNSPECIFIED,
    nextRoomId: String? = null,
) {
    // 公开聊天室通过 communities/{id}/rooms 查询
    val result =
        getCommunityRooms(
            communityId,
            CustomApi.Communities.Id.Rooms.CommunityRoomQuery(nextRoomId, size = 10, joinStatus = joinStatusSearch),
        )
    assertListSize(expected, result)
}

/**
 * 用于搜索当前用户的所有房间（不带关键词）.
 */
suspend fun UserSessionManager.expectedRoomCountForAllRoomList(expected: Int, nextRoomId: String? = null) {
    // 搜索本人已加入的房间使用 users/rooms
    val result = getUserRooms(PaginationQuery(nextRoomId, size = 10))
    assertListSize(expected, result)
}

suspend fun UserSessionManager.createJoinRoomTitleForTest(privateRoomId: PrimaryKey, uid: PrimaryKey): TitleDetail =
    createTitle(
        NewTitle("invite", TitleType.JOIN, uid, privateRoomId, ObjectType.ROOM, "invite for test"),
    ).getOrThrow()

suspend fun waitRTCAnswer(list: MutableList<RoomFrame>) {
    waitRTCAnswerCount(list, 1)
}

suspend fun waitRTCAnswerCount(list: MutableList<RoomFrame>, expectedCount: Int) {
    waitForRtcFrame(list) {
        list.count { frame -> frame is RoomFrame.RespondAnswer } >= expectedCount
    }
}

suspend fun waitForRtcFrame(list: MutableList<RoomFrame>, predicate: (RoomFrame) -> Boolean) {
    withTimeout(30.seconds) {
        while (!list.any(predicate)) {
            delay(200.milliseconds)
        }
    }
    assertTrue(list.any(predicate), "RTC frame not received within 30s, received: $list")
}

suspend fun processRTCMessage(frame: RoomFrame, session: DefaultClientWebSocketSession) {
    if (frame is RoomFrame.CreateOffer) {
        session.sendFrame(RoomFrame.SendOffer(CustomOffer("offer"), frame.roomId, frame.targetUid))
    } else if (frame is RoomFrame.CreateAnswer) {
        session.sendFrame(RoomFrame.SendAnswer(CustomAnswer("answer"), frame.roomId, frame.targetUid))
    }
}
