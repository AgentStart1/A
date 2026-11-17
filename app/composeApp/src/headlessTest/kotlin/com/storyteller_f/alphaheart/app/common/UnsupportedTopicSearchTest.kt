/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.app.PlatformHeadlessTest
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.createSimpleUserSessionManager
import com.storyteller_f.alphaheart.client.room.RoomModelStorage
import com.storyteller_f.alphaheart.client.room.getRoomDatabase
import com.storyteller_f.shared.type.ObjectType
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

class UnsupportedTopicSearchTest : PlatformHeadlessTest() {
    @Test
    fun worldAndChildTopicCannotRequestRecommendationsOrThrowUnsupportedScopeErrors() =
        runTest {
            var requests = 0
            val session =
                createSimpleUserSessionManager(
                    "wss://search.invalid/ws",
                    passHolder = ConstPassHolder(null),
                    createClient = { _, _ ->
                        HttpClient(
                            MockEngine {
                                requests++
                                error("Unsupported search must not issue any request, including recommendations")
                            },
                        )
                    },
                ) { _, _, _ -> }
            val database = getRoomDatabase("unsupported-search-${Uuid.random().toHexString()}")
            try {
                val storage = RoomModelStorage(database)
                TopicSearchViewModel(session, storage, "UI验证", null, null).flow.first()
                TopicSearchViewModel(session, storage, "UI验证", 123, ObjectType.TOPIC).flow.first()
                assertEquals(0, requests)
            } finally {
                session.client.close()
                database.close()
            }
        }
}
