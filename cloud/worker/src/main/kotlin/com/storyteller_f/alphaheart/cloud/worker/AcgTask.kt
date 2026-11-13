/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.worker

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.Cursor.AscCursor
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch.IdListFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.types.AssetTransaction
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.cloud.error.WorkerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.AssetType
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.associateByPair
import com.storyteller_f.shared.utils.now
import com.storytellerf.alphaheart.cloud.worker.DEFAULT_TASK_OBJECT_FETCH_SIZE
import com.storytellerf.alphaheart.cloud.worker.executeTaskObject
import com.storytellerf.alphaheart.cloud.worker.getSystemUserId
import io.github.aakira.napier.Napier

suspend fun Backend.doAcgTask(fetchSize: Int = DEFAULT_TASK_OBJECT_FETCH_SIZE) {
    val result = executeAcgTask(fetchSize)
    result.fold(
        onSuccess = {
            Napier.i(tag = ACG_LOG_TAG) {
                "acg task completed"
            }
        },
        onFailure = { failure ->
            Napier.e(tag = ACG_LOG_TAG, throwable = failure.cause) {
                "acg task failed"
            }
        },
    )
}

private suspend fun Backend.executeAcgTask(fetchSize: Int): CustomResult<Unit, WorkerError> =
    database.user.getTaskRecordsToRetry(
        TaskRecordType.TOPIC_ACG,
        fetchSize,
    ).andThen(WorkerError::fromError) { retryRecords ->
        if (retryRecords.isEmpty()) {
            processNextAcgTopics(fetchSize)
        } else {
            retryRecords.forEach { retryRecord ->
                processAcgRetry(retryRecord)
            }
            CustomResult.success(Unit)
        }
    }.mapError(WorkerError::fromError)

private suspend fun Backend.processNextAcgTopics(fetchSize: Int): CustomResult<Unit, WorkerError> =
    database.user.getLatestTaskRecord(TaskRecordType.TOPIC_ACG).andThen(WorkerError::fromError) { taskRecord ->
        val cursor = AscCursor(taskRecord?.objectId ?: 0)
        database.topic.getTopicList(PrimaryKeyFetch(cursor = cursor, size = fetchSize))
    }.andThen(WorkerError::fromError) { topics ->
        if (topics.isEmpty()) {
            Napier.i(tag = ACG_LOG_TAG) {
                "no more topic"
            }
        } else {
            Napier.i(tag = ACG_LOG_TAG) {
                "process ${topics.size} topics"
            }
            topics.forEach { topic ->
                processAcgTopic(topic, retryRecordId = null)
            }
        }
        CustomResult.success(Unit)
    }.mapError(WorkerError::fromError)

private suspend fun Backend.processAcgRetry(record: TaskRecord) {
    executeTaskObject(
        type = TaskRecordType.TOPIC_ACG,
        objectId = record.objectId,
        retryRecordId = record.id,
        failureType = { TaskRecordType.DATA_ACCESS_FAILURE },
    ) { successRecord ->
        val topic =
            database.topic.getTopic(ObjectFetch.IdFetch(record.objectId)).getOrThrow()
                ?: error("Topic ${record.objectId} not found")
        addAcgForTopic(topic, successRecord)
    }.logAcgResult(record.objectId)
}

private suspend fun Backend.processAcgTopic(topic: Topic, retryRecordId: PrimaryKey?) {
    executeTaskObject(
        type = TaskRecordType.TOPIC_ACG,
        objectId = topic.id,
        retryRecordId = retryRecordId,
        failureType = { TaskRecordType.DATA_ACCESS_FAILURE },
    ) { successRecord ->
        addAcgForTopic(topic, successRecord)
    }.logAcgResult(topic.id)
}

private suspend fun Backend.addAcgForTopic(topic: Topic, successRecord: TaskRecord) {
    val userAcgMap =
        database.user.getUserAcgByIds(IdListFetch(listOf(topic.author))).getOrThrow().associateByPair()
    val assetTransaction =
        userAcgMap[topic.author]?.let { oldAcgAmount ->
            AssetTransaction(
                id = SnowflakeFactory.nextId(),
                uid = topic.author,
                createdTime = now(),
                type = AssetType.ACG,
                before = oldAcgAmount,
                after = oldAcgAmount + 1,
            )
        }
    database.user.addAcgForUser(successRecord, assetTransaction).getOrThrow()
    if (assetTransaction != null) {
        notifyAcgAuthor(topic)
    }
}

private suspend fun Backend.notifyAcgAuthor(topic: Topic) {
    CustomResult.Success(Unit).andThen(WorkerError::fromError) {
        val systemUserId = getSystemUserId()
        if (shouldNotifyAcgAuthor(topic.author, systemUserId)) {
            val rawUser =
                database.user.getRawUser(ObjectFetch.IdFetch(topic.author)).getOrThrow()
                    ?: error("User ${topic.author} not found")
            sendTopicToNotificationRoom(
                systemUserId,
                rawUser.user,
                buildAcgNotificationContent(topic.id),
            ).getOrThrow()
        }
        CustomResult.Success(Unit)
    }.onFailure { failure ->
        Napier.e(tag = ACG_LOG_TAG, throwable = failure.cause) {
            "failed to notify topic author ${topic.author} about ACG award for ${topic.id}"
        }
    }
}

private fun shouldNotifyAcgAuthor(authorId: PrimaryKey, systemUserId: PrimaryKey): Boolean = authorId != systemUserId

private fun buildAcgNotificationContent(topicId: PrimaryKey): String = "Your topic $topicId earned 1 ACG."

private fun CustomResult<Unit, WorkerError>.logAcgResult(objectId: PrimaryKey) {
    fold(
        onSuccess = {
            Napier.i(tag = ACG_LOG_TAG) {
                "processed topic $objectId"
            }
        },
        onFailure = { failure ->
            Napier.e(tag = ACG_LOG_TAG, throwable = failure.cause) {
                "failed to process topic $objectId"
            }
        },
    )
}

private const val ACG_LOG_TAG = "acg"
