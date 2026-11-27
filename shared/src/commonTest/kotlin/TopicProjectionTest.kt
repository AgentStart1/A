/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared

import com.storyteller_f.shared.model.TopicActivity
import com.storyteller_f.shared.model.TopicCreated
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicInfo
import com.storyteller_f.shared.model.TopicReplyPreview
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.RoomFrame
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class TopicProjectionTest {
    @Test
    fun detailWrapsBaseAndFlattensRelations() {
        val reply = TopicReplyPreview(TopicInfo.EMPTY.copy(id = 2), UserInfo.EMPTY)
        val detail =
            TopicDetail(
                TopicInfo.EMPTY.copy(id = 1),
                UserInfo.EMPTY,
                persistentListOf(reply),
                persistentListOf(),
                null,
            )
        val json = commonJson.encodeToJsonElement(detail).jsonObject
        assertEquals(setOf("topicInfo", "authorInfo", "subTopics", "reactions", "roomInfo"), json.keys)
        assertFalse("extension" in json.getValue("topicInfo").jsonObject)
        assertEquals(detail, commonJson.decodeFromString<TopicDetail>(json.toString()))
    }

    @Test
    fun compactScenariosOmitUnloadedRelations() {
        val created = commonJson.encodeToJsonElement(TopicCreated(TopicInfo.EMPTY, UserInfo.EMPTY)).jsonObject
        val reply = commonJson.encodeToJsonElement(TopicReplyPreview(TopicInfo.EMPTY, UserInfo.EMPTY)).jsonObject
        val activity =
            commonJson.encodeToJsonElement(
                TopicActivity(TopicInfo.EMPTY, UserInfo.EMPTY, persistentListOf(), null),
            ).jsonObject
        assertEquals(setOf("topicInfo", "authorInfo"), created.keys)
        assertEquals(created.keys, reply.keys)
        assertEquals(setOf("topicInfo", "authorInfo", "reactions", "roomInfo"), activity.keys)
    }

    @Test
    fun activityUsesExplicitRepliesAndPreservesCompatibilityWithOlderResponses() {
        val reply = TopicReplyPreview(TopicInfo.EMPTY.copy(id = 2), UserInfo.EMPTY)
        val activity = TopicActivity(TopicInfo.EMPTY, UserInfo.EMPTY, persistentListOf(), null)
        val cached = activity.withCachedReplies(null).copy(subTopics = persistentListOf(reply))
        assertEquals(cached.subTopics, activity.withCachedReplies(cached).subTopics)

        val fresh = activity.copy(subTopics = persistentListOf(reply))
        val decoded = commonJson.decodeFromString<TopicActivity>(commonJson.encodeToString(fresh))
        assertEquals(fresh.subTopics, decoded.withCachedReplies(null).subTopics)
        assertEquals(emptyList(), fresh.copy(subTopics = persistentListOf()).withCachedReplies(cached).subTopics)
    }

    @Test
    fun newTopicFrameRoundTripsCreatedProjection() {
        val frame: RoomFrame = RoomFrame.NewTopicInfo(TopicCreated(TopicInfo.EMPTY, UserInfo.EMPTY))
        assertEquals(frame, commonJson.decodeFromString<RoomFrame>(commonJson.encodeToString(frame)))
    }

    @Test
    fun networkDetailRejectsLegacyUnwrappedBody() {
        val legacy = commonJson.encodeToString(TopicInfo.EMPTY)
        assertFailsWith<SerializationException> { commonJson.decodeFromString<TopicDetail>(legacy) }
    }
}
