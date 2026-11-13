/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.CommonPath
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.SearchQuery
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.JoinSearch
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.ObjectFetch.IdFetch
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.ROOM_NAME_LENGTH
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.fixedSort
import com.storyteller_f.alphaheart.backend.core.idListFetch
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.service.MemberDocument
import com.storyteller_f.alphaheart.backend.core.service.MemberDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.RoomDocument
import com.storyteller_f.alphaheart.backend.core.service.RoomDocumentSearch
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.RawRoom
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.alphaheart.backend.core.types.toRoomInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.RoomError
import com.storyteller_f.result.ConstraintFailure
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.firstOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onPresent
import com.storyteller_f.result.onSuccess
import com.storyteller_f.result.recover
import com.storyteller_f.result.requireTrue
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import io.github.aakira.napier.Napier

suspend fun Backend.getRoomPubKeys(roomId: PrimaryKey, userId: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
    database.container.isMemberJoined(roomId, userId).andThen(RoomError::fromError) {
        if (it) {
            database.room.getRoomPubKeyPaginationResult(roomId, primaryKeyFetch)
        } else {
            CustomResult.Failure(RoomError.from(ForbiddenException("Permission denied")))
        }
    }

suspend fun Backend.joinRoom(roomId: PrimaryKey, uid: PrimaryKey) =
    database.room.getRawRoom(IdFetch(roomId)).andThenPresent(RoomError::fromError) { rawRoom ->
        val communityId = rawRoom.room.communityId
        if (communityId == null) {
            // 检查是否存在title
            getJoinTitleByScope(uid, roomId)
        } else {
            database.container.isMemberJoined(communityId, uid).requireTrue(RoomError::fromError) {
                RoomError.from(
                    run {
                        ForbiddenException("you should join community first.")
                    },
                )
            }
        }.andThen(RoomError::fromError) {
            database.container.getMember(roomId, uid).andThen(RoomError::fromError) {
                if (it != null && it.status == MemberStatus.JOINED) {
                    CustomResult.success(it)
                } else {
                    joinRoomOrUpdateMemberStatus(it, uid, rawRoom).onSuccess {
                        addUserLog(uid, UserLogType.JOIN, roomId ob ObjectType.ROOM)
                    }
                }
            }
        }.andThen(RoomError::fromError) {
            getRoomInfo(IdFetch(roomId), uid, true)
        }.recover(RoomError::fromError) { failure ->
            if (failure is ConstraintFailure) {
                getRoomInfo(IdFetch(roomId), uid, true)
            } else {
                CustomResult.Failure(failure)
            }
        }
    }

private suspend fun Backend.joinRoomOrUpdateMemberStatus(
    member: Member?,
    uid: PrimaryKey,
    rawRoom: RawRoom,
): CustomResult<Member, RoomError> {
    val time = now()
    val room = rawRoom.room
    val roomId = room.id
    return if (member == null) {
        database.container.addMember(
            Member(SnowflakeFactory.nextId(), uid, roomId, ObjectType.ROOM, time, MemberStatus.JOINED, time),
        )
    } else {
        database.container.updateMemberStatus(
            Member(member.id, member.uid, member.objectId, member.objectType, time, MemberStatus.JOINED, time),
        )
    }.onSuccess {
        // 保存到 MemberSearchService
        saveMemberDocument(uid, it, roomId, room)
    }.mapError(RoomError::fromError)
}

private suspend fun Backend.saveMemberDocument(uid: PrimaryKey, member: Member, roomId: PrimaryKey, room: Room) {
    getUserInfo(IdFetch(uid)).onPresent { userInfo ->
        memberSearchService.saveDocument(
            listOf(
                MemberDocument.fromUserInfo(
                    member.id,
                    userInfo,
                    roomId,
                    ObjectType.ROOM,
                    room.name,
                    room.communityId,
                ),
            ),
        ).onFailure { e ->
            Napier.e(e.cause) {
                "save member document failed"
            }
        }
    }
}

suspend fun Backend.getJoinTitleByScope(uid: PrimaryKey, scopeId: PrimaryKey): CustomResult<Unit, RoomError> =
    database.title.getTitlePaginationResult(
        PrimaryKeyFetch(null, 1),
        uid,
        TitleSearchType.RECEIVER,
        TitleType.JOIN,
        scopeId,
        TitleWorkStatus.OK,
    ).andThen(RoomError::fromError) {
        if (it.list.isNotEmpty()) {
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(RoomError.from(ForbiddenException("Join failed.")))
        }
    }.mapError(RoomError::fromError)

suspend fun Backend.exitRoom(roomId: PrimaryKey, uid: PrimaryKey) =
    getRoomInfo(IdFetch(roomId), uid, true).andThenPresent(RoomError::fromError) { info ->
        if (info.joinedTime == null) {
            CustomResult.success(info)
        } else {
            database.container.deleteMember(roomId, uid).onSuccess {
                // 从 MemberSearchService 删除
                memberSearchService.deleteDocument(uid, roomId).onFailure { e ->
                    Napier.e(e.cause) {
                        "delete member document failed"
                    }
                }
            }.map {
                addUserLog(uid, UserLogType.JOIN, roomId ob ObjectType.ROOM)
                info.copy(joinedTime = null)
            }
        }
    }

suspend fun Backend.getRoomInfo(objectFetch: ObjectFetch, uid: PrimaryKey?, fillJoinInfo: Boolean?) =
    database.room.getRawRoom(objectFetch, fillJoinInfo, uid).andThenPresent(RoomError::fromError) {
        processRawRoomToRoomInfo(listOf(it)).mapPresent(List<RoomInfo>::first)
    }

private fun checkRoomName(newRoom: NewRoom): CustomResult<Unit, RoomError> =
    when (checkNickname(newRoom.name, 1..ROOM_NAME_LENGTH)) {
        StringCheckResult.RANGE_MISMATCH ->
            CustomResult.Failure(
                RoomError.from(
                    CustomBadRequestException("room name must be between in 1 and 20"),
                ),
            )

        StringCheckResult.CONTAIN_INVALID_CHAR ->
            CustomResult.Failure(
                RoomError.from(CustomBadRequestException("room name must be visible")),
            )

        StringCheckResult.SUCCESS -> CustomResult.Success(Unit)

        StringCheckResult.NULL, StringCheckResult.EMPTY ->
            CustomResult.Failure(RoomError.from(CustomBadRequestException("room name must be visible")))
    }.mapError(RoomError::fromError)

suspend fun Backend.createRoom(newRoom: NewRoom, uid: PrimaryKey): CustomResult<RoomInfo?, RoomError> {
    captureFailure(RoomError::from) {
        checkAid(newRoom.aid).getOrThrow()
        checkRoomName(newRoom).getOrThrow()
    }.exceptionOrNull()?.let {
        return CustomResult.Failure(RoomError.from(it)).mapError(RoomError::fromError)
    }
    val communityId = newRoom.communityId
    return if (communityId != null) {
        checkCommunityAdminPermission(communityId, uid).andThenPresent(RoomError::fromError) {
            CustomResult.Success(Unit)
        }
    } else {
        CustomResult.success(Unit)
    }.andThenPresent(RoomError::fromError) {
        val roomId = SnowflakeFactory.nextId()
        val memberId = SnowflakeFactory.nextId()
        val room = Room(roomId, now(), newRoom.aid, newRoom.name, uid, newRoom.icon, communityId)
        val member =
            Member(
                memberId,
                room.creator,
                room.id,
                ObjectType.ROOM,
                room.createdTime,
                MemberStatus.JOINED,
                room.createdTime,
            )
        database.room.createRoom(room, listOf(member)).onSuccess {
            if (communityId != null) {
                roomSearchService.saveDocument(listOf(RoomDocument.fromRoom(room))).onFailure {
                    Napier.e(it.cause) {
                        "save room document failed"
                    }
                }
            }
            saveMemberDocument(uid, member, roomId, room)
        }
    }.andThenPresent(RoomError::fromError) { room ->
        processRawRoomToRoomInfo(listOf(RawRoom(room, room.createdTime)))
    }.firstOrNull().mapError(RoomError::fromError)
}

suspend fun Backend.updateRoom(
    id: PrimaryKey,
    old: UpdateRoomBody,
    uid: PrimaryKey,
): CustomResult<RoomInfo?, RoomError> {
    val newUpdate = old.copy(name = old.name?.trim(), icon = old.icon)
    return checkRoomAdminPermission(id, uid).andThenPresent(RoomError::fromError) {
        captureFailure(RoomError::from) {
            checkRoomName(newUpdate).getOrThrow()
            checkRoomIcon(newUpdate).getOrThrow()
        }
    }.andThenPresent(RoomError::fromError) {
        checkObjectWritable(ObjectType.ROOM, id)
    }.andThenPresent(RoomError::fromError) {
        database.room.updateRoom(id, newUpdate)
    }.andThenPresent(RoomError::fromError) {
        if (newUpdate.name == null) CustomResult.success(Unit) else refreshRoomSearch(id)
    }.andThenPresent(RoomError::fromError) {
        getRoomInfo(IdFetch(id), uid, true)
    }.mapError(RoomError::fromError)
}

private suspend fun Backend.checkRoomIcon(update: UpdateRoomBody): CustomResult<Unit, RoomError> =
    checkIcon(update.icon, Dimension.ROOM_DIMENSION).andThen(RoomError::fromError) { checkResult ->
        when (checkResult) {
            MediaCheckResult.NOT_FOUND ->
                CustomResult.Failure(
                    RoomError.from(CustomBadRequestException("icon not found")),
                )

            MediaCheckResult.CONTENT_TYPE_MISMATCH ->
                CustomResult.Failure(
                    RoomError.from(CustomBadRequestException("only support image")),
                )

            MediaCheckResult.DIMENSION_MISMATCH ->
                CustomResult.Failure(
                    RoomError.from(CustomBadRequestException("dimension mismatch")),
                )

            MediaCheckResult.EMPTY, MediaCheckResult.SUCCESS, null -> CustomResult.Success(Unit)
        }
    }.mapError(RoomError::fromError)

private fun checkRoomName(update: UpdateRoomBody): CustomResult<Unit, RoomError> =
    when (
        checkNickname(
            update.name,
            1..ROOM_NAME_LENGTH,
        )
    ) {
        StringCheckResult.RANGE_MISMATCH ->
            CustomResult.Failure(
                RoomError.from(
                    CustomBadRequestException("room name must be between in 1 and $ROOM_NAME_LENGTH"),
                ),
            )

        StringCheckResult.CONTAIN_INVALID_CHAR ->
            CustomResult.Failure(
                RoomError.from(CustomBadRequestException("room name must be visible")),
            )

        StringCheckResult.SUCCESS -> CustomResult.Success(Unit)

        StringCheckResult.NULL, StringCheckResult.EMPTY ->
            CustomResult.Failure(RoomError.from(CustomBadRequestException("room name must be visible")))
    }.mapError(RoomError::fromError)

suspend fun searchRoomMembers(
    backend: Backend,
    p: CommonPath,
    uid: PrimaryKey?,
    q: SearchQuery,
    f: OffsetFetch,
): CustomResult<PaginationResult<MemberInfo>?, RoomError> {
    val word = q.word.trim()
    if (word.isBlank()) {
        return CustomResult.success(PaginationResult(emptyList(), 0))
    }
    return backend.checkRoomReadPermission(p.id, uid).andThenPresent(RoomError::fromError) { permission ->
        if (permission.hasRead) {
            backend.searchContainerMembers(p.id, word, f)
        } else {
            CustomResult.Failure(RoomError.from(UnauthorizedException()))
        }
    }.mapError(RoomError::fromError)
}

suspend fun Backend.processRawRoomToRoomInfo(list: List<RawRoom>) =
    database.file.getFileRecordByIds(
        list.mapNotNull {
            it.room.icon
        },
    ).andThen(RoomError::fromError) { medias ->
        processFileRecordToFileInfo(medias).map { mediaList ->
            val mediaInfoMap = mediaList.associateBy { it.id }
            list.map { rawRoom ->
                rawRoom.toRoomInfo(icon = rawRoom.room.icon?.let { mediaInfoMap[it] })
            }
        }
    }

suspend fun Backend.getRoomInfoList(listFetch: ObjectListFetch.IdListFetch, uid: PrimaryKey? = null) =
    database.room.getRawRooms(listFetch, uid).andThen(RoomError::fromError) {
        processRawRoomToRoomInfo(it)
    }

suspend fun Backend.getAllPublicRooms(primaryKeyFetch: PrimaryKeyFetch) =
    database.room.getRoomPaginationResult(
        uid = null,
        community = null,
        primaryKeyFetch = primaryKeyFetch,
        joinSearch = JoinSearch.Unspecified(null),
    ).mapPagingResultNotNull(RoomError::fromError) { list ->
        processRawRoomToRoomInfo(list)
    }

suspend fun Backend.getAllPrivateRooms(primaryKeyFetch: PrimaryKeyFetch) =
    database.room.getPrivateRoomPaginationResult(
        primaryKeyFetch = primaryKeyFetch,
        word = null,
    ).mapPagingResultNotNull(RoomError::fromError) { list ->
        processRawRoomToRoomInfo(list)
    }

suspend fun Backend.getUserJoinedRooms(
    uid: PrimaryKey,
    primaryKeyFetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<RoomInfo>, RoomError> =
    database.room.getRoomPaginationResult(
        uid = uid,
        community = null,
        primaryKeyFetch = primaryKeyFetch,
        joinSearch = JoinSearch.Joined(uid),
    ).mapPagingResultNotNull(RoomError::fromError) { list ->
        processRawRoomToRoomInfo(list)
    }.mapError(RoomError::fromError)

suspend fun Backend.getCommunityRooms(
    communityId: PrimaryKey,
    primaryKeyFetch: PrimaryKeyFetch,
    uid: PrimaryKey?,
    joinStatus: JoinStatusSearch? = null,
): CustomResult<PaginationResult<RoomInfo>, RoomError> =
    database.room.getRoomPaginationResult(
        uid = uid,
        community = communityId,
        primaryKeyFetch = primaryKeyFetch,
        joinSearch = joinStatus.toJoinSearch(uid),
    ).mapPagingResultNotNull(RoomError::fromError) { list ->
        processRawRoomToRoomInfo(list)
    }.mapError(RoomError::fromError)

/**
 * 搜索社区中的房间.
 * @param uid 用户ID
 * @param communityId 社区ID
 * @param primaryKeyFetch 分页参数
 * @param query 搜索查询参数
 * @return 返回房间信息列表
 */
suspend fun Backend.searchCommunityRooms(
    uid: PrimaryKey?,
    communityId: PrimaryKey,
    primaryKeyFetch: OffsetFetch,
    query: CustomApi.Communities.Id.Rooms.CommunityRoomSearchQuery,
): CustomResult<PaginationResult<RoomInfo>, RoomError> {
    val word = query.word
    val search = query.joinStatus?.toJoinSearch(uid)
    if (word.isBlank()) return CustomResult.success(PaginationResult(emptyList(), 0))

    // word 不为空 && 搜索已加入的房间，使用 memberSearchService
    return if (search is JoinSearch.Joined) {
        memberSearchService.searchDocument(
            MemberDocumentSearch.RoomMembers(
                uid = search.uid,
                objectName = word,
                communityId = communityId,
                fetch = primaryKeyFetch,
            ),
        ).mapPagingResultNotNull(RoomError::fromError) { searchResults ->
            val roomIds = searchResults.map { it.objectId }
            // 过滤出属于该社区的房间
            getOrderedSearchRooms(roomIds, uid)
        }
    } else {
        // word 不为空 && 不是搜索已加入（Unspecified），使用 roomSearchService
        val keyword = RoomDocumentSearch.Keyword(word, communityId, fetch = primaryKeyFetch)
        roomSearchService.searchDocument(
            keyword,
        ).mapPagingResultNotNull(RoomError::fromError) { list ->
            val roomIds = list.map { it.id }
            // 过滤出属于该社区的房间
            getOrderedSearchRooms(roomIds, uid)
        }
    }.mapPagingResultNotNull(RoomError::fromError) { list ->
        processRawRoomToRoomInfo(list)
    }.mapError(RoomError::fromError)
}

/**
 * 搜索当前用户的房间.
 * @param uid 当前登录用户ID
 * @param primaryKeyFetch 分页参数
 * @param query 搜索查询参数
 * @return 返回房间信息列表
 */
suspend fun Backend.searchCurrentUserRooms(
    uid: PrimaryKey,
    primaryKeyFetch: OffsetFetch,
    query: CustomApi.Users.JoinedRooms.UserRoomsSearchQuery,
): CustomResult<PaginationResult<RoomInfo>?, RoomError> {
    val word = query.word
    if (word.isBlank()) return CustomResult.success(PaginationResult(emptyList(), 0))
    return memberSearchService.searchDocument(
        MemberDocumentSearch.RoomMembers(uid = uid, objectName = word, fetch = primaryKeyFetch),
    ).mapPagingResultNotNull(RoomError::fromError) { searchResults ->
        val roomIds = searchResults.map { it.objectId }
        getOrderedSearchRooms(roomIds, uid)
    }.mapPagingResultNotNull(RoomError::fromError) { list ->
        processRawRoomToRoomInfo(list)
    }.mapError(RoomError::fromError)
}

suspend fun Backend.uncheckedGetRoomMemberInfos(
    roomId: PrimaryKey,
    primaryKeyFetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<MemberInfo>, RoomError> =
    getContainerMemberInfos(
        roomId,
        primaryKeyFetch,
    ).mapError(RoomError::fromError)

suspend fun Backend.getRoomMemberInfos(
    roomId: PrimaryKey,
    uid: PrimaryKey?,
    primaryKeyFetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<MemberInfo>?, RoomError> {
    // 检查权限
    return checkRoomReadPermission(roomId, uid).andThenPresent(RoomError::fromError) { permission ->
        if (permission.hasRead) {
            uncheckedGetRoomMemberInfos(roomId, primaryKeyFetch)
        } else {
            CustomResult.Failure(RoomError.from(UnauthorizedException()))
        }
    }.mapError(RoomError::fromError)
}

private suspend fun Backend.getOrderedSearchRooms(ids: List<PrimaryKey>, uid: PrimaryKey?) =
    database.room.getRawRooms(idListFetch(ids), uid).map { rooms ->
        fixedSort(rooms, ids) { it.room.id }
    }
