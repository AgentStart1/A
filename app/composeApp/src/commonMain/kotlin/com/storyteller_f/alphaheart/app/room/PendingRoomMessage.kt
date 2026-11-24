/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.room

import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicCreated
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.PrimaryKey

/** Only a persisted echo for the submitted author, parent and content completes this draft. */
internal data class PendingRoomMessage(
    val content: String,
    val parent: ObjectTuple,
    val author: PrimaryKey,
    val expectedContent: String = content,
    val phase: RoomMessagePhase = RoomMessagePhase.PREPARING,
    private val operation: Any = Any(),
) {
    fun owns(other: PendingRoomMessage?): Boolean = other?.operation === operation

    fun matches(topic: TopicCreated): Boolean =
        phase == RoomMessagePhase.SUBMITTED && topic.author == author &&
            topic.parentId == parent.objectId && topic.parentType == parent.objectType &&
            (topic.content as? TopicContent.Plain)?.plain == expectedContent
}

internal enum class RoomMessagePhase { PREPARING, SUBMITTED }

/** Suspended preparation cannot resume an invalidated operation or migrate it to a new connection. */
internal suspend fun prepareRoomMessage(
    message: PendingRoomMessage,
    current: () -> PendingRoomMessage?,
    connectionValid: () -> Boolean,
    normalize: suspend () -> String,
): PendingRoomMessage? {
    val expected = normalize()
    return if (message.owns(current()) && current()?.phase == RoomMessagePhase.PREPARING && connectionValid()) {
        message.copy(expectedContent = expected, phase = RoomMessagePhase.SUBMITTED)
    } else {
        null
    }
}
