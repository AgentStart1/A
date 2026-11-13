/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.worker

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.Cursor
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.types.SubscriptionSentLog
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.cloud.error.WorkerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.generateModelMarkdownContent
import com.storyteller_f.shared.utils.now
import com.storytellerf.alphaheart.cloud.worker.DEFAULT_TASK_OBJECT_FETCH_SIZE
import com.storytellerf.alphaheart.cloud.worker.getSystemUserId
import io.github.aakira.napier.Napier

suspend fun Backend.doSubscriptionTask(fetchSize: Int = DEFAULT_TASK_OBJECT_FETCH_SIZE) {
    val result =
        database.user.getLatestTaskRecord(TaskRecordType.SUBSCRIPTION).andThen(WorkerError::fromError) { taskRecord ->
            val cursor = Cursor.AscCursor(taskRecord?.objectId ?: 0)
            database.topic.getTopicList(PrimaryKeyFetch(cursor, fetchSize))
        }.andThen(WorkerError::fromError) { topics ->
            if (topics.isEmpty()) {
                Napier.i(tag = SUBSCRIPTION_LOG_TAG) {
                    "no topic to send"
                }
                CustomResult.Success(Unit)
            } else {
                Napier.i(tag = SUBSCRIPTION_LOG_TAG) {
                    "process ${topics.size} topics"
                }
                CustomResult.Success(Unit).andThen(WorkerError::fromError) {
                    topics.forEach { topic ->
                        Napier.i(tag = SUBSCRIPTION_LOG_TAG) { "send topic ${topic.id}" }
                        processTopicSubscription(topic, fetchSize)
                    }
                    CustomResult.Success(Unit)
                }
            }
        }
    result.fold(
        onSuccess = {
            Napier.i(tag = SUBSCRIPTION_LOG_TAG) {
                "subscription task completed"
            }
        },
        onFailure = { failure ->
            Napier.e(tag = SUBSCRIPTION_LOG_TAG, throwable = failure.cause) {
                "subscription task failed"
            }
        },
    )
}

private suspend fun Backend.processTopicSubscription(topic: Topic, fetchSize: Int) {
    // 当前对象发送的最新日志
    val topicParentId = topic.parentId
    val content = generateTopicSubscriptionContent(topic, topicParentId)
    val latestSentSubscriptionId =
        database.subscription.getLatestSubscriptionSentLog(topic.id)
            .getOrThrow()?.subscriptionId
    var cursor = Cursor.AscCursor(latestSentSubscriptionId ?: 0)
    while (content != null) {
        val userSubscriptions =
            database.subscription.getSubscriptionsByObjectId(
                topicParentId,
                PrimaryKeyFetch(cursor, fetchSize),
            ).getOrThrow()
        if (userSubscriptions.isEmpty()) {
            break
        }
        val systemUserId = getSystemUserId()
        userSubscriptions.forEach { userSubscription ->
            val rawUser =
                database.user.getRawUser(ObjectFetch.IdFetch(userSubscription.uid))
                    .getOrThrow() ?: error("user not found")
            sendTopicToNotificationRoom(systemUserId, rawUser.user, content).getOrThrow()
            database.subscription.insertSubscriptionSentLog(
                SubscriptionSentLog(
                    SnowflakeFactory.nextId(),
                    userSubscription.uid,
                    topic.id,
                    ObjectType.TOPIC,
                    userSubscription.id,
                    now(),
                ),
            ).getOrThrow()
        }
        cursor = Cursor.AscCursor(userSubscriptions.last().id)
    }
    Napier.i(tag = "subscription") {
        "all user subscriptions sent for topic ${topic.id}"
    }
    database.admin.createTaskRecord(
        TaskRecord(
            id = SnowflakeFactory.nextId(),
            createdTime = now(),
            type = TaskRecordType.SUBSCRIPTION,
            objectId = topic.id,
        ),
    ).getOrThrow()
}

private fun generateTopicSubscriptionContent(topic: Topic, topicParentId: PrimaryKey): String? =
    when (topic.parentType) {
        ObjectType.TOPIC -> generateTopicSubscriptionContentForTopic(topic.id, topicParentId)
        ObjectType.ROOM -> generateTopicSubscriptionContentForRoom(topic.id, topicParentId)
        ObjectType.COMMUNITY -> generateTopicSubscriptionContentForCommunity(topic.id, topicParentId)
        ObjectType.USER -> generateTopicSubscriptionContentForUser(topic.id, topicParentId)
        else -> null
    }

private fun generateTopicSubscriptionContentForUser(topicId: PrimaryKey, topicParentId: PrimaryKey): String {
    val content =
        buildString {
            appendLine("New topic at user")
            appendLine(generateModelMarkdownContent(ObjectTuple(topicId, ObjectType.TOPIC)))
            appendLine(generateModelMarkdownContent(ObjectTuple(topicParentId, ObjectType.USER)))
        }
    return content
}

private fun generateTopicSubscriptionContentForCommunity(topicId: PrimaryKey, topicParentId: PrimaryKey): String {
    val content =
        buildString {
            appendLine("New topic at community")
            appendLine(generateModelMarkdownContent(ObjectTuple(topicId, ObjectType.TOPIC)))
            appendLine(generateModelMarkdownContent(ObjectTuple(topicParentId, ObjectType.COMMUNITY)))
        }
    return content
}

private fun generateTopicSubscriptionContentForRoom(topicId: PrimaryKey, topicParentId: PrimaryKey): String {
    val content =
        buildString {
            appendLine("New topic at room")
            appendLine(generateModelMarkdownContent(ObjectTuple(topicId, ObjectType.TOPIC)))
            appendLine(generateModelMarkdownContent(ObjectTuple(topicParentId, ObjectType.ROOM)))
        }
    return content
}

private fun generateTopicSubscriptionContentForTopic(topicId: PrimaryKey, topicParentId: PrimaryKey): String {
    val content =
        buildString {
            appendLine("New topic at topic")
            appendLine(generateModelMarkdownContent(ObjectTuple(topicId, ObjectType.TOPIC)))
            appendLine(generateModelMarkdownContent(ObjectTuple(topicParentId, ObjectType.TOPIC)))
        }
    return content
}

private const val SUBSCRIPTION_LOG_TAG = "subscription"
