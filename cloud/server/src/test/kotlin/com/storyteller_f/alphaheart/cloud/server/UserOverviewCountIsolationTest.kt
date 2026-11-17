/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.addFavorite
import com.storyteller_f.alphaheart.client.core.addReaction
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getFavorites
import com.storyteller_f.alphaheart.client.core.getUserOverview
import com.storyteller_f.alphaheart.client.core.removeFavorite
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UserOverviewCountIsolationTest {
    @Test
    fun personalOverviewCountsRemainIsolatedAcrossUsersAndFavoriteRemoval() =
        test(mapOf("DISABLE_TEST_WS" to "true")) {
            val first = attachSession { populatePersonalCounters(it.uid, 1) }
            val second = attachSession { populatePersonalCounters(it.uid, 2) }
            loginSession(first) {
                assertPersonalCounters(getUserOverview().getOrThrow(), 1)
                assertListTotalSize(1, getFavorites(PaginationQuery()))
                removeFavorite(first.custom, ObjectType.TOPIC).getOrThrow()
                val updated = getUserOverview().getOrThrow()
                assertPersonalCounters(updated, 1, favoriteCount = 0)
                assertListTotalSize(0, getFavorites(PaginationQuery()))
            }
            loginSession(second) {
                assertPersonalCounters(getUserOverview().getOrThrow(), 2)
                assertListTotalSize(2, getFavorites(PaginationQuery()))
            }
            attachSession {
                assertPersonalCounters(getUserOverview().getOrThrow(), 0)
                assertListTotalSize(0, getFavorites(PaginationQuery()))
            }
        }
}

private suspend fun UserSessionManager.populatePersonalCounters(uid: PrimaryKey, count: Int): PrimaryKey {
    assertPersonalCounters(getUserOverview().getOrThrow(), 0)
    val topics =
        List(count) { index ->
            createTopic(ObjectType.USER, uid, "Personal topic $index").getOrThrow().id
        }
    for (topicId in topics) {
        addFavorite(NewFavorite(ObjectType.TOPIC, topicId)).getOrThrow()
        addReaction(topicId, "👍").getOrThrow()
        createTopic(ObjectType.TOPIC, topicId, "Personal reply").getOrThrow()
    }
    return topics.first()
}

private fun assertPersonalCounters(overview: UserOverview, count: Long, favoriteCount: Long = count) {
    assertEquals(favoriteCount, overview.favoriteCount)
    assertEquals(count, overview.reactionRecordCount)
    assertEquals(count, overview.commentCount)
    // Creating each original topic and each reply uses the existing automatic subscription behavior.
    assertEquals(count * 2, overview.subscriptionCount)
    assertEquals(0L, overview.childAccountCount)
}
