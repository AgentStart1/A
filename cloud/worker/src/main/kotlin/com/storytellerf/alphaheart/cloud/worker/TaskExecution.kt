/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.cloud.error.WorkerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now

internal suspend fun Backend.executeTaskObject(
    type: TaskRecordType,
    objectId: PrimaryKey,
    retryRecordId: PrimaryKey? = null,
    failureType: (Throwable) -> String,
    executeAndSaveSuccess: suspend (TaskRecord) -> Unit,
): CustomResult<Unit, WorkerError> {
    val successRecord =
        TaskRecord(
            id = SnowflakeFactory.nextId(),
            createdTime = now(),
            type = type,
            objectId = objectId,
        )
    val result =
        captureFailure(WorkerError::from) {
            executeAndSaveSuccess(successRecord)
        }
    result.exceptionOrNull()?.let { failure ->
        database.admin.createTaskRecord(
            TaskRecord(
                id = SnowflakeFactory.nextId(),
                createdTime = now(),
                type = type,
                objectId = objectId,
                failureType = failureType(failure),
                failureReason = failure.message ?: failure::class.simpleName,
            ),
        ).getOrThrow()
    }
    retryRecordId?.let { database.user.updateTaskRecordRetryRequested(it, false).getOrThrow() }
    return result.mapError(WorkerError::fromError)
}

internal suspend fun Backend.getSystemUserId(): PrimaryKey {
    val systemUser = database.user.getRawUser(ObjectFetch.AidFetch(SYSTEM_USER_AID)).getOrThrow()
    return systemUser?.run { user.id } ?: error("System user not found")
}

internal const val DEFAULT_TASK_OBJECT_FETCH_SIZE = 10
private const val SYSTEM_USER_AID = "System"
