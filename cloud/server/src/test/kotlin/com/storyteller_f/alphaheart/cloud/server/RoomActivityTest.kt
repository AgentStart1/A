/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.RoomActivityInfo
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.backend.exposed.database.ExposedTopicDatabase
import com.storyteller_f.alphaheart.backend.exposed.tables.Topics
import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.addReadLog
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.exitCommunity
import com.storyteller_f.alphaheart.client.core.exitRoom
import com.storyteller_f.alphaheart.client.core.getCommunityRooms
import com.storyteller_f.alphaheart.client.core.getJoinedPinnedTopics
import com.storyteller_f.alphaheart.client.core.getRoomMembersPublicKeys
import com.storyteller_f.alphaheart.client.core.getRoomTopics
import com.storyteller_f.alphaheart.client.core.getUnreadMessageCount
import com.storyteller_f.alphaheart.client.core.getUnreadRoomActivities
import com.storyteller_f.alphaheart.client.core.getUserRooms
import com.storyteller_f.alphaheart.client.core.hasUnreadRooms
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.pinTopic
import com.storyteller_f.alphaheart.client.core.searchCommunityRooms
import com.storyteller_f.alphaheart.client.core.searchCurrentUserRooms
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.alphaheart.client.core.unpinTopic
import com.storyteller_f.alphaheart.cloud.core.service.createTopicAtRoom
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicCreated
import com.storyteller_f.shared.obj.NewRoomTopic
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.obj.UpdateUserRead
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomActivityTest {
    @Test
    fun `room pages count direct messages in page order`() =
        test {
            attachSession { account ->
                val community = createCommunityForTest()
                val room = createPublicRoomForTest(community.id, "active", "Active")
                val empty = createPublicRoomForTest(community.id, "empty", "Empty")
                val first = createRoomMessage(account.uid, room.id, "First")
                val second = createRoomMessage(account.uid, room.id, "Second")
                createRoomMessage(account.uid, second.id, "Reply excluded", ObjectType.TOPIC)
                val initial = getUserRooms(PaginationQuery()).getOrThrow().activities
                val latest = assertNotNull(initial.last().latestMessage)
                assertEquals(listOf(empty.id, room.id), initial.map { it.room.id })
                assertNull(initial.first().latestMessage)
                assertEquals(0L, initial.first().unreadMessageCount)
                assertEquals(2L, initial.last().unreadMessageCount)
                assertEquals(second.id, initial.last().latestMessageId)
                assertEquals(
                    getRoomTopics(
                        room.id,
                        paginationQuery = PaginationQuery(),
                    ).getOrThrow().data.first { it.id == second.id }.createdTime,
                    latest.createdTime,
                )
                assertEquals(second.author, latest.authorInfo?.id)
                assertEquals("Second", assertIs<TopicContent.Plain>(latest.content).plain)
                workerBackend.database.room.updateRoomStatus(room.id, ObjectStatus.READ_ONLY).getOrThrow()
                assertEquals(
                    ObjectStatus.READ_ONLY,
                    getOnlyRoomActivity(room.id).room.status,
                )
                addReadLog(UpdateUserRead(room.tuple(), first.id)).getOrThrow()
                assertEquals(1L, getOnlyRoomActivity(room.id).unreadMessageCount)
                addReadLog(UpdateUserRead(room.tuple(), second.id)).getOrThrow()
                assertEquals(0L, getOnlyRoomActivity(room.id).unreadMessageCount)
                assertEquals(0, hasUnreadRooms().getOrThrow().unreadCount)
                addReadLog(UpdateUserRead(room.tuple(), first.id)).getOrThrow()
                assertEquals(second.id, getOnlyRoomActivity(room.id).room.lastRead)
                assertEquals(0L, getOnlyRoomActivity(room.id).unreadMessageCount)
                assertEquals(0, hasUnreadRooms().getOrThrow().unreadCount)
                exitRoom(room.id).getOrThrow()
                assertTrue(getUserRooms(PaginationQuery()).getOrThrow().activities.none { it.room.id == room.id })
            }
        }

    @Test
    fun `list activity and totals cover unloaded rooms`() =
        test {
            attachSession { account ->
                val community = createCommunityForTest()
                val rooms = (1..105).map { createPublicRoomForTest(community.id, "page$it", "Room $it") }
                val messages = rooms.map { createRoomMessage(account.uid, it.id, "Page message") }
                createRoomMessage(account.uid, rooms.first().id, "Second direct message")
                createRoomMessage(account.uid, messages.first().id, "Excluded reply", ObjectType.TOPIC)
                assertEquals(106L, getUnreadMessageCount().getOrThrow().unreadMessageCount)
                val first = getUserRooms(PaginationQuery(size = 100)).getOrThrow()
                val second = getUserRooms(PaginationQuery(first.pagination?.nextPageToken, size = 100)).getOrThrow()
                assertEquals(100, first.data.size)
                assertEquals(5, second.data.size)
                listOf(first, second).forEach { page ->
                    assertEquals(page.data.map { it.id }, page.activities.map { it.room.id })
                    assertTrue(page.activities.all { it.latestMessage != null && it.unreadMessageCount > 0 })
                }
                assertEquals(105, hasUnreadRooms().getOrThrow().unreadCount)
                val communityPage =
                    getCommunityRooms(
                        community.id,
                        CustomApi.Communities.Id.Rooms.CommunityRoomQuery(size = 3),
                    ).getOrThrow()
                assertEquals(communityPage.data.map { it.id }, communityPage.activities.map { it.room.id })
                val search = searchCurrentUserRooms("Room", 3, null).getOrThrow()
                val communitySearch =
                    searchCommunityRooms(
                        community.id,
                        "Room",
                        JoinStatusSearch.JOINED,
                        3,
                        null,
                    ).getOrThrow()
                listOf(search, communitySearch).forEach { page ->
                    assertEquals(3, page.activities.size)
                    assertEquals(page.data.map { it.id }, page.activities.map { it.room.id })
                }
                val read = first.activities.first()
                addReadLog(UpdateUserRead(read.room.tuple(), requireNotNull(read.latestMessageId))).getOrThrow()
                assertEquals(106L - read.unreadMessageCount, getUnreadMessageCount().getOrThrow().unreadMessageCount)
                exitRoom(rooms.first().id).getOrThrow()
                assertEquals(103L, getUnreadMessageCount().getOrThrow().unreadMessageCount)
            }
        }

    @Test
    fun `unread pages contain each room once`() =
        test {
            attachSession { account ->
                val community = createCommunityForTest()
                val rooms = (1..3).map { createPublicRoomForTest(community.id, "r$it", "Room $it") }
                repeat(3) { index -> rooms.forEach { createRoomMessage(account.uid, it.id, "Message $index") } }
                val first = getUnreadRoomActivities(PaginationQuery(size = 1)).getOrThrow()
                val second =
                    getUnreadRoomActivities(
                        PaginationQuery(first.pagination?.nextPageToken, size = 1),
                    ).getOrThrow()
                val third =
                    getUnreadRoomActivities(
                        PaginationQuery(second.pagination?.nextPageToken, size = 1),
                    ).getOrThrow()
                val last =
                    getUnreadRoomActivities(
                        PaginationQuery(third.pagination?.nextPageToken, size = 1),
                    ).getOrThrow()
                val pages = listOf(first, second, third)
                assertEquals(rooms.reversed().map { it.id }, pages.map { it.data.single().room.id })
                pages.forEach {
                    assertEquals(3L, it.pagination?.total)
                    assertEquals(3L, it.data.single().unreadMessageCount)
                }
                assertTrue(last.data.isEmpty())
                val previous =
                    getUnreadRoomActivities(
                        PaginationQuery(prePageToken = third.pagination?.prePageToken, size = 1),
                    )
                        .getOrThrow()
                assertEquals(second.data.single().room.id, previous.data.single().room.id)
                val previousTwo =
                    getUnreadRoomActivities(
                        PaginationQuery(prePageToken = third.pagination?.prePageToken, size = 2),
                    ).getOrThrow()
                assertEquals(
                    listOf(first.data.single().room.id, second.data.single().room.id),
                    previousTwo.data.map { it.room.id },
                )
                val read = first.data.single()
                addReadLog(UpdateUserRead(read.room.tuple(), requireNotNull(read.latestMessageId))).getOrThrow()
                assertEquals(2L, getUnreadRoomActivities(PaginationQuery()).getOrThrow().pagination?.total)
                assertEquals(2, hasUnreadRooms().getOrThrow().unreadCount)
            }
        }

    @Test
    fun `private activity wire content stays encrypted`() =
        test {
            val owner = preparePrivateActivity()
            loginSession(owner) {
                val wire = client.get("/users/joined-rooms").body<RoomInfoListResponse>()
                assertIs<TopicContent.Encrypted>(wire.activities.single().latestMessage?.content)
            }
        }

    @Test
    fun `private activity decrypts with sender keys`() =
        test {
            val owner = preparePrivateActivity()
            loginSession(owner) {
                val page = getUserRooms(PaginationQuery()).getOrThrow()
                assertIs<TopicContent.Plain>(page.activities.single().latestMessage?.content)
                assertEquals(
                    "Private message",
                    assertIs<TopicContent.Plain>(
                        getOnlyRoomActivity(owner.custom)
                            .latestMessage?.content,
                    ).plain,
                )
            }
        }

    @Test
    fun `private activity is invisible to nonmembers`() =
        test {
            val owner = preparePrivateActivity()
            attachSession {
                assertTrue(getUserRooms(PaginationQuery()).getOrThrow().activities.none { it.room.id == owner.custom })
            }
        }

    @Test
    fun `late member cannot decrypt private history`() =
        test {
            withLatePrivateMember { roomId ->
                val activity = getOnlyRoomActivity(roomId)
                assertEquals(1L, activity.unreadMessageCount)
                assertIs<TopicContent.DecryptFailed>(activity.latestMessage?.content)
            }
        }

    @Test
    fun `exiting private room removes unread activity`() =
        test {
            withLatePrivateMember { roomId ->
                assertEquals(1L, getOnlyRoomActivity(roomId).unreadMessageCount)
                exitRoom(roomId).getOrThrow()
                assertTrue(getUnreadRoomActivities(PaginationQuery()).getOrThrow().data.isEmpty())
            }
        }

    @Test
    fun `anonymous activity request is unauthorized`() =
        test {
            noneSession {
                val error =
                    assertFailsWith<ServerErrorException> {
                        client.get("/users/joined-rooms").body<RoomInfoListResponse>()
                    }
                assertEquals(HttpStatusCode.Unauthorized, error.status)
            }
        }

    @Test
    fun `joined pins paginate newest first`() =
        test {
            withPinnedCommunity { _, old, recent ->
                val page = getJoinedPinnedTopics(PaginationQuery(size = 1)).getOrThrow()
                assertEquals(recent, page.data.single().id)
                assertEquals(
                    old,
                    getJoinedPinnedTopics(PaginationQuery(page.pagination?.nextPageToken, size = 1))
                        .getOrThrow().data.single().id,
                )
            }
        }

    @Test
    fun `unpin removes only the selected joined pin`() =
        test {
            withPinnedCommunity { _, old, recent ->
                unpinTopic(recent).getOrThrow()
                assertEquals(listOf(old), getJoinedPinnedTopics(PaginationQuery()).getOrThrow().data.map { it.id })
            }
        }

    @Test
    fun `joined pins exclude nonmember communities`() =
        test {
            withPinnedCommunity { _, _, _ ->
                attachSession { assertTrue(getJoinedPinnedTopics(PaginationQuery()).getOrThrow().data.isEmpty()) }
            }
        }

    @Test
    fun `exiting community removes its joined pins`() =
        test {
            withPinnedCommunity { communityId, old, recent ->
                assertEquals(
                    setOf(old, recent),
                    getJoinedPinnedTopics(PaginationQuery()).getOrThrow().data.map { it.id }.toSet(),
                )
                exitCommunity(communityId).getOrThrow()
                assertTrue(getJoinedPinnedTopics(PaginationQuery()).getOrThrow().data.isEmpty())
            }
        }

    @Test
    fun `read only accounts retain context read access`() =
        test {
            attachSession { account ->
                val community = createCommunityForTest()
                val room = createPublicRoomForTest(community.id, "readonly", "Read only")
                createRoomMessage(account.uid, room.id, "Message")
                val topic = createTopic(ObjectType.COMMUNITY, community.id, "Pinned").getOrThrow()
                pinTopic(topic.id).getOrThrow()
                workerBackend.database.user.updateUserStatus(account.uid, UserStatus.READ_ONLY).getOrThrow()
                assertEquals(1L, getOnlyRoomActivity(room.id).unreadMessageCount)
                val unread = getUnreadRoomActivities(PaginationQuery()).getOrThrow().data.single()
                assertEquals(room.id, unread.room.id)
                assertEquals(topic.id, getJoinedPinnedTopics(PaginationQuery()).getOrThrow().data.single().id)
            }
        }

    @Test
    fun `deleted messages disappear from activity and count`() =
        test {
            attachSession { account ->
                val community = createCommunityForTest()
                val room = createPublicRoomForTest(community.id, "deletion", "Deletion")
                val first = createRoomMessage(account.uid, room.id, "First")
                val second = createRoomMessage(account.uid, room.id, "Second")
                val database = workerBackend.database.topic as ExposedTopicDatabase
                database.databaseSession.dbQuery { Topics.deleteWhere { Topics.id eq second.id } }.getOrThrow()
                val remaining = getOnlyRoomActivity(room.id)
                assertEquals(first.id, remaining.latestMessageId)
                assertEquals(1L, remaining.unreadMessageCount)
                database.databaseSession.dbQuery { Topics.deleteWhere { Topics.id eq first.id } }.getOrThrow()
                val empty = getOnlyRoomActivity(room.id)
                assertNull(empty.latestMessage)
                assertNull(empty.latestMessageId)
                assertEquals(0L, empty.unreadMessageCount)
                assertTrue(getUnreadRoomActivities(PaginationQuery()).getOrThrow().data.isEmpty())
            }
        }

    @Test
    fun `activity rejects invalid page sizes`() =
        test {
            attachSession {
                assertTrue(getUnreadRoomActivities(PaginationQuery(size = 101)).isFailure)
                assertTrue(getJoinedPinnedTopics(PaginationQuery(size = 0)).isFailure)
                assertFalse(hasUnreadRooms().getOrThrow().hasUnread)
                assertEquals(0L, getUnreadMessageCount().getOrThrow().unreadMessageCount)
            }
        }
}

