/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.app.search.SearchScope
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SearchNavigationTest {
    @Test
    fun `browser history round trips each entity search scope`() {
        val json = Json { serializersModule = appNavSerializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)
        listOf(
            SearchScope.World,
            SearchScope.MyRoom,
            SearchScope.MyCommunity,
            SearchScope.CommunityTopic(12),
            SearchScope.RoomTopic(24),
            SearchScope.UserTopic(36),
        ).forEach { scope ->
            val route = SearchScreen(scope)
            assertEquals(route, json.decodeFromString(serializer, json.encodeToString(serializer, route)))
        }
    }
}
