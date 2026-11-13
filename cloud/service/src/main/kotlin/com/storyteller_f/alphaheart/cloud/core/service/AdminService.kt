/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.NewUser
import com.storyteller_f.alphaheart.api.TransferAuthKey
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.types.PanelLog
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.core.types.toPanelLogInfo
import com.storyteller_f.alphaheart.backend.core.types.toTaskRecordInfo
import com.storyteller_f.alphaheart.backend.core.types.toUserInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.AdminError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.PanelLogInfo
import com.storyteller_f.shared.model.PanelOverview
import com.storyteller_f.shared.model.PassType
import com.storyteller_f.shared.model.TaskRecordInfo
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.UpdateObjectStatusBody
import com.storyteller_f.shared.obj.UpdateUserStatusBody
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.now

suspend fun Backend.getOverview() =
    captureFailure(AdminError::from) {
        val userCount = database.user.getUserCount().getOrThrow()
        val topicCount = database.topic.getTopicCount().getOrThrow()
        val communityCount = database.community.getCommunityCount().getOrThrow()
        val privateRoomCount = database.room.getPrivateRoomCount().getOrThrow()
        val publicRoomCount = database.room.getPublicRoomCount().getOrThrow()
        val fileCount = database.file.getFileCount().getOrThrow()
        val fileVolume = database.file.getFileVolume().getOrThrow()
        val titleCount = database.title.getTitleCount().getOrThrow()

        PanelOverview(
            userCount,
            topicCount,
            communityCount,
            privateRoomCount,
            publicRoomCount,
            fileCount,
            fileVolume,
            titleCount,
        )
    }

suspend fun Backend.addUser(newUser: NewUser): CustomResult<UserInfo, AdminError> {
    val nickname = newUser.nickname
    if (nickname != null && nickname.isBlank()) {
        return CustomResult.Failure(
            AdminError.from(Exception(CustomBadRequestException("invalid nickname"))),
        ).mapError(AdminError::fromError)
    }
    captureFailure(AdminError::from) {
        checkUserNickname(newUser)
        checkAid(newUser.aid, true).getOrThrow()
    }.exceptionOrNull()?.let {
        return CustomResult.Failure(AdminError.from(it)).mapError(AdminError::fromError)
    }
    return captureFailure(AdminError::from) {
        val authKey = newUser.authKey
        val algoType = authKey.algo
        val (address, pemPubKey) =
            getAlgo(algoType).run {
                val address = calcAddress(authKey.derPublicKey).getOrThrow()
                address to authKey.derPublicKey
            }
        val encPubKey =
            when (authKey) {
                is TransferAuthKey.Dilithium -> authKey.derEncryptionPublicKey
                is TransferAuthKey.P256 -> null
            }

        val id = SnowflakeFactory.nextId()
        val notificationId = SnowflakeFactory.nextId()
        database.user.createUser(
            User(
                newUser.aid,
                encPubKey,
                pemPubKey,
                address,
                null,
                nickname ?: nameService.parse(id),
                id,
                now(),
                0,
                PassType.RAW,
                algoType,
                notificationId,
            ),
        ).getOrThrow().toUserInfo()
    }.mapError(AdminError::fromError)
}

suspend fun Backend.updateUserStatus(
    uid: com.storyteller_f.shared.type.PrimaryKey,
    body: UpdateUserStatusBody,
    adminId: com.storyteller_f.shared.type.PrimaryKey?,
): CustomResult<Unit, AdminError> {
    if (adminId == null) {
        return CustomResult.Failure(
            AdminError.from(UnauthorizedException()),
        ).mapError(AdminError::fromError)
    }
    return database.user.updateUserStatus(uid, body.status).andThen(AdminError::fromError) {
        val logId = SnowflakeFactory.nextId()
        database.admin.insertPanelLog(
            PanelLog(
                id = logId,
                adminId = adminId,
                targetId = uid,
                objectType = ObjectType.USER,
                action = "Update user status to ${body.status}",
                createdTime = now(),
            ),
        )
    }.mapError(AdminError::fromError)
}

suspend fun Backend.updateCommunityStatus(
    id: com.storyteller_f.shared.type.PrimaryKey,
    body: UpdateObjectStatusBody,
    adminId: com.storyteller_f.shared.type.PrimaryKey?,
): CustomResult<Unit, AdminError> {
    if (adminId == null) {
        return CustomResult.Failure(
            AdminError.from(UnauthorizedException()),
        ).mapError(AdminError::fromError)
    }
    return database.community.updateCommunityStatus(id, body.status).andThen(AdminError::fromError) {
        val logId = SnowflakeFactory.nextId()
        database.admin.insertPanelLog(
            PanelLog(
                id = logId,
                adminId = adminId,
                targetId = id,
                objectType = ObjectType.COMMUNITY,
                action = "Update community status to ${body.status}",
                createdTime = now(),
            ),
        )
    }.mapError(AdminError::fromError)
}

