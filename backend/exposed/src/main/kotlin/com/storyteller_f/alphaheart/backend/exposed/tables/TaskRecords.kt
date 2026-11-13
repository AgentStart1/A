/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.taskRecordType
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.WorkerTask
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.insert

object TaskRecords : BaseTable() {
    val type = taskRecordType("type")

    /** Identifier of the object processed by the task execution. */
    val objectId: Column<Long> = customPrimaryKey("object_id")

    /** Whether the task execution completed successfully. */
    val success: Column<Boolean> = bool("is_success").default(true)

    /** Optional machine-readable failure category. */
    val failureType: Column<String?> = varchar("failure_type", FAILURE_TYPE_LENGTH).nullable()

    /** Optional safe diagnostic failure reason. */
    val failureReason: Column<String?> = text("failure_reason").nullable()

    /** Whether an administrator requested another execution attempt. */
    val retryRequested: Column<Boolean> = bool("is_retry_requested").default(false)

    init {
        index("task-records-main", false, type)
        index("task-records-filter", false, type, success, failureType, retryRequested)
    }
}

fun TaskRecord.Companion.wrapRow(resultRow: ResultRow): TaskRecord {
    val taskRecord =
        with(TaskRecords) {
            val rowFailureType =
                if (resultRow[success]) {
                    null
                } else {
                    resultRow[failureType] ?: TaskRecordType.UNKNOWN_FAILURE
                }
            TaskRecord(
                id = resultRow[id],
                createdTime = resultRow[createdTime],
                objectId = resultRow[objectId],
                type = resultRow[type],
                failureType = rowFailureType,
                failureReason = resultRow[failureReason],
                isRetryRequested = resultRow[retryRequested],
            )
        }
    return taskRecord
}

suspend fun addTaskRecord(taskRecord: TaskRecord) {
    check(
        TaskRecords.insert { statement ->
            statement[TaskRecords.id] = taskRecord.id
            statement[TaskRecords.createdTime] = taskRecord.createdTime
            statement[TaskRecords.type] = taskRecord.type
            statement[TaskRecords.objectId] = taskRecord.objectId
            statement[TaskRecords.success] = taskRecord.isSuccess
            statement[TaskRecords.failureType] = taskRecord.failureType
            statement[TaskRecords.failureReason] = taskRecord.failureReason
            statement[TaskRecords.retryRequested] = taskRecord.isRetryRequested
        }.insertedCount > 0,
    ) {
        "Insert task record failed"
    }
}

private const val FAILURE_TYPE_LENGTH = 20

/** Persisted runtime configuration for worker tasks. */
object WorkerTasks : Table("worker_tasks") {
    /** Configured worker task type. */
    val type: Column<TaskRecordType> = taskRecordType("type")

    /** Whether the task may execute. */
    val enabled: Column<Boolean> = bool("enabled")

    /** Maximum number of objects fetched per iteration. */
    val fetchSize: Column<Int> = integer("fetch_size")

    /** Delay after an enabled iteration, in milliseconds. */
    val waitDurationMillis: Column<Long> = long("wait_duration_millis")

    override val primaryKey: PrimaryKey = PrimaryKey(type)
}

/** Maps a task configuration database row to its shared model. */
internal fun WorkerTask.Companion.wrapRow(resultRow: ResultRow): WorkerTask {
    val config =
        with(WorkerTasks) {
            WorkerTask(
                type = resultRow[type],
                isEnabled = resultRow[enabled],
                fetchSize = resultRow[fetchSize],
                waitDurationMillis = resultRow[waitDurationMillis],
            )
        }
    return config
}

/** Generic persisted backend configuration values. */
object BackendConfigs : Table("backend_configs") {
    /** Unique configuration key. */
    val key: Column<String> = varchar("key", CONFIG_KEY_MAX_LENGTH)

    /** Serialized configuration value interpreted by the caller that owns the key. */
    val value: Column<String> = text("value")

    override val primaryKey: PrimaryKey = PrimaryKey(key)
}

private const val CONFIG_KEY_MAX_LENGTH = 100
