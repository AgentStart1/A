/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.moderation

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.types.RawUser
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.cloud.worker.sendTopicToNotificationRoom
import com.storyteller_f.result.getOrElse
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import com.storytellerf.alphaheart.cloud.worker.getSystemUserId
import io.github.aakira.napier.Napier

internal suspend fun Backend.moderateTopic(topic: Topic, reviewer: TopicSafetyReviewer) {
    if (!reviewer.isHarmful(topic.content.decodeToString())) return

    val rawUser = getTopicAuthor(topic)
    updateTopicAuthorStatus(topic)
    notifyTopicAuthor(topic, rawUser)
    Napier.w(tag = MODERATION_LOG_TAG) {
        "marked topic author ${topic.author} as read only after reviewing topic ${topic.id}"
    }
}

private suspend fun Backend.getTopicAuthor(topic: Topic): RawUser =
    database.user.getRawUser(ObjectFetch.IdFetch(topic.author)).getOrElse { failure ->
        throw TopicModerationDataAccessException("Failed to load topic author ${topic.author}", failure.cause)
    } ?: throw TopicModerationDataAccessException("Topic author ${topic.author} not found")

private suspend fun Backend.updateTopicAuthorStatus(topic: Topic) {
    val isUpdated =
        database.user.updateUserStatus(topic.author, UserStatus.READ_ONLY).getOrElse { failure ->
            throw TopicModerationDataAccessException("Failed to update topic author ${topic.author}", failure.cause)
        }
    if (!isUpdated) {
        throw TopicModerationDataAccessException("Topic author ${topic.author} not found")
    }
}

private suspend fun Backend.notifyTopicAuthor(topic: Topic, rawUser: RawUser) {
    sendTopicToNotificationRoom(
        getSystemUserId(),
        rawUser.user,
        buildModerationNotificationContent(topic.id),
    ).getOrElse { failure ->
        throw TopicModerationDataAccessException("Failed to notify topic author ${topic.author}", failure.cause)
    }
}

internal fun buildModerationNotificationContent(topicId: PrimaryKey): String =
    "Your account was set to read only because topic $topicId contains harmful content."