suspend fun Backend.updateRoomStatus(
    id: com.storyteller_f.shared.type.PrimaryKey,
    body: UpdateObjectStatusBody,
    adminId: com.storyteller_f.shared.type.PrimaryKey?,
): CustomResult<Unit, AdminError> {
    if (adminId == null) {
        return CustomResult.Failure(
            AdminError.from(UnauthorizedException()),
        ).mapError(AdminError::fromError)
    }
    return database.room.updateRoomStatus(id, body.status).andThen(AdminError::fromError) {
        val logId = SnowflakeFactory.nextId()
        database.admin.insertPanelLog(
            PanelLog(
                id = logId,
                adminId = adminId,
                targetId = id,
                objectType = ObjectType.ROOM,
                action = "Update room status to ${body.status}",
                createdTime = now(),
            ),
        )
    }.mapError(AdminError::fromError)
}

suspend fun Backend.updateTopicStatus(
    id: com.storyteller_f.shared.type.PrimaryKey,
    body: UpdateObjectStatusBody,
    adminId: com.storyteller_f.shared.type.PrimaryKey?,
): CustomResult<Unit, AdminError> {
    if (adminId == null) {
        return CustomResult.Failure(
            AdminError.from(UnauthorizedException()),
        ).mapError(AdminError::fromError)
    }
    return database.topic.updateTopicStatus(id, body.status).andThen(AdminError::fromError) {
        val logId = SnowflakeFactory.nextId()
        database.admin.insertPanelLog(
            PanelLog(
                id = logId,
                adminId = adminId,
                targetId = id,
                objectType = ObjectType.TOPIC,
                action = "Update topic status to ${body.status}",
                createdTime = now(),
            ),
        )
    }.mapError(AdminError::fromError)
}

suspend fun Backend.updateTitleStatus(
    id: com.storyteller_f.shared.type.PrimaryKey,
    body: UpdateObjectStatusBody,
    adminId: com.storyteller_f.shared.type.PrimaryKey?,
): CustomResult<Unit, AdminError> {
    if (adminId == null) {
        return CustomResult.Failure(
            AdminError.from(UnauthorizedException()),
        ).mapError(AdminError::fromError)
    }
    return database.title.updateTitleStatus(id, body.status).andThen(AdminError::fromError) {
        val logId = SnowflakeFactory.nextId()
        database.admin.insertPanelLog(
            PanelLog(
                id = logId,
                adminId = adminId,
                targetId = id,
                objectType = ObjectType.TITLE,
                action = "Update title status to ${body.status}",
                createdTime = now(),
            ),
        )
    }.mapError(AdminError::fromError)
}

suspend fun Backend.updateFileStatus(
    id: com.storyteller_f.shared.type.PrimaryKey,
    body: UpdateObjectStatusBody,
    adminId: com.storyteller_f.shared.type.PrimaryKey?,
): CustomResult<Unit, AdminError> {
    if (adminId == null) {
        return CustomResult.Failure(
            AdminError.from(UnauthorizedException()),
        ).mapError(AdminError::fromError)
    }
    return database.file.updateFileStatus(id, body.status).andThen(AdminError::fromError) {
        val logId = SnowflakeFactory.nextId()
        database.admin.insertPanelLog(
            PanelLog(
                id = logId,
                adminId = adminId,
                targetId = id,
                objectType = ObjectType.FILE,
                action = "Update file status to ${body.status}",
                createdTime = now(),
            ),
        )
    }.mapError(AdminError::fromError)
}

suspend fun Backend.getPanelLogs(
    targetId: com.storyteller_f.shared.type.PrimaryKey,
    objectType: ObjectType,
    fetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<PanelLogInfo>, AdminError> =
    database.admin.getPanelLogs(targetId, objectType, fetch).map { result ->
        PaginationResult(result.list.map { it.toPanelLogInfo() }, result.total)
    }.mapError(AdminError::fromError)

suspend fun Backend.getTaskRecords(
    type: TaskRecordType?,
    isSuccess: Boolean?,
    failureType: String?,
    fetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<TaskRecordInfo>, AdminError> {
    val taskRecords =
        database.admin.getTaskRecords(
            type = type,
            isSuccess = isSuccess,
            failureType = failureType,
            fetch = fetch,
        ).map { result ->
            PaginationResult(result.list.map { it.toTaskRecordInfo() }, result.total)
        }
    return taskRecords.mapError(AdminError::fromError)
}
