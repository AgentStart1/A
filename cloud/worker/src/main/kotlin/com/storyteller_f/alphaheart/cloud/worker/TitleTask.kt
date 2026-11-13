/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.worker

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.Cursor
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.backend.core.types.Title
import com.storyteller_f.alphaheart.cloud.error.WorkerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.type.PrimaryKey
import com.storytellerf.alphaheart.cloud.worker.DEFAULT_TASK_OBJECT_FETCH_SIZE
import com.storytellerf.alphaheart.cloud.worker.executeTaskObject
import com.storytellerf.alphaheart.cloud.worker.getSystemUserId
import io.github.aakira.napier.Napier

suspend fun Backend.doTitleTask(fetchSize: Int = DEFAULT_TASK_OBJECT_FETCH_SIZE) {
    val result = executeTitleTask(fetchSize)
    result.fold(
        onSuccess = {
            Napier.i(tag = TITLE_LOG_TAG) {
                "title task completed"
            }
        },
        onFailure = { failure ->
            Napier.e(tag = TITLE_LOG_TAG, throwable = failure.cause) {
                "title task failed"
            }
        },
    )
}

private suspend fun Backend.executeTitleTask(fetchSize: Int): CustomResult<Unit, WorkerError> =
    database.user.getTaskRecordsToRetry(
        TaskRecordType.TITLE,
        fetchSize,
    ).andThen(WorkerError::fromError) { retryRecords ->
        if (retryRecords.isEmpty()) {
            processNextTitles(fetchSize)
        } else {
            retryRecords.forEach { retryRecord ->
                processTitleRetry(retryRecord)
            }
            CustomResult.success(Unit)
        }
    }.mapError(WorkerError::fromError)

private suspend fun Backend.processNextTitles(fetchSize: Int): CustomResult<Unit, WorkerError> =
    database.user.getLatestTaskRecord(TaskRecordType.TITLE).andThen(WorkerError::fromError) { taskRecord ->
        val cursor = Cursor.AscCursor(taskRecord?.objectId ?: 0)
        database.title.getAllRawTitles(PrimaryKeyFetch(cursor, fetchSize))
    }.andThen(WorkerError::fromError) { result ->
        if (result.list.isEmpty()) {
            Napier.i(tag = TITLE_LOG_TAG) {
                "no title to send"
            }
        } else {
            Napier.i(tag = TITLE_LOG_TAG) {
                "process ${result.list.size} titles"
            }
            result.list.forEach { rawTitle ->
                processTitle(rawTitle.title, retryRecordId = null)
            }
        }
        CustomResult.success(Unit)
    }.mapError(WorkerError::fromError)

private suspend fun Backend.processTitleRetry(record: TaskRecord) {
    executeTaskObject(
        type = TaskRecordType.TITLE,
        objectId = record.objectId,
        retryRecordId = record.id,
        failureType = { TaskRecordType.DATA_ACCESS_FAILURE },
    ) { successRecord ->
        val title =
            database.title.getTitle(record.objectId).getOrThrow()?.title
                ?: error("Title ${record.objectId} not found")
        processTitleNotification(title, successRecord)
    }.logTitleResult(record.objectId)
}

private suspend fun Backend.processTitle(title: Title, retryRecordId: PrimaryKey?) {
    executeTaskObject(
        type = TaskRecordType.TITLE,
        objectId = title.id,
        retryRecordId = retryRecordId,
        failureType = { TaskRecordType.DATA_ACCESS_FAILURE },
    ) { successRecord ->
        processTitleNotification(title, successRecord)
    }.logTitleResult(title.id)
}

private suspend fun Backend.processTitleNotification(title: Title, successRecord: TaskRecord) {
    val rawUser =
        database.user.getRawUser(ObjectFetch.IdFetch(title.receiver))
            .getOrThrow() ?: error("user not found")

    val content = generateTitleNotificationContent(title)
    sendTopicToNotificationRoom(getSystemUserId(), rawUser.user, content).getOrThrow()
    database.admin.createTaskRecord(successRecord).getOrThrow()

    Napier.i(tag = TITLE_LOG_TAG) {
        "send title notification to user ${title.receiver}"
    }
}

private fun CustomResult<Unit, WorkerError>.logTitleResult(objectId: PrimaryKey) {
    fold(
        onSuccess = {
            Napier.i(tag = TITLE_LOG_TAG) {
                "processed title $objectId"
            }
        },
        onFailure = { failure ->
            Napier.e(tag = TITLE_LOG_TAG, throwable = failure.cause) {
                "failed to process title $objectId"
            }
        },
    )
}

private fun generateTitleNotificationContent(title: Title): String =
    buildString {
        appendLine("You received a new title!")
        appendLine("Title: ${title.name}")
        appendLine("From: System")
        appendLine("Created: ${title.createdTime}")
        if (title.expiresAt != null) {
            appendLine("Expires: ${title.expiresAt ?: "<none>"}")
        }
    }

private const val TITLE_LOG_TAG = "title"