private suspend fun TestMate.createRoomMessage(
    uid: PrimaryKey,
    parentId: PrimaryKey,
    text: String,
    parentType: ObjectType = ObjectType.ROOM,
): TopicCreated =
    requireNotNull(
        workerBackend.createTopicAtRoom(NewRoomTopic(parentType, parentId, TopicContent.Plain(text)), uid).getOrThrow(),
    )

private suspend fun UserSessionManager.getOnlyRoomActivity(roomId: PrimaryKey): RoomActivityInfo =
    getUserRooms(PaginationQuery()).getOrThrow().activities.single { it.room.id == roomId }

private suspend fun TestMate.preparePrivateActivity(): SessionOuterTuple<Long> {
    val frames = mutableListOf<RoomFrame>()
    return attachSession(onReceive = { frame, _, _ -> frames.add(frame) }) {
        val room = createRoom(NewRoom("private", "Private")).getOrThrow()
        val keys = getRoomMembersPublicKeys(room.id, PaginationQuery()).getOrThrow().data
        createTopicInRoomAndWait(frames) { sendMessage(room.tuple(), true, "Private message", keys) }
        room.id
    }
}

private suspend fun TestMate.withLatePrivateMember(block: suspend UserSessionManager.(Long) -> Unit) {
    val owner = preparePrivateActivity()
    val member = attachSession()
    loginSession(owner) {
        createTitle(NewTitle("join", TitleType.JOIN, member.uid, owner.custom, ObjectType.ROOM, "")).getOrThrow()
    }
    loginSession(member) {
        joinRoom(owner.custom).getOrThrow()
        block(owner.custom)
    }
}

private suspend fun TestMate.withPinnedCommunity(block: suspend UserSessionManager.(Long, Long, Long) -> Unit) {
    attachSession {
        val community = createCommunityForTest()
        val old = createTopic(ObjectType.COMMUNITY, community.id, "Old pin").getOrThrow()
        val recent = createTopic(ObjectType.COMMUNITY, community.id, "Recent pin").getOrThrow()
        pinTopic(old.id).getOrThrow()
        pinTopic(recent.id).getOrThrow()
        block(community.id, old.id, recent.id)
    }
}
