/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.NewTopic
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.exitCommunity
import com.storyteller_f.alphaheart.client.core.getCommunityInfo
import com.storyteller_f.alphaheart.client.core.getCommunityInfoByAid
import com.storyteller_f.alphaheart.client.core.getCommunityRooms
import com.storyteller_f.alphaheart.client.core.getCommunityTopics
import com.storyteller_f.alphaheart.client.core.getTopicInfo
import com.storyteller_f.alphaheart.client.core.getUserCommunities
import com.storyteller_f.alphaheart.client.core.getUserJoinedCommunities
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.searchCommunityTopics
import com.storyteller_f.alphaheart.client.core.updateCommunityInfo
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.FontSettings
import com.storyteller_f.shared.model.MemberPolicy
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.type.ObjectType
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CommunityTest {
    @Test
    fun `anonymous joined community detail requires login`() =
        test(mapOf("DISABLE_TEST_WS" to "true")) {
            val objectId = attachSession { createCommunityForTest().id }.custom
            noneSession {
                val failure =
                    assertFailsWith<ServerErrorException> {
                        client.get("/communities/$objectId?fillJoinInfo=true").body<CommunityInfo>()
                    }
                assertEquals(HttpStatusCode.Unauthorized, failure.status)
            }
        }

    @Test
    fun `get community by id`() =
        test {
            val newId =
                attachSession {
                    createCommunityForTest().id
                }.custom
            noneSession {
                val community = getCommunityInfo(newId).getOrThrow()
                assertEquals(1, community.memberCount)
            }
        }

    @Test
    fun `get community by aid`() =
        test {
            val newId =
                attachSession {
                    createCommunityForTest().id
                }.custom
            noneSession {
                val community = getCommunityInfo(newId).getOrThrow()
                assertEquals(newId, getCommunityInfoByAid(community.aid).getOrThrow().id)
            }
        }

    @Test
    fun `nonmember cannot publish community topic`() =
        test {
            val communityId =
                attachSession {
                    createCommunityForTest().id
                }.custom
            attachSession {
                assertFails { createTopic(ObjectType.COMMUNITY, communityId, "hello").getOrThrow() }
            }
        }

    @Test
    fun `joining community updates membership`() =
        test {
            val communityId =
                attachSession {
                    createCommunityForTest().id
                }.custom
            attachSession {
                joinCommunity(communityId).getOrThrow()
                assertTrue(getCommunityInfo(communityId).getOrThrow().isJoined)
            }
        }

    @Test
    fun `exiting community clears membership`() =
        test {
            val communityId =
                attachSession {
                    createCommunityForTest().id
                }.custom
            attachSession {
                joinCommunity(communityId).getOrThrow()
                exitCommunity(communityId).getOrThrow()
                assertFalse(getCommunityInfo(communityId).getOrThrow().isJoined)
            }
        }

    @Test
    fun `created topic appears in community lists`() =
        test {
            val communityId =
                attachSession {
                    createCommunityForTest().id
                }.custom
            attachSession {
                joinCommunity(communityId).getOrThrow()
                createTopic(ObjectType.COMMUNITY, communityId, "hello").getOrThrow()
                assertListSize(1, searchCommunityTopics(communityId, 10, "hello"))
                assertListSize(1, getCommunityTopics(communityId, paginationQuery = PaginationQuery(size = 10)))
            }
        }

    @Test
    fun `community comment updates parent metadata`() =
        test {
            val communityId =
                attachSession {
                    createCommunityForTest().id
                }.custom
            attachSession {
                joinCommunity(communityId).getOrThrow()
                val topicId = createTopic(ObjectType.COMMUNITY, communityId, "hello").getOrThrow().id
                val new = createTopic(ObjectType.TOPIC, topicId, "test").getOrThrow()
                assertEquals(ObjectType.COMMUNITY, new.rootType)
                assertEquals(communityId, new.rootId)
                val newInfo = getTopicInfo(topicId).getOrThrow()
                assertTrue(newInfo.hasComment)
            }
        }

    @Test
    fun `test create empty topic in community`() =
        test {
            attachSession {
                val communityId = createCommunityForTest().id
                assertFails {
                    client.post("/topics") {
                        contentType(ContentType.Application.Json)
                        setBody(NewTopic(ObjectType.COMMUNITY, communityId, ""))
                    }
                }
            }
        }

    @Test
    fun `test communities pagination`() =
        test {
            val session =
                attachSession {
                    buildList {
                        repeat(10) {
                            add(createCommunityForTest("c1", "aid$it").id)
                        }
                    }
                }
            attachSession {
                session.custom.forEach {
                    joinCommunity(it).getOrThrow()
                }
                var lastCommunityId: String? = null
                var sum = 0L
                while (true) {
                    val res =
                        getUserCommunities(
                            CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(
                                nextPageToken = lastCommunityId,
                                size = 3,
                            ),
                        ).getOrThrow()
                    val pagination = res.pagination!!
                    lastCommunityId = pagination.nextPageToken
                    sum += res.data.size
                    if (lastCommunityId == null) {
                        assertEquals(pagination.total, sum)
                        break
                    }
                }
            }
        }

    @Test
    fun `anonymous sees target community memberships`() =
        test {
            val id =
                attachSession {
                    repeat(10) {
                        val communityInfo = createCommunityForTest("c$it", "c$it").id
                        joinCommunity(communityInfo).getOrThrow()
                    }
                }.uid
            noneSession {
                val response =
                    getUserJoinedCommunities(
                        id,
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(size = 10),
                    ).getOrThrow()
                assertEquals(10, response.data.size)
                response.data.forEach {
                    assertFalse(it.isJoined)
                    assertNotNull(it.targetMemberInfo)
                }
            }
        }

    @Test
    fun `joined viewer sees target community memberships`() =
        test {
            val id =
                attachSession {
                    repeat(10) {
                        val communityInfo = createCommunityForTest("c$it", "c$it").id
                        joinCommunity(communityInfo).getOrThrow()
                    }
                }.uid
            attachSession {
                val response =
                    getUserJoinedCommunities(
                        id,
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(size = 10),
                    ).getOrThrow()
                response.data.forEach {
                    joinCommunity(it.id).getOrThrow()
                }
                val response2 =
                    getUserJoinedCommunities(
                        id,
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(size = 10),
                    ).getOrThrow()
                response2.data.forEach {
                    assertTrue(it.isJoined)
                    assertNotNull(it.targetMemberInfo)
                }
            }
        }

    @Test
    fun `test community invite only`() =
        test {
            val firstTuple =
                attachSession {
                    createCommunityForTest("name1", "c1", memberPolicy = MemberPolicy.INVITE_ONLY)
                }
            val communityId = firstTuple.custom.id
            val secondTuple =
                attachSession {
                    assertFails {
                        joinCommunity(communityId).getOrThrow()
                    }
                }
            loginSession(firstTuple) {
                createTitle(
                    NewTitle("join", TitleType.JOIN, secondTuple.uid, communityId, ObjectType.COMMUNITY, "join"),
                ).getOrThrow()
            }
            loginSession(secondTuple) {
                joinCommunity(communityId).getOrThrow()
            }
        }

    @Test
    fun `test expired community invite title cannot join`() =
        test {
            val firstTuple =
                attachSession {
                    createCommunityForTest("name-expired", "c-expired", memberPolicy = MemberPolicy.INVITE_ONLY)
                }
            val communityId = firstTuple.custom.id
            val secondTuple =
                attachSession {
                    assertFails {
                        joinCommunity(communityId).getOrThrow()
                    }
                }
            loginSession(firstTuple) {
                createTitle(
                    NewTitle(
                        "join-expired",
                        TitleType.JOIN,
                        secondTuple.uid,
                        communityId,
                        ObjectType.COMMUNITY,
                        "join",
                        LocalDateTime.parse("2000-01-01T00:00:00"),
                    ),
                ).getOrThrow()
            }
            loginSession(secondTuple) {
                assertFails {
                    joinCommunity(communityId).getOrThrow()
                }
            }
        }

    @Test
    fun `test get community rooms`() =
        test {
            val communityId =
                attachSession {
                    createCommunityForTest().id
                }.custom

            attachSession {
                // 测试获取社区中的所有房间
                val rooms = getCommunityRooms(communityId, CustomApi.Communities.Id.Rooms.CommunityRoomQuery())
                assertListSize(0, rooms)
            }
        }

    @Test
    fun `test update community font settings`() =
        test {
            val firstTuple =
                attachSession {
                    createCommunityForTest().id
                }
            val communityId = firstTuple.custom
            loginSession(firstTuple) {
                // Test setting empty font settings
                val fontSettings = FontSettings()
                val updatedInfo =
                    updateCommunityInfo(
                        communityId,
                        com.storyteller_f.shared.obj.UpdateCommunityBody(fontSettings = fontSettings),
                    ).getOrThrow()
                assertNotNull(updatedInfo.fontSettings)
                assertEquals(fontSettings, updatedInfo.fontSettings!!.settings)
            }
        }
}

suspend fun UserSessionManager.createCommunityForTest(
    name: String = "name1",
    id: String = "c1",
    memberPolicy: MemberPolicy = MemberPolicy.OPEN,
): CommunityInfo = createCommunity(NewCommunity(name, id, memberPolicy = memberPolicy)).getOrThrow()
