/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.ObjectFetch.IdFetch
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.getRawTopic
import com.storyteller_f.alphaheart.backend.core.service.FileDocument
import com.storyteller_f.alphaheart.backend.core.types.FileRecord
import com.storyteller_f.alphaheart.backend.core.types.Quota
import com.storyteller_f.alphaheart.backend.core.types.toQuotaInfo
import com.storyteller_f.alphaheart.backend.core.types.toUploadRecordInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.ContainerError
import com.storyteller_f.result.ConstraintFailure
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.recover
import com.storyteller_f.shared.model.QuotaDetail
import com.storyteller_f.shared.model.QuotaInfo
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import io.github.aakira.napier.Napier
import org.apache.tika.Tika

data class RootReadPermission(val hasRead: Boolean, val hasJoined: Boolean, val isPrivate: Boolean)

data class RootWritePermission(val rootType: ObjectType, val rootId: PrimaryKey, val level: Int = 0) {
    val tuple = ObjectTuple(rootId, rootType)
}

data class RootAdminPermission(val rootType: ObjectType, val rootId: PrimaryKey)

suspend fun Backend.checkTopicReadPermission(
    topicId: PrimaryKey,
    uid: PrimaryKey?,
): CustomResult<RootReadPermission?, ContainerError> =
    database.topic.getTopicRootTuple(topicId).andThenPresent(ContainerError::fromError) { (rootId, rootType) ->
        checkRootReadPermission(rootType, rootId, uid)
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkRoomReadPermission(
    roomId: PrimaryKey,
    uid: PrimaryKey?,
): CustomResult<RootReadPermission?, ContainerError> =
    database.room.getRoomCommunityId(roomId).andThen(ContainerError::fromError) { communityId ->
        if (communityId == null && uid == null) {
            CustomResult.Failure(ContainerError.from(UnauthorizedException()))
        } else {
            database.container.isMemberJoined(roomId, uid).map { hasJoined ->
                RootReadPermission(hasJoined || communityId != null, hasJoined, communityId == null)
            }
        }
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkCommunityReadPermission(
    communityId: PrimaryKey,
    uid: PrimaryKey?,
): CustomResult<RootReadPermission?, ContainerError> =
    database.community.getRawCommunity(IdFetch(communityId)).andThenPresent(ContainerError::fromError) {
        database.container.isMemberJoined(communityId, uid).map { hasJoined ->
            RootReadPermission(true, hasJoined, false)
        }
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkUserReadPermission(userId: PrimaryKey): CustomResult<RootReadPermission?, ContainerError> =
    database.user.getRawUser(IdFetch(userId)).mapPresent {
        RootReadPermission(hasRead = true, hasJoined = false, isPrivate = false)
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkRootReadPermission(
    parentType: ObjectType,
    parentId: PrimaryKey,
    uid: PrimaryKey?,
): CustomResult<RootReadPermission?, ContainerError> =
    when (parentType) {
        ObjectType.TOPIC -> checkTopicReadPermission(parentId, uid)

        ObjectType.ROOM -> checkRoomReadPermission(parentId, uid)

        ObjectType.COMMUNITY -> checkCommunityReadPermission(parentId, uid)

        ObjectType.USER -> checkUserReadPermission(parentId)

        ObjectType.TITLE ->
            CustomResult.success(
                RootReadPermission(hasRead = true, hasJoined = false, isPrivate = false),
            )

        ObjectType.FILE -> CustomResult.Failure(ContainerError.from(ForbiddenException()))

        ObjectType.PANEL_ACCOUNT -> CustomResult.Failure(ContainerError.from(ForbiddenException()))
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkTopicWritePermission(
    topicId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootWritePermission?, ContainerError> =
    database.getRawTopic(IdFetch(topicId), null).andThenPresent(ContainerError::fromError) { topicInfo ->
        if (topicInfo.topic.readOnly) {
            CustomResult.Failure(ContainerError.from(ForbiddenException("topic is read only")))
        } else {
            checkRootWritePermission(
                topicInfo.topic.rootType,
                topicInfo.topic.rootId,
                uid,
            ).mapPresent {
                it.copy(level = topicInfo.topic.level)
            }
        }
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkRoomWritePermission(
    roomId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootWritePermission?, ContainerError> =
    database.room.getRawRoom(IdFetch(roomId), true, uid).andThenPresent(ContainerError::fromError) {
        if (it.room.readOnly) {
            CustomResult.Failure(ContainerError.from(ForbiddenException("room is read only")))
        } else if (it.hasJoined) {
            CustomResult.success(RootWritePermission(ObjectType.ROOM, roomId))
        } else {
            CustomResult.Failure(ContainerError.from(ForbiddenException()))
        }
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkCommunityWritePermission(
    communityId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootWritePermission?, ContainerError> =
    database.community.getRawCommunity(IdFetch(communityId), true, uid).andThenPresent(ContainerError::fromError) {
        if (it.community.readOnly) {
            CustomResult.Failure(ContainerError.from(ForbiddenException("community is read only")))
        } else if (it.hasJoined) {
            CustomResult.success(RootWritePermission(ObjectType.COMMUNITY, communityId))
        } else {
            CustomResult.Failure(ContainerError.from(ForbiddenException()))
        }
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkUserWritePermission(
    userId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootWritePermission?, ContainerError> =
    (
        if (uid == userId) {
            database.user.getRawUser(IdFetch(userId)).mapPresent {
                RootWritePermission(ObjectType.USER, userId)
            }
        } else {
            CustomResult.Failure(ContainerError.from(ForbiddenException()))
        }
        ).mapError(ContainerError::fromError)

suspend fun Backend.checkRootWritePermission(
    parentType: ObjectType,
    parentId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootWritePermission?, ContainerError> =
    database.user.getRawUser(IdFetch(uid)).andThenPresent(ContainerError::fromError) { user ->
        if (user.user.status == UserStatus.READ_ONLY) {
            CustomResult.Failure(ContainerError.from(ForbiddenException("User is read only")))
        } else {
            checkRootWritePermissionForUser(parentType, parentId, uid)
        }
    }.mapError(ContainerError::fromError)

private suspend fun Backend.checkRootWritePermissionForUser(
    parentType: ObjectType,
    parentId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootWritePermission?, ContainerError> =
    when (parentType) {
        ObjectType.TOPIC -> checkTopicWritePermission(parentId, uid)
        ObjectType.ROOM -> checkRoomWritePermission(parentId, uid)
        ObjectType.COMMUNITY -> checkCommunityWritePermission(parentId, uid)
        ObjectType.USER -> checkUserWritePermission(parentId, uid)
        ObjectType.TITLE -> CustomResult.Failure(ContainerError.from(ForbiddenException()))
        ObjectType.FILE -> CustomResult.Failure(ContainerError.from(ForbiddenException()))
        ObjectType.PANEL_ACCOUNT -> CustomResult.Failure(ContainerError.from(ForbiddenException()))
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkTopicAdminPermission(
    topicId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootAdminPermission?, ContainerError> =
    database.getRawTopic(IdFetch(topicId), null).andThenPresent(ContainerError::fromError) { topicInfo ->
        if (topicInfo.topic.readOnly) {
            CustomResult.Failure(ContainerError.from(ForbiddenException("topic is read only")))
        } else {
            checkRootAdminPermission(topicInfo.topic.rootType, topicInfo.topic.rootId, uid)
        }
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkRoomAdminPermission(
    roomId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootAdminPermission?, ContainerError> =
    database.room.getRawRoom(IdFetch(roomId), true, uid)
        .andThenPresent(ContainerError::fromError) {
            if (it.room.readOnly) {
                CustomResult.Failure(ContainerError.from(ForbiddenException("room is read only")))
            } else if (it.room.creator == uid) {
                CustomResult.success(RootAdminPermission(ObjectType.ROOM, roomId))
            } else {
                CustomResult.Failure(ContainerError.from(ForbiddenException()))
            }
        }.mapError(ContainerError::fromError)

suspend fun Backend.checkCommunityAdminPermission(
    communityId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootAdminPermission?, ContainerError> =
    database.community.getRawCommunity(IdFetch(communityId))
        .andThenPresent(ContainerError::fromError) {
            if (it.community.readOnly) {
                CustomResult.Failure(ContainerError.from(ForbiddenException("community is read only")))
            } else if (it.community.owner == uid) {
                CustomResult.success(RootAdminPermission(ObjectType.COMMUNITY, communityId))
            } else {
                CustomResult.Failure(ContainerError.from(ForbiddenException()))
            }
        }.mapError(ContainerError::fromError)

suspend fun Backend.checkUserAdminPermission(
    userId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootAdminPermission?, ContainerError> =
    (
        if (userId == uid) {
            database.user.getRawUser(IdFetch(userId)).mapPresent {
                RootAdminPermission(ObjectType.USER, userId)
            }
        } else {
            CustomResult.Failure(ContainerError.from(ForbiddenException()))
        }
        ).mapError(ContainerError::fromError)

suspend fun Backend.checkRootAdminPermission(
    parentType: ObjectType,
    parentId: PrimaryKey,
    uid: PrimaryKey,
): CustomResult<RootAdminPermission?, ContainerError> =
    when (parentType) {
        ObjectType.TOPIC -> checkTopicAdminPermission(parentId, uid)
        ObjectType.ROOM -> checkRoomAdminPermission(parentId, uid)
        ObjectType.COMMUNITY -> checkCommunityAdminPermission(parentId, uid)
        ObjectType.USER -> checkUserAdminPermission(parentId, uid)
        ObjectType.TITLE -> CustomResult.success(RootAdminPermission(parentType, parentId))
        ObjectType.FILE -> CustomResult.Failure(ContainerError.from(ForbiddenException()))
        ObjectType.PANEL_ACCOUNT -> CustomResult.Failure(ContainerError.from(ForbiddenException()))
    }.mapError(ContainerError::fromError)

suspend fun Backend.checkObjectWritable(
    objectType: ObjectType,
    objectId: PrimaryKey,
): CustomResult<Unit, ContainerError> =
    when (objectType) {
        ObjectType.COMMUNITY ->
            database.community.getRawCommunity(IdFetch(objectId)).andThen(ContainerError::fromError) {
                checkWritableFlag(it?.community?.readOnly, "community")
            }

        ObjectType.ROOM ->
            database.room.getRawRoom(IdFetch(objectId)).andThen(ContainerError::fromError) {
                checkWritableFlag(it?.room?.readOnly, "room")
            }

        ObjectType.TOPIC ->
            database.topic.getTopic(IdFetch(objectId)).andThen(ContainerError::fromError) {
                checkWritableFlag(it?.readOnly, "topic")
            }

        ObjectType.TITLE ->
            database.title.getTitle(objectId).andThen(ContainerError::fromError) {
                checkWritableFlag(it?.title?.readOnly, "title")
            }

        ObjectType.FILE ->
            database.file.getFileRecordByIds(listOf(objectId)).andThen(ContainerError::fromError) {
                checkWritableFlag(it.firstOrNull()?.readOnly, "file")
            }

        ObjectType.USER, ObjectType.PANEL_ACCOUNT -> CustomResult.success(Unit)
    }

private fun checkWritableFlag(readOnly: Boolean?, kind: String): CustomResult<Unit, ContainerError> =
    if (readOnly == true) {
        CustomResult.Failure(ContainerError.Forbidden(ForbiddenException("$kind is read only")))
    } else {
        CustomResult.success(Unit)
    }

/**
 * 执行出错会释放配额并标记为失败.
 */
suspend fun <E : FailureDetails> Backend.lockQuotaInfo(
    objectTuple: ObjectTuple,
    quotaType: QuotaType,
    length: Long,
    block: suspend () -> CustomResult<List<FileRecord>, E>,
) = checkQuotaStatus(objectTuple, length, quotaType).andThen(ContainerError::fromError) { quotaInfo ->
    val operationId = SnowflakeFactory.nextId()
    database.file.lockFileQuota(quotaInfo, operationId).andThen(ContainerError::fromError) {
        var committed = false
        try {
            val files = block().getOrThrow()
            database.file.finishFileQuota(quotaInfo, operationId, files).getOrThrow()
            committed = true
            fileSearchService.saveDocument(files.map(FileDocument::fromFileRecord)).onFailure { error ->
                Napier.e(error.cause) { "save file document failed" }
            }
            CustomResult.success(files)
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            CustomResult.Failure(ContainerError.from(failure))
        } finally {
            if (!committed) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    database.file.finishFileQuota(quotaInfo, operationId, emptyList()).onFailure { error ->
                        Napier.e(error.cause) { "release file quota failed" }
                    }
                }
            }
        }
    }
}

suspend fun Backend.getQuotaInfo(quotaType: QuotaType, objectTuple: ObjectTuple) =
    getQuotaDetail(quotaType, objectTuple).map { it.quotaInfo }

suspend fun Backend.getQuotaDetail(quotaType: QuotaType, objectTuple: ObjectTuple) =
    database.container.getQuotaInfo(objectTuple.objectId, quotaType).andThen(ContainerError::fromError) { quota ->
        if (quota == null) {
            insertQuotaAndGet(quotaType, objectTuple).map { QuotaDetail(it) }
        } else {
            val info = quota.toQuotaInfo()
            val lockId = info.lockId
            if (lockId != null) {
                database.file.getUploadRecord(lockId).andThen(ContainerError::fromError) { rec ->
                    CustomResult.success(
                        QuotaDetail(info, rec?.toUploadRecordInfo()),
                    )
                }
            } else {
                CustomResult.success(QuotaDetail(info))
            }
        }
    }

suspend fun Backend.checkQuotaStatus(
    tuple: ObjectTuple,
    size: Long,
    quotaType: QuotaType,
): CustomResult<QuotaInfo, ContainerError> =
    getQuotaInfo(quotaType, tuple).andThen(ContainerError::fromError) {
        if (it.lockId != null) {
            CustomResult.Failure(ContainerError.from(CustomBadRequestException("quota is locking")))
        } else if (it.used + size > it.total) {
            CustomResult.Failure(ContainerError.from(CustomBadRequestException("quota is not enough")))
        } else {
            CustomResult.success(it)
        }
    }.mapError(ContainerError::fromError)

private suspend fun Backend.insertQuotaAndGet(
    quotaType: QuotaType,
    objectTuple: ObjectTuple,
): CustomResult<QuotaInfo, ContainerError> {
    val (ownerId, ownerType) = objectTuple
    val quota = Quota(ownerId, ownerType, 1024 * 1024 * 1024, 0, quotaType, null)
    return database.container.insertQuota(quota).map {
        quota
    }.recover(ContainerError::fromError) { failure ->
        if (failure is ConstraintFailure) {
            database.container.getQuotaInfo(ownerId, quotaType).andThen(ContainerError::fromError) {
                if (it == null) {
                    CustomResult.Failure(ContainerError.from(Exception("get quota failed")))
                } else {
                    CustomResult.success(it)
                }
            }
        } else {
            CustomResult.Failure(failure)
        }
    }.map {
        it.toQuotaInfo()
    }.mapError(ContainerError::fromError)
}

val Backend.Companion.tika by lazy {
    Tika()
}
