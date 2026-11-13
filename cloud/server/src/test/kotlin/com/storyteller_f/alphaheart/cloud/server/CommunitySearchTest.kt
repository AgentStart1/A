/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.getUserCommunities
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.searchCommunity
import com.storyteller_f.alphaheart.client.core.updateCommunityInfo
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.obj.UpdateCommunityBody
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.sha256
import io.ktor.http.ContentType
import kotlinx.io.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class CommunitySearchTest {
    @Test
    fun `joined community list contains joined communities`() =
        test {
            withSearchCommunities {
                val result =
                    getUserCommunities(
                        query = CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(size = 10),
                    ).getOrThrow()
                assertEquals(2, result.data.size, "Should find 2 joined communities")
            }
        }

    @Test
    fun `joined community keyword search filters membership`() =
        test {
            withSearchCommunities {
                val result =
                    searchCommunity(size = 10, joinStatusSearch = JoinStatusSearch.JOINED, word = "test")
                        .getOrThrow()
                assertEquals(2, result.data.size, "Should find 2 joined communities matching 'test'")
            }
        }

    @Test
    fun `global community search includes unjoined entries`() =
        test {
            withSearchCommunities {
                listOf("community" to 3, "another" to 1).forEach { (word, expected) ->
                    val result =
                        searchCommunity(size = 10, joinStatusSearch = JoinStatusSearch.UNSPECIFIED, word = word)
                            .getOrThrow()
                    assertEquals(expected, result.data.size, "Unexpected community count for $word")
                }
            }
        }

    @Test
    fun `test get user communities with poster filter`() =
        test {
            val session =
                attachSession { userSession ->
                    val community1 = createCommunityForTest("no poster community", "nop1")

                    val community2 = createCommunityForTest("has poster community", "hap1")

                    val inputStream = ClassLoader.getSystemResourceAsStream("cover.jpg")!!
                    val buf = java.io.ByteArrayOutputStream()
                    val image = javax.imageio.ImageIO.read(inputStream).getSubimage(0, 0, 300, 400)
                    val out = java.io.File("build/test/poster.jpg")
                    out.parentFile?.mkdirs()
                    javax.imageio.ImageIO.write(image, "jpg", out)
                    javax.imageio.ImageIO.write(image, "jpg", buf)
                    val bytes = buf.toByteArray()
                    val fileInfo =
                        upload(
                            userSession.uid ob ObjectType.USER,
                            UploadData(
                                buf.size().toLong(),
                                "cover.jpg",
                                ContentType.Image.JPEG,
                                sha256(
                                    Buffer().apply { write(bytes) }.peek(),
                                ),
                            ) {
                                Buffer().apply { write(bytes) }
                            },
                        ).getOrThrow().data.first()

                    updateCommunityInfo(community2.id, UpdateCommunityBody(poster = fileInfo.id)).getOrThrow()

                    Triple(userSession.uid, community1.id, community2.id)
                }

            attachSession {
                val (_, c1, c2) = session.custom
                joinCommunity(c1).getOrThrow()
                joinCommunity(c2).getOrThrow()

                // Get all user communities
                val all =
                    getUserCommunities(
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(size = 10),
                    ).getOrThrow().data
                assertEquals(2, all.size, "Should get 2 joined communities")

                // Get only has poster communities
                val onlyPoster =
                    getUserCommunities(
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(
                            hasPoster = PosterSearch.HAS_POSTER,
                            size = 10,
                        ),
                    ).getOrThrow().data
                assertEquals(1, onlyPoster.size, "Should get 1 community with poster")
                assertEquals(c2, onlyPoster.first().id)

                // Optional: Get no poster communities
                val noPoster =
                    getUserCommunities(
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(
                            hasPoster = PosterSearch.NO_POSTER,
                            size = 10,
                        ),
                    ).getOrThrow().data
                assertEquals(1, noPoster.size, "Should get 1 community without poster")
                assertEquals(c1, noPoster.first().id)
            }
        }
}

private suspend fun TestMate.withSearchCommunities(block: suspend UserSessionManager.() -> Unit) {
    val owner =
        attachSession {
            val first = createCommunityForTest("test community 1", "tc1")
            val second = createCommunityForTest("test community 2", "tc2")
            createCommunityForTest("another community", "ac1")
            first.id to second.id
        }
    attachSession {
        joinCommunity(owner.custom.first).getOrThrow()
        joinCommunity(owner.custom.second).getOrThrow()
        block()
    }
}
