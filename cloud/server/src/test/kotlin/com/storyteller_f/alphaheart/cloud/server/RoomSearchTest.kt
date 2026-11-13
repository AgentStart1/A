/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.service.MemberDocumentSearch
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.getCommunityRooms
import com.storyteller_f.alphaheart.client.core.getUserRooms
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.searchCommunityRooms
import com.storyteller_f.alphaheart.client.core.searchCurrentUserRooms
import com.storyteller_f.alphaheart.client.core.updateRoomInfo
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class RoomSearchTest {
    @Test
    fun `paged search preserves index order after rename`() =
        test {
            attachSession { account ->
                val community = createCommunityForTest().id
                val rooms =
                    (0 until 65).map { index ->
                        createPublicRoomForTest(community, "orderroom$index", "orderfixture ${index * 37 % 67}")
                    }
                updateRoomInfo(rooms[60].id, UpdateRoomBody(name = "orderfixture renamed")).getOrThrow()
                val indexed =
                    workerBackend.memberSearchService.searchDocument(
                        MemberDocumentSearch.RoomMembers(account.uid, "orderfixture", fetch = OffsetFetch(null, 100)),
                    ).getOrThrow().list.map { it.objectId }
                val full = searchCurrentUserRooms("orderfixture", 100, null).getOrThrow().data.map { it.id }
                assertEquals(65, full.distinct().size)
                assertEquals(indexed, full, "SQL hydration must retain search hit order")
                assertEquals(full, collectRoomSearchPages("orderfixture"))
            }
        }

    @Test
    fun `renaming room updates joined and global search`() =
        test {
            attachSession {
                val owner = this
                val community = createCommunityForTest().id
                val room = createPublicRoomForTest(community, "renamedroom", "beforetoken room").id
                attachSession {
                    joinCommunity(community).getOrThrow()
                    joinRoom(room).getOrThrow()
                    val original = searchCurrentUserRooms("beforetoken", 10, null).getOrThrow().data
                    assertEquals(listOf(room), original.map { it.id })
                    owner.updateRoomInfo(room, UpdateRoomBody(name = "aftertoken room")).getOrThrow()
                    assertEquals(emptyList(), searchCurrentUserRooms("beforetoken", 10, null).getOrThrow().data)
                    val renamed = searchCurrentUserRooms("aftertoken", 10, null).getOrThrow().data
                    assertEquals(listOf(room), renamed.map { it.id })
                }
                expectedCurrentUserRoomCount(0, "beforetoken")
                expectedCurrentUserRoomCount(1, "aftertoken")
                expectedCommunityRoomCount(0, community, "beforetoken")
                expectedCommunityRoomCount(1, community, "aftertoken")
                attachSession { expectedCurrentUserRoomCount(0, "aftertoken") }
            }
        }

    @Test
    fun `test search joined rooms by name`() =
        test {
            val sessionOuterTuple =
                attachSession {
                    val communityId = createCommunityForTest().id
                    val room1Id = createPublicRoomForTest(communityId, "r1", "game room").id
                    val room2Id = createPublicRoomForTest(communityId, "r2", "study room").id
                    val room3Id = createPublicRoomForTest(communityId, "r3", "chat room").id
                    communityId to listOf(room1Id, room2Id, room3Id)
                }
            val communityId = sessionOuterTuple.custom.first
            val (room1Id, room2Id, room3Id) = sessionOuterTuple.custom.second

            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(room1Id).getOrThrow()
                joinRoom(room2Id).getOrThrow()
                joinRoom(room3Id).getOrThrow()

                // 测试搜索所有已加入的房间
                expectedRoomCountForJoinedRoomList(3)

                // 测试按名称搜索已加入的房间
                expectedRoomCountForJoinedRooms(1, "game")
                expectedRoomCountForJoinedRooms(1, "study")
                expectedRoomCountForJoinedRooms(1, "chat")

                // 测试部分匹配
                expectedRoomCountForJoinedRooms(3, "room")
            }
        }

    @Test
    fun `test search rooms with member search service`() =
        test {
            val sessionOuterTuple =
                attachSession {
                    val communityId = createCommunityForTest().id
                    val room1Id = createPublicRoomForTest(communityId, "r1", "dragon ball").id
                    val room2Id = createPublicRoomForTest(communityId, "r2", "one piece").id
                    val room3Id = createPublicRoomForTest(communityId, "r3", "naruto").id
                    communityId to listOf(room1Id, room2Id, room3Id)
                }
            val communityId = sessionOuterTuple.custom.first
            val (room1Id, room2Id, room3Id) = sessionOuterTuple.custom.second

            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(room1Id).getOrThrow()
                joinRoom(room2Id).getOrThrow()

                // 测试搜索已加入的房间，使用memberSearchService
                expectedRoomCountForJoinedRoomList(2)
                expectedRoomCountForJoinedRooms(1, "dragon")
                expectedRoomCountForJoinedRooms(1, "piece")

                // 测试未加入的房间不会出现在结果中
                expectedRoomCountForJoinedRooms(0, "naruto")

                // 加入第三个房间后再搜索
                joinRoom(room3Id).getOrThrow()
                expectedRoomCountForJoinedRooms(1, "naruto")
                expectedRoomCountForJoinedRoomList(3)
            }
        }

    @Test
    fun `test private room cannot be searched unless joined`() =
        test {
            val sessionOuterTuple =
                attachSession {
                    val privateRoomId = createPrivateRoomForTest().id
                    expectedRoomCountForJoinedRoomList(1)
                    expectedRoomCountForJoinedRooms(1, "name")
                    privateRoomId
                }
            val privateRoomId = sessionOuterTuple.custom

            // 测试未加入的私有房间不应出现在搜索结果中
            val secondTuple =
                attachSession {
                    // 搜索所有房间（不指定社区），私有房间不应出现
                    expectedRoomCountForAllRoomList(0)

                    // 尝试通过名称搜索私有房间，也不应出现（使用roomSearchService）
                    expectedRoomCountForAllRooms(0, "name")

                    expectedRoomCountForJoinedRoomList(0)
                    expectedRoomCountForJoinedRooms(0, "name")

                    // 即使使用 UNSPECIFIED 搜索，未加入的私有房间也不应出现
                    expectedRoomCountForAllRooms(0, "name")
                }

            loginSession(sessionOuterTuple) {
                createJoinRoomTitleForTest(privateRoomId, secondTuple.uid)
            }

            loginSession(secondTuple) {
                joinRoom(privateRoomId).getOrThrow()
                expectedRoomCountForJoinedRoomList(1)
                expectedRoomCountForJoinedRooms(1, "name")
            }
        }

    @Test
    fun `test search rooms in community`() =
        test {
            val sessionOuterTuple =
                attachSession {
                    val communityId = createCommunityForTest().id
                    val room1Id = createPublicRoomForTest(communityId, "r1", "game room").id
                    val room2Id = createPublicRoomForTest(communityId, "r2", "study room").id
                    val room3Id = createPublicRoomForTest(communityId, "r3", "chat room").id
                    communityId to listOf(room1Id, room2Id, room3Id)
                }
            val communityId = sessionOuterTuple.custom.first
            val (room1Id, room2Id, room3Id) = sessionOuterTuple.custom.second

            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(room1Id).getOrThrow()
                joinRoom(room2Id).getOrThrow()

                // 测试搜索社区中的所有房间
                expectedCommunityRoomCount(3, communityId)

                // 测试按名称搜索社区中的房间
                expectedCommunityRoomCount(1, communityId, word = "game")
                expectedCommunityRoomCount(1, communityId, word = "study")
                expectedCommunityRoomCount(1, communityId, word = "chat") // 未加入的房间不应出现

                // 测试搜索已加入的房间
                expectedCommunityRoomCount(2, communityId, joinStatusSearch = JoinStatusSearch.JOINED)
                expectedCommunityRoomCount(1, communityId, word = "game", joinStatusSearch = JoinStatusSearch.JOINED)
                expectedCommunityRoomCount(1, communityId, word = "study", joinStatusSearch = JoinStatusSearch.JOINED)
                expectedCommunityRoomCount(0, communityId, word = "chat", joinStatusSearch = JoinStatusSearch.JOINED)

                // 加入第三个房间后再搜索
                joinRoom(room3Id).getOrThrow()
                expectedCommunityRoomCount(3, communityId, joinStatusSearch = JoinStatusSearch.JOINED)
                expectedCommunityRoomCount(1, communityId, word = "chat", joinStatusSearch = JoinStatusSearch.JOINED)
            }
        }

    @Test
    fun `joined room list excludes unjoined rooms`() =
        test {
            withJoinedRoomSearch { expectedCurrentUserRoomCount(2) }
        }

    @Test
    fun `joined room keyword search excludes unjoined rooms`() =
        test {
            withJoinedRoomSearch {
                expectedCurrentUserRoomCount(1, word = "alice")
                expectedCurrentUserRoomCount(1, word = "bob")
                expectedCurrentUserRoomCount(0, word = "charlie")
            }
        }

    @Test
    fun `joining a room updates list and keyword visibility`() =
        test {
            withJoinedRoomSearch { thirdRoom ->
                expectedCurrentUserRoomCount(2)
                expectedCurrentUserRoomCount(0, word = "charlie")
                joinRoom(thirdRoom).getOrThrow()
                expectedCurrentUserRoomCount(3)
                expectedCurrentUserRoomCount(1, word = "charlie")
            }
        }
}

