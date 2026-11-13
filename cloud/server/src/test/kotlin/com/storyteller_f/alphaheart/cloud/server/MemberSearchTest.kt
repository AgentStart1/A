/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.exitCommunity
import com.storyteller_f.alphaheart.client.core.exitRoom
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.searchCommunityMembers
import com.storyteller_f.alphaheart.client.core.searchRoomMembers
import com.storyteller_f.alphaheart.client.core.updateUserInfo
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.UpdateUserBody
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 测试三种成员搜索方式：
 * 1. 无关键字搜索（直接从数据库查询）
 * 2. 有关键字 + 有 objectId（使用 MemberSearchService）
 * 3. 只有关键字（使用 UserSearchService）
 */
class MemberSearchTest {
    @Test
    fun `test private room member search without keyword`() =
        test {
            val firstUser =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Alice")).getOrThrow()
                    val privateRoomId = createPrivateRoomForTest().id
                    privateRoomId
                }
            val privateRoomId = firstUser.custom

            // 第二个用户加入私有房间
            val secondUser =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Bob")).getOrThrow()
                }

            // 创建邀请标题让第二个用户加入私有房间
            loginSession(firstUser) {
                createJoinRoomTitleForTest(privateRoomId, secondUser.uid)
            }

            loginSession(secondUser) {
                joinRoom(privateRoomId).getOrThrow()
            }

            // 第三个用户加入私有房间
            val thirdUser =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Charlie")).getOrThrow()
                }

            // 创建邀请标题让第三个用户加入私有房间
            loginSession(firstUser) {
                createJoinRoomTitleForTest(privateRoomId, thirdUser.uid)
            }

            loginSession(thirdUser) {
                joinRoom(privateRoomId).getOrThrow()
            }

            // 测试无关键字搜索 - 应该返回所有成员
            loginSession(thirdUser) {
                val members = searchRoomMembers(privateRoomId, null, 10, "").getOrThrow()
                assertEquals(3, members.data.size, "Should have 3 members in private room without keyword search")
            }
        }

    @Test
    fun `search private room members by keyword and id`() =
        test {
            val firstUser =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Alice")).getOrThrow()
                    val privateRoomId = createPrivateRoomForTest().id
                    privateRoomId
                }
            val privateRoomId = firstUser.custom

            // 第二个用户设置昵称并加入私有房间
            val secondUser =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Bob")).getOrThrow()
                }

            // 创建邀请标题让第二个用户加入私有房间
            loginSession(firstUser) {
                createJoinRoomTitleForTest(privateRoomId, secondUser.uid)
            }

            loginSession(secondUser) {
                joinRoom(privateRoomId).getOrThrow()
            }

            // 第三个用户设置昵称并加入私有房间
            val thirdUser =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Charlie")).getOrThrow()
                }

            // 创建邀请标题让第三个用户加入私有房间
            loginSession(firstUser) {
                createJoinRoomTitleForTest(privateRoomId, thirdUser.uid)
            }

            loginSession(thirdUser) {
                joinRoom(privateRoomId).getOrThrow()
            }

            // 测试使用 MemberSearchService - 在私有房间搜索包含 "Bob" 的成员
            loginSession(thirdUser) {
                val members = searchRoomMembers(privateRoomId, null, 10, "Bob").getOrThrow()
                assertTrue(members.data.isNotEmpty(), "Should find members with keyword 'Bob' in private room")
                assertTrue(
                    members.data.any { it.userInfo.nickname.contains("Bob") },
                    "Should contain member with nickname 'Bob' in private room",
                )
            }
        }

    @Test
    fun `test room member search without keyword`() =
        test {
            val result =
                attachSession {
                    val communityId = createCommunityForTest().id
                    val roomId = createPublicRoomForTest(communityId, "r1", "room1").id
                    communityId to roomId
                }
            val communityId = result.custom.first
            val roomId = result.custom.second

            // 第二个用户加入
            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(roomId).getOrThrow()
            }

            // 第三个用户加入
            attachSession {
                joinCommunity(communityId).getOrThrow()
                joinRoom(roomId).getOrThrow()
            }

            // 测试无关键字搜索 - 应该返回所有成员
            noneSession {
                val members = searchRoomMembers(roomId, null, 10, "").getOrThrow()
                assertEquals(3, members.data.size, "Should have 3 members without keyword search")
            }
        }

    @Test
    fun `test room member search with keyword and objectId`() =
        test {
            val result =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Alice")).getOrThrow()
                    val communityId = createCommunityForTest().id
                    val roomId = createPublicRoomForTest(communityId, "r1", "room1").id
                    communityId to roomId
                }
            val communityId = result.custom.first
            val roomId = result.custom.second

            // 第二个用户设置昵称并加入
            attachSession {
                updateUserInfo(UpdateUserBody(nickname = "Bob")).getOrThrow()
                joinCommunity(communityId).getOrThrow()
                joinRoom(roomId).getOrThrow()
            }

            // 第三个用户设置昵称并加入
            attachSession {
                updateUserInfo(UpdateUserBody(nickname = "Charlie")).getOrThrow()
                joinCommunity(communityId).getOrThrow()
                joinRoom(roomId).getOrThrow()
            }

            // 测试使用 MemberSearchService - 在特定房间搜索包含 "Bob" 的成员
            noneSession {
                val members = searchRoomMembers(roomId, null, 10, "Bob").getOrThrow()
                assertTrue(members.data.isNotEmpty(), "Should find members with keyword 'Bob'")
                assertTrue(
                    members.data.any { it.userInfo.nickname.contains("Bob") },
                    "Should contain member with nickname 'Bob'",
                )
            }
        }

    @Test
    fun `test community member search without keyword`() =
        test {
            val communityId =
                attachSession {
                    createCommunity(NewCommunity("community1", "c1")).getOrThrow().id
                }.custom

            // 第二个用户加入
            attachSession {
                joinCommunity(communityId).getOrThrow()
            }

            // 第三个用户加入
            attachSession {
                joinCommunity(communityId).getOrThrow()
            }

            // 测试无关键字搜索 - 应该返回所有成员
            noneSession {
                val members = searchCommunityMembers(communityId, null, 10, "").getOrThrow()
                assertEquals(3, members.data.size, "Should have 3 members without keyword search")
            }
        }

    @Test
    fun `search community members by keyword and id`() =
        test {
            val communityId =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "David")).getOrThrow()
                    createCommunity(NewCommunity("community1", "c1")).getOrThrow().id
                }.custom

            // 第二个用户设置昵称并加入
            attachSession {
                updateUserInfo(UpdateUserBody(nickname = "Eve")).getOrThrow()
                joinCommunity(communityId).getOrThrow()
            }

            // 第三个用户设置昵称并加入
            attachSession {
                updateUserInfo(UpdateUserBody(nickname = "Frank")).getOrThrow()
                joinCommunity(communityId).getOrThrow()
            }

            // 测试使用 MemberSearchService - 在特定社区搜索包含 "Eve" 的成员
            noneSession {
                val members = searchCommunityMembers(communityId, null, 10, "Eve").getOrThrow()
                assertTrue(members.data.isNotEmpty(), "Should find members with keyword 'Eve'")
                assertTrue(
                    members.data.any { it.userInfo.nickname.contains("Eve") },
                    "Should contain member with nickname 'Eve'",
                )
            }
        }

    @Test
    fun `test member search after exit room`() =
        test {
            val result =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Grace")).getOrThrow()
                    val communityId = createCommunityForTest().id
                    val roomId = createPublicRoomForTest(communityId, "r1", "room1").id
                    communityId to roomId
                }
            val communityId = result.custom.first
            val roomId = result.custom.second

            // 第二个用户加入并退出
            val secondTuple =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Henry")).getOrThrow()
                    joinCommunity(communityId).getOrThrow()
                    joinRoom(roomId).getOrThrow()
                }

            // 退出前应该能搜索到
            noneSession {
                val membersBeforeExit = searchRoomMembers(roomId, null, 10, "Henry").getOrThrow()
                assertTrue(
                    membersBeforeExit.data.any { it.userInfo.nickname.contains("Henry") },
                    "Should find Henry before exit",
                )
            }

            // 退出房间
            loginSession(secondTuple) {
                exitRoom(roomId).getOrThrow()
            }

            // 退出后不应该搜索到
            noneSession {
                val membersAfterExit = searchRoomMembers(roomId, null, 10, "Henry").getOrThrow()
                assertTrue(
                    membersAfterExit.data.none { it.userInfo.nickname.contains("Henry") },
                    "Should not find Henry after exit",
                )
            }
        }

    @Test
    fun `test member search after exit private room`() =
        test {
            val firstUser =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Alice")).getOrThrow()
                    val privateRoomId = createPrivateRoomForTest().id
                    privateRoomId
                }
            val privateRoomId = firstUser.custom

            // 第二个用户加入私有房间
            val secondUser =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Bob")).getOrThrow()
                }

            // 创建邀请标题让第二个用户加入私有房间
            loginSession(firstUser) {
                createJoinRoomTitleForTest(privateRoomId, secondUser.uid)
            }

            loginSession(secondUser) {
                joinRoom(privateRoomId).getOrThrow()
            }

            // 退出前应该能搜索到
            loginSession(firstUser) {
                val membersBeforeExit = searchRoomMembers(privateRoomId, null, 10, "Bob").getOrThrow()
                assertTrue(
                    membersBeforeExit.data.any { it.userInfo.nickname.contains("Bob") },
                    "Should find Bob before exit from private room",
                )
            }

            // 退出私有房间
            loginSession(secondUser) {
                exitRoom(privateRoomId).getOrThrow()
            }

            // 退出后不应该搜索到
            loginSession(firstUser) {
                val membersAfterExit = searchRoomMembers(privateRoomId, null, 10, "Bob").getOrThrow()
                assertTrue(
                    membersAfterExit.data.none { it.userInfo.nickname.contains("Bob") },
                    "Should not find Bob after exit from private room",
                )
            }
        }

    @Test
    fun `test member search after exit community`() =
        test {
            val communityId =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Iris")).getOrThrow()
                    createCommunity(NewCommunity("community1", "c1")).getOrThrow().id
                }.custom

            // 第二个用户加入并退出
            val secondTuple =
                attachSession {
                    updateUserInfo(UpdateUserBody(nickname = "Jack")).getOrThrow()
                    joinCommunity(communityId).getOrThrow()
                }

            // 退出前应该能搜索到
            noneSession {
                val membersBeforeExit = searchCommunityMembers(communityId, null, 10, "Jack").getOrThrow()
                assertTrue(
                    membersBeforeExit.data.any { it.userInfo.nickname.contains("Jack") },
                    "Should find Jack before exit",
                )
            }

            // 退出社区
            loginSession(secondTuple) {
                exitCommunity(communityId).getOrThrow()
            }

            // 退出后不应该搜索到
            noneSession {
                val membersAfterExit = searchCommunityMembers(communityId, null, 10, "Jack").getOrThrow()
                assertTrue(
                    membersAfterExit.data.none { it.userInfo.nickname.contains("Jack") },
                    "Should not find Jack after exit",
                )
            }
        }
}
