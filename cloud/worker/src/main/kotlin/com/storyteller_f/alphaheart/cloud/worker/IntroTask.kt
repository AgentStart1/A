/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.worker

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.Cursor
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.types.RawUser
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.core.types.buildMemberForNotificationRoom
import com.storyteller_f.alphaheart.backend.core.types.buildUserNotificationRoom
import com.storyteller_f.alphaheart.cloud.core.service.createTopicAtRoom
import com.storyteller_f.alphaheart.cloud.error.WorkerError
import com.storyteller_f.alphaheart.cloud.ws.api.GlobalWsEventPublisher
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.buildEncryptedTopicContent
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.obj.NewRoomTopic
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storytellerf.alphaheart.cloud.worker.DEFAULT_TASK_OBJECT_FETCH_SIZE
import com.storytellerf.alphaheart.cloud.worker.executeTaskObject
import com.storytellerf.alphaheart.cloud.worker.getSystemUserId
import io.github.aakira.napier.Napier

suspend fun Backend.doIntroTask(fetchSize: Int = DEFAULT_TASK_OBJECT_FETCH_SIZE) {
    val result = executeIntroTask(fetchSize)
    result.fold(
        onSuccess = {
            Napier.i(tag = INTRO_LOG_TAG) {
                "intro task completed"
            }
        },
        onFailure = { failure ->
            Napier.e(tag = INTRO_LOG_TAG, throwable = failure.cause) {
                "intro task failed"
            }
        },
    )
}

private suspend fun Backend.executeIntroTask(fetchSize: Int): CustomResult<Unit, WorkerError> =
    database.user.getTaskRecordsToRetry(
        TaskRecordType.INTRO,
        fetchSize,
    ).andThen(WorkerError::fromError) { retryRecords ->
        if (retryRecords.isEmpty()) {
            processNextIntroUsers(fetchSize)
        } else {
            retryRecords.forEach { retryRecord ->
                processIntroRetry(retryRecord)
            }
            CustomResult.success(Unit)
        }
    }.mapError(WorkerError::fromError)

private suspend fun Backend.processNextIntroUsers(fetchSize: Int): CustomResult<Unit, WorkerError> =
    database.user.getLatestTaskRecord(TaskRecordType.INTRO).andThen(WorkerError::fromError) { taskRecord ->
        val fetch =
            PrimaryKeyFetch(
                Cursor.AscCursor(taskRecord?.objectId ?: INTRO_START_OBJECT_ID),
                fetchSize,
            )
        database.user.getAllUsers(fetch)
    }.andThen(WorkerError::fromError) { paginationResult ->
        if (paginationResult.list.isEmpty()) {
            Napier.i(tag = INTRO_LOG_TAG) {
                "no more user, total user count is ${paginationResult.total}"
            }
        } else {
            Napier.i(tag = INTRO_LOG_TAG) {
                "process ${paginationResult.list.size} users"
            }
            paginationResult.list.forEach { rawUser ->
                processIntroUser(rawUser, retryRecordId = null)
            }
        }
        CustomResult.success(Unit)
    }.mapError(WorkerError::fromError)

private suspend fun Backend.processIntroRetry(record: TaskRecord) {
    executeTaskObject(
        type = TaskRecordType.INTRO,
        objectId = record.objectId,
        retryRecordId = record.id,
        failureType = { TaskRecordType.DATA_ACCESS_FAILURE },
    ) { successRecord ->
        val rawUser =
            database.user.getRawUser(ObjectFetch.IdFetch(record.objectId)).getOrThrow()
                ?: error("User ${record.objectId} not found")
        sendHelloTopic(rawUser, successRecord)
    }.logIntroResult(record.objectId)
}

private suspend fun Backend.processIntroUser(rawUser: RawUser, retryRecordId: PrimaryKey?) {
    executeTaskObject(
        type = TaskRecordType.INTRO,
        objectId = rawUser.user.id,
        retryRecordId = retryRecordId,
        failureType = { TaskRecordType.DATA_ACCESS_FAILURE },
    ) { successRecord ->
        sendHelloTopic(rawUser, successRecord)
    }.logIntroResult(rawUser.user.id)
}

private suspend fun Backend.sendHelloTopic(rawUser: RawUser, successRecord: TaskRecord) {
    val systemUserId = getSystemUserId()
    sendTopicToNotificationRoom(
        systemUserId,
        rawUser.user,
        "Hello, ${rawUser.user.nickname}",
    ).getOrThrow()
    database.admin.createTaskRecord(successRecord).getOrThrow()
}

private fun CustomResult<Unit, WorkerError>.logIntroResult(objectId: PrimaryKey) {
    fold(
        onSuccess = {
            Napier.i(tag = INTRO_LOG_TAG) {
                "sent hello topic to user $objectId"
            }
        },
        onFailure = { failure ->
            Napier.e(tag = INTRO_LOG_TAG, throwable = failure.cause) {
                "failed to send hello topic to user $objectId"
            }
        },
    )
}

internal suspend fun Backend.sendTopicToNotificationRoom(
    uid: PrimaryKey,
    user: User,
    content: String,
): CustomResult<Unit, WorkerError> =
    getNotificationRoom(user).andThen(WorkerError::fromError) { room ->
        (room?.let { CustomResult.success(it) } ?: createNotificationRoom(user, uid))
            .andThen(WorkerError::fromError) { notificationRoom ->
                sedTopicAtRoom(uid, notificationRoom.id, content)
            }
    }.mapError(WorkerError::fromError)

private suspend fun Backend.getNotificationRoom(user: User): CustomResult<Room?, WorkerError> =
    database.room.getRawRoom(
        ObjectFetch.IdFetch(user.notificationId),
    ).map { rawRoom -> rawRoom?.room }.mapError(WorkerError::fromError)

private suspend fun Backend.createNotificationRoom(user: User, uid: PrimaryKey): CustomResult<Room, WorkerError> =
    database.room.createRoom(
        buildUserNotificationRoom(user, uid),
        buildMemberForNotificationRoom(user, uid),
    ).mapError(WorkerError::fromError)

internal suspend fun Backend.sedTopicAtRoom(
    uid: PrimaryKey,
    roomId: PrimaryKey,
    content: String,
): CustomResult<Unit, WorkerError> =
    database.room.getRoomPubKeyPaginationResult(
        roomId,
        PrimaryKeyFetch(null, 10),
    ).andThen(WorkerError::fromError) { paginationResult ->
        val encrypted = buildEncryptedTopicContent(content, paginationResult.list)
        createTopicAtRoom(
            NewRoomTopic(ObjectType.ROOM, roomId, encrypted),
            uid,
        ).andThen(WorkerError::fromError) { topic ->
            topic?.let {
                GlobalWsEventPublisher.publishNewTopic(RoomFrame.NewTopicInfo(it))
            }
            CustomResult.success(Unit)
        }
    }.mapError(WorkerError::fromError)

private const val INTRO_START_OBJECT_ID = 1000L
private const val INTRO_LOG_TAG = "intro"