suspend fun UserSessionManager.expectedCommunityRoomCount(
    expected: Int,
    communityId: PrimaryKey,
    word: String? = null,
    joinStatusSearch: JoinStatusSearch = JoinStatusSearch.UNSPECIFIED,
    nextRoomId: String? = null,
    size: Int = 10,
) {
    val result =
        if (word != null) {
            searchCommunityRooms(communityId, word, joinStatusSearch, size, nextRoomId)
        } else {
            getCommunityRooms(
                communityId,
                CustomApi.Communities.Id.Rooms.CommunityRoomQuery(
                    nextRoomId,
                    size = size,
                    joinStatus = joinStatusSearch,
                ),
            )
        }
    assertListSize(expected, result)
}

suspend fun UserSessionManager.expectedCurrentUserRoomCount(
    expected: Int,
    word: String? = null,
    nextRoomId: String? = null,
    size: Int = 10,
) {
    val result =
        if (word != null) {
            searchCurrentUserRooms(word, size, nextRoomId)
        } else {
            getUserRooms(PaginationQuery(nextRoomId, size = size))
        }
    assertListSize(expected, result)
}

private suspend fun TestMate.withJoinedRoomSearch(block: suspend UserSessionManager.(Long) -> Unit) {
    val owner =
        attachSession {
            val communityId = createCommunityForTest().id
            val rooms =
                listOf("alice", "bob", "charlie").mapIndexed { index, name ->
                    createPublicRoomForTest(communityId, "r${index + 1}", "$name room").id
                }
            communityId to rooms
        }
    attachSession {
        joinCommunity(owner.custom.first).getOrThrow()
        owner.custom.second.take(2).forEach { joinRoom(it).getOrThrow() }
        block(owner.custom.second.last())
    }
}

private suspend fun UserSessionManager.collectRoomSearchPages(word: String): List<PrimaryKey> =
    withTimeout(30_000) {
        val ids = mutableListOf<PrimaryKey>()
        val tokens = mutableSetOf<String>()
        var token: String? = null
        do {
            val page = searchCurrentUserRooms(word, 20, token).getOrThrow()
            ids.addAll(page.data.map { it.id })
            token = page.pagination?.nextPageToken
            token?.let { check(tokens.add(it)) { "Repeated room search token" } }
        } while (token != null)
        ids
    }
