/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.TaskRecordInfo
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class TaskRecord(
    /** Unique identifier of this task execution. */
    val id: PrimaryKey,
    /** Time when this task execution was recorded. */
    val createdTime: LocalDateTime,
    /** Kind of worker task that produced this execution. */
    val type: TaskRecordType,
    /** Identifier of the business object processed by this execution. */
    val objectId: PrimaryKey,
    /** Classification of the failure, or null when this execution succeeded. */
    val failureType: String? = null,
    /** Safe diagnostic reason for the failure, when available. */
    val failureReason: String? = null,
    /** Whether an administrator requested this failed execution be retried. */
    val isRetryRequested: Boolean = false,
) {
    /** Whether this execution completed successfully. */
    val isSuccess: Boolean = failureType == null

    companion object
}

fun TaskRecord.toTaskRecordInfo(): TaskRecordInfo {
    val taskRecordInfo =
        TaskRecordInfo(
            id = id,
            createdTime = createdTime,
            objectId = objectId,
            type = type,
            isSuccess = isSuccess,
            failureType = failureType,
            failureReason = failureReason,
            isRetryRequested = isRetryRequested,
        )
    return taskRecordInfo
}
