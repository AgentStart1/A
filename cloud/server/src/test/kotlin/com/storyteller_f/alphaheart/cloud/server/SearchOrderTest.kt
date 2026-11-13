/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.searchCommunity
import com.storyteller_f.alphaheart.client.core.searchCommunityRooms
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.type.JoinStatusSearch
import kotlin.test.Test
import kotlin.test.assertEquals

class SearchOrderTest {
    @Test
    fun `test community search order`() =
        test {
            attachSession {
                // 预期顺序: aid 前缀匹配 > name 前缀匹配 > aid 包含匹配 > name 包含匹配
                // 搜索词: "app"
                createCommunityForTest(name = "ignore_name_1", id = "apple") // 1. aid prefix
                createCommunityForTest(name = "application", id = "ignore_aid_1") // 2. name prefix
                createCommunityForTest(name = "ignore_name_2", id = "pineapple") // 3. aid inclusion
                createCommunityForTest(name = "snappy", id = "ignore_aid_2") // 4. name inclusion
            }

            attachSession {
                val result =
                    searchCommunity(
                        size = 10,
                        joinStatusSearch = JoinStatusSearch.UNSPECIFIED,
                        word = "app",
                    ).getOrThrow()

                assertEquals(4, result.data.size)
                assertEquals("apple", result.data[0].communityInfo.aid, "First result should be aid prefix match")
                assertEquals(
                    "application",
                    result.data[1].communityInfo.name,
                    "Second result should be name prefix match",
                )
                assertEquals(
                    "pineapple",
                    result.data[2].communityInfo.aid,
                    "Third result should be aid inclusion match",
                )
                assertEquals(
                    "snappy",
                    result.data[3].communityInfo.name,
                    "Fourth result should be name inclusion match",
                )
            }
        }

    @Test
    fun `test room search order`() =
        test {
            val session =
                attachSession {
                    val communityId = createCommunityForTest(id = "room_test_community").id
                    // 搜索词: "test"
                    createPublicRoomForTest(communityId, "test_room", "ignore_name_1") // 1. aid prefix
                    createPublicRoomForTest(communityId, "ignore_aid_1", "test search") // 2. name prefix
                    createPublicRoomForTest(communityId, "retest", "ignore_name_2") // 3. aid inclusion
                    createPublicRoomForTest(communityId, "ignore_aid_2", "latest test") // 4. name inclusion
                    communityId
                }

            attachSession {
                val communityId = session.custom
                // 加入社区以搜索房间
                joinCommunity(communityId).getOrThrow()

                val result =
                    searchCommunityRooms(
                        communityId = communityId,
                        word = "test",
                        joinStatusSearch = JoinStatusSearch.UNSPECIFIED,
                        size = 10,
                        nextRoomId = null,
                    ).getOrThrow()

                assertEquals(4, result.data.size)
                assertEquals("test_room", result.data[0].aid)
                assertEquals("test search", result.data[1].name)
                assertEquals("retest", result.data[2].aid)
                assertEquals("latest test", result.data[3].name)
            }
        }
}
