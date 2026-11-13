/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.CustomApi.Accounts.ChildAccounts.AddChildAccountRequest
import com.storyteller_f.alphaheart.api.NewDevice
import com.storyteller_f.alphaheart.api.NewUser
import com.storyteller_f.alphaheart.backend.core.AID_LENGTH
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.USER_NICKNAME
import com.storyteller_f.alphaheart.backend.core.getUserOverview
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.paging
import com.storyteller_f.alphaheart.backend.core.pagingNotNull
import com.storyteller_f.alphaheart.backend.core.processTopicToRawTopic
import com.storyteller_f.alphaheart.backend.core.service.MemberDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.UserDocument
import com.storyteller_f.alphaheart.backend.core.service.UserDocumentSearch
import com.storyteller_f.alphaheart.backend.core.types.RawUser
import com.storyteller_f.alphaheart.backend.core.types.RawUserOverview
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.core.types.UserLog
import com.storyteller_f.alphaheart.backend.core.types.UserTopicRead
import com.storyteller_f.alphaheart.backend.core.types.toUserInfo
import com.storyteller_f.alphaheart.backend.core.types.toUserLogInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.UserError
import com.storyteller_f.result.ConstraintFailure
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.recover
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.ChildAccountInfo
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.PassType
import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.model.UnreadRoomsResponse
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserLogItem
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.model.checkMediaFileDimensionRatioMatch
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.UpdateUserBody
import com.storyteller_f.shared.obj.UpdateUserRead
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import io.github.aakira.napier.Napier

suspend fun Backend.updateUser(uid: PrimaryKey, old: UpdateUserBody): CustomResult<UserInfo?, UserError> {
    val newUpdate = old.copy(nickname = old.nickname?.trim(), aid = old.aid?.trim(), avatar = old.avatar)
    captureFailure(UserError::from) {
        checkAidModifyTimes(newUpdate, uid).getOrThrow()
        checkAid(newUpdate.aid, true).getOrThrow()
        checkUserNickname(old).getOrThrow()
        checkUserIcon(newUpdate)
    }.exceptionOrNull()?.let {
        return CustomResult.Failure(UserError.from(it)).mapError(UserError::fromError)
    }
    return database.user.updateUserInfo(uid, newUpdate).andThen(UserError::fromError) {
        if (it) {
            addUserLog(uid, UserLogType.UPDATE, uid ob ObjectType.USER)
            getUserInfo(ObjectFetch.IdFetch(uid))
        } else {
            CustomResult.success(null)
        }
    }.mapError(UserError::fromError)
}

private suspend fun Backend.checkUserIcon(newUser: UpdateUserBody) {
    checkIcon(newUser.avatar, Dimension.DEFAULT_DIMENSION).andThen(UserError::fromError) { checkResult ->
        when (checkResult) {
            MediaCheckResult.NOT_FOUND ->
                CustomResult.Failure(
                    UserError.from(CustomBadRequestException("avatar not font")),
                )

            MediaCheckResult.CONTENT_TYPE_MISMATCH ->
                CustomResult.Failure(
                    UserError.from(CustomBadRequestException("avatar must be image")),
                )

            MediaCheckResult.DIMENSION_MISMATCH ->
                CustomResult.Failure(
                    UserError.from(CustomBadRequestException("dimension mismatch")),
                )

            MediaCheckResult.EMPTY, MediaCheckResult.SUCCESS, null -> CustomResult.Success(Unit)
        }
    }.getOrThrow()
}

fun checkUserNickname(newUser: NewUser) {
    when (
        checkNickname(
            newUser.nickname,
            1..USER_NICKNAME,
        )
    ) {
        StringCheckResult.CONTAIN_INVALID_CHAR -> throw CustomBadRequestException(
            "user nickname must not contain invalid char",
        )

        StringCheckResult.RANGE_MISMATCH -> throw CustomBadRequestException("user nickname must be between in 1 and 20")

        StringCheckResult.NULL, StringCheckResult.EMPTY, StringCheckResult.SUCCESS -> CustomResult.Success(Unit)
    }
}

private suspend fun Backend.checkAidModifyTimes(newUser: UpdateUserBody, id: PrimaryKey) =
    if (newUser.aid.isNullOrBlank()) {
        CustomResult.Success(Unit)
    } else {
        // check aid is null
        database.user.getUserAid(id).andThen(UserError::fromError) {
            if (it != null) {
                CustomResult.Failure(UserError.from(CustomBadRequestException("aid is not null.")))
            } else {
                CustomResult.Success(Unit)
            }
        }
    }

fun checkAid(aid: String?, supportEmptyAid: Boolean = false): CustomResult<Unit, UserError> {
    if (aid.isNullOrBlank()) {
        if (supportEmptyAid) {
            return CustomResult.Success(Unit)
        }
        return CustomResult.Failure(
            UserError.from(CustomBadRequestException("aid is empty")),
        ).mapError(UserError::fromError)
    }
    if (aid.length !in 2..AID_LENGTH) {
        return CustomResult.Failure(
            UserError.from(CustomBadRequestException("aid too long or too short")),
        ).mapError(UserError::fromError)
    }

    if (aid.all {
            !it.isEnglishLetter()
        }
    ) {
        return CustomResult.Failure(
            UserError.from(CustomBadRequestException("aid must contains english letter")),
        ).mapError(UserError::fromError)
    }
    if (!aid.all {
            it.isEnglishLetter() || it.isDigit() || it == '_' || it == '-'
        }
    ) {
        return CustomResult.Failure(
            UserError.from(CustomBadRequestException("only support alphabet, number, underline and hyphen")),
        ).mapError(UserError::fromError)
    }
    val underlineCount = aid.count { it == '_' || it == '-' }
    if (underlineCount / aid.length > 1.0 / 4) {
        return CustomResult.Failure(
            UserError.from(CustomBadRequestException("aid contains too many underline or hyphen")),
        ).mapError(UserError::fromError)
    }
    return checkUnderlineAndHyphen(aid).mapError(UserError::fromError)
}

private fun checkUnderlineAndHyphen(aid: String): CustomResult<Unit, UserError> {
    for (i in 0 until aid.length - 1) {
        if ((aid[i] == '_' || aid[i] == '-') && (aid[i + 1] == '_' || aid[i + 1] == '-')) {
            return CustomResult.Failure(
                UserError.from(CustomBadRequestException("aid contains double underline or hyphen")),
            ).mapError(UserError::fromError)
        }
    }
    return CustomResult.Success(Unit)
}

fun checkNickname(nickname: String?, range: IntRange): StringCheckResult {
    if (nickname == null) return StringCheckResult.NULL
    if (nickname.isBlank()) return StringCheckResult.EMPTY
    if (nickname.length !in range) return StringCheckResult.RANGE_MISMATCH
    if (!isAllVisibleChar(nickname)) return StringCheckResult.CONTAIN_INVALID_CHAR
    return StringCheckResult.SUCCESS
}

/**
 * 接受空字符串或者null.
 */
fun checkUserNickname(update: UpdateUserBody): CustomResult<Unit, UserError> =
    when (checkNickname(update.nickname, 1..USER_NICKNAME)) {
        StringCheckResult.RANGE_MISMATCH ->
            CustomResult.Failure(
                UserError.from(
                    CustomBadRequestException("user nickname must be between in 1 and $USER_NICKNAME"),
                ),
            )

        StringCheckResult.CONTAIN_INVALID_CHAR ->
            CustomResult.Failure(
                UserError.from(
                    CustomBadRequestException("user nickname must be visible"),
                ),
            )

        StringCheckResult.NULL, StringCheckResult.EMPTY, StringCheckResult.SUCCESS -> CustomResult.Success(Unit)
    }.mapError(UserError::fromError)

fun isAllVisibleChar(s: String) =
    !s.codePoints().anyMatch { codePoint ->
        val type = Character.getType(codePoint).toByte()
        val isNonSpaceWhitespace =
            (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) && codePoint != ' '.code
        type == Character.FORMAT ||
            type == Character.NON_SPACING_MARK ||
            type == Character.COMBINING_SPACING_MARK ||
            type == Character.ENCLOSING_MARK ||
            Character.isISOControl(codePoint) ||
            isNonSpaceWhitespace ||
            !Character.isDefined(codePoint) ||
            !Character.isValidCodePoint(codePoint)
    }

enum class MediaCheckResult {
    EMPTY,
    NOT_FOUND,
    CONTENT_TYPE_MISMATCH,
    SUCCESS,
    DIMENSION_MISMATCH,
}

enum class StringCheckResult {
    NULL,
    EMPTY,
    SUCCESS,
    RANGE_MISMATCH,
    CONTAIN_INVALID_CHAR,
}

suspend fun Backend.checkIcon(
    icon: PrimaryKey?,
    aspectRatio: Dimension? = null,
): CustomResult<MediaCheckResult?, UserError> {
    if (icon == null) {
        return CustomResult.success(MediaCheckResult.EMPTY)
    }
    return database.file.getFileRecordByIds(listOf(icon)).mapPresent { fileRecords ->
        val mediaInfo = fileRecords.firstOrNull()
        val dimension = mediaInfo?.dimension
        when {
            mediaInfo == null -> MediaCheckResult.NOT_FOUND

            !mediaInfo.contentType.startsWith("image/") -> MediaCheckResult.CONTENT_TYPE_MISMATCH

            aspectRatio != null && (
                dimension == null ||
                    !checkMediaFileDimensionRatioMatch(
                        dimension,
                        aspectRatio,
                    )
                ) -> MediaCheckResult.DIMENSION_MISMATCH

            else -> MediaCheckResult.SUCCESS
        }
    }.mapError(UserError::fromError)
}

suspend fun Backend.addReadLog(uid: PrimaryKey, tuple: UpdateUserRead): CustomResult<Unit?, UserError> =
    checkRootReadPermission(
        tuple.objectTuple.objectType,
        tuple.objectTuple.objectId,
        uid,
    ).andThenPresent(UserError::fromError) { topic ->
        if (topic.hasRead) {
            database.user.addReadLog(
                UserTopicRead(uid, now(), tuple.objectTuple.objectId, tuple.objectTuple.objectType, tuple.topicId),
            )
        } else {
            CustomResult.Failure(UserError.from(ForbiddenException("Permission denied")))
        }
    }.mapError(UserError::fromError)

suspend fun Backend.addChildAccount(
    uid: PrimaryKey,
    request: AddChildAccountRequest,
): CustomResult<ChildAccountInfo, UserError> =
    database.user.getRawChildAccount(uid).andThen(UserError::fromError) { childAccount ->
        if (childAccount != null) {
            CustomResult.Failure(UserError.from(CustomBadRequestException("child account can't create child account")))
        } else {
            captureFailure(UserError::from) {
                // Get user's public key based on algorithm type
                // Generate new user account with the public key
                val address = getAlgo(request.algoType).calcAddress(request.derPublicKey).getOrThrow()
                val id = SnowflakeFactory.nextId()
                val notificationId = SnowflakeFactory.nextId()
                val user =
                    User(
                        null,
                        request.encryptionPublicKey,
                        request.derPublicKey,
                        address,
                        null,
                        nameService.parse(id),
                        id,
                        now(),
                        0,
                        PassType.RAW,
                        request.algoType,
                        notificationId,
                    )
                database.user.createChildAccount(
                    uid,
                    request.encryptedPrivateKey,
                    request.encryptedAesKey,
                    user,
                    request.encryptedEncryptionPrivateKey,
                ).getOrThrow()
                userSearchService.saveDocument(listOf(UserDocument.fromUser(user)))
                    .onFailure { throwable ->
                        Napier.e(throwable.cause) {
                            "save user document failed"
                        }
                    }
                addUserLog(uid, UserLogType.ADD_ALTERNATIVE_ACCOUNT, id ob ObjectType.USER)
                ChildAccountInfo(
                    hostId = uid,
                    encryptedPrivateKey = request.encryptedPrivateKey,
                    encryptedAesKey = request.encryptedAesKey,
                    userInfo = user.toUserInfo(),
                    algoType = request.algoType,
                    encryptedEncryptionPrivateKey = request.encryptedEncryptionPrivateKey,
                )
            }
        }
    }.mapError(UserError::fromError)

suspend fun Backend.addUserLog(
    uid: PrimaryKey,
    type: UserLogType,
    objectTuple: ObjectTuple,
): CustomResult<Unit, UserError> {
    val logId = SnowflakeFactory.nextId()
    val log = UserLog(logId, now(), uid, type, objectTuple.objectId, objectTuple.objectType)
    return database.user.insertUserLog(log).onFailure {
        Napier.i(tag = "user log", throwable = it.cause) {
            "add failed"
        }
    }.mapError(UserError::fromError)
}

suspend fun Backend.addDevice(uid: PrimaryKey, newDevice: NewDevice): CustomResult<Unit, UserError> =
    database.user.addDevice(uid, newDevice.endpointUrl).recover(UserError::fromError) { failure ->
        if (failure is ConstraintFailure) {
            CustomResult.Success(Unit)
        } else {
            CustomResult.Failure(failure)
        }
    }.mapError(UserError::fromError)

suspend fun Backend.getUserAlternateUserInfoList(
    uid: PrimaryKey,
    fetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<ChildAccountInfo>?, UserError> =
    database.user.getRawChildAccountPaginationListByHost(
        uid,
        fetch,
    ).mapPagingResultNotNull(UserError::fromError) { results ->
        processRawUserToUserInfo(
            results.map {
                it.rawUser
            },
        ).map { userList ->
            val userInfoMap = userList.associateBy { it.id }
            val unreadRoomMap = database.container.getUsersHasUnreadRoomMap(userInfoMap.keys.toList()).getOrThrow()
            results.mapNotNull {
                userInfoMap[it.rawUser.user.id]?.let { userInfo ->
                    ChildAccountInfo(
                        hostId = it.childAccount.hostId,
                        encryptedPrivateKey = it.childAccount.encryptedPrivateKey,
                        encryptedAesKey = it.childAccount.encryptedAesKey,
                        userInfo = userInfo,
                        algoType = it.rawUser.user.algoType,
                        encryptedEncryptionPrivateKey = it.childAccount.encryptedEncryptionPrivateKey,
                        hasUnreadRoomMessage = unreadRoomMap[userInfo.id] == true,
                    )
                }
            }
        }
    }.mapError(UserError::fromError)

suspend fun Backend.isKeyVerified(
    roomId: PrimaryKey,
    encryptedAes: Map<PrimaryKey, String>,
): CustomResult<Boolean, UserError> =
    database.container.getJoinedUserList(
        roomId,
    ).map { value ->
        value.map {
            it.uid
        }.toSet().minus(encryptedAes.keys).isEmpty()
    }.mapError(UserError::fromError)

/**
 * 搜索 room/community 的成员列表.
 * @param objectId 容器 ID（room 或 community）
 * @param word 搜索关键字，可选
 * @param primaryKeyFetch 分页范围
 * @return 返回 MemberInfo 列表，包含成员关系信息
 */
suspend fun Backend.searchContainerMembers(
    objectId: PrimaryKey,
    word: String,
    primaryKeyFetch: OffsetFetch,
): CustomResult<PaginationResult<MemberInfo>, UserError> {
    if (word.isBlank()) {
        return CustomResult.success(PaginationResult(emptyList(), 0))
    }
    val result =
        memberSearchService.searchDocument(
            MemberDocumentSearch.Keyword(objectId = objectId, nickname = word, fetch = primaryKeyFetch),
        ).mapPagingResultNotNull(UserError::fromError) { searchResults ->
            val uidList = searchResults.map { it.uid }
            database.container.getMemberWithUserByUids(objectId, uidList)
        }

    return result.mapPagingResultNotNull(UserError::fromError) { list ->
        val rawUsers = list.map { it.second }
        processRawUserToUserInfo(rawUsers).map { users ->
            val userMap = users.associateBy { it.id }
            list.map { (member, rawUser) ->
                MemberInfo(
                    id = member.id,
                    uid = member.uid,
                    objectId = member.objectId,
                    objectType = member.objectType,
                    status = member.status,
                    joinedTime = member.joinedTime,
                    invitedTime = member.invitedTime,
                    userInfo = userMap.getValue(rawUser.user.id),
                )
            }
        }
    }.mapError(UserError::fromError)
}

/**
 * 搜索用户.
 * @param word 搜索关键字
 * @param uid 可选的用户 ID
 * @param primaryKeyFetch 分页范围
 * @return 返回 UserInfo 列表
 */
suspend fun Backend.searchUsers(
    word: String?,
    uid: PrimaryKey?,
    primaryKeyFetch: OffsetFetch,
): CustomResult<PaginationResult<UserInfo>?, UserError> {
    if (word.isNullOrBlank()) {
        return CustomResult.success(null)
    }
    return userSearchService.searchDocument(
        UserDocumentSearch.Keyword(word, fetch = primaryKeyFetch),
    ).andThen(UserError::fromError) { (list, total) ->
        database.user.getRawUsers(
            ObjectListFetch.IdListFetch(
                list.map {
                    it.id
                },
            ),
            uid,
        ).andThen(UserError::fromError) {
            processRawUserToUserInfo(it)
        }.pagingNotNull(total)
    }.mapError(UserError::fromError)
}

suspend fun Backend.getUserInfoList(listFetch: ObjectListFetch) =
    database.user.getRawUsers(listFetch).andThen(UserError::fromError) {
        processRawUserToUserInfo(it)
    }

suspend fun Backend.getUserInfo(fetch: ObjectFetch, uid: PrimaryKey? = null) =
    database.user.getRawUser(fetch, uid).andThenPresent(UserError::fromError) {
        processRawUserToUserInfo(listOf(it)).mapPresent(List<UserInfo>::first)
    }

suspend fun Backend.processRawUserToUserInfo(rawResults: List<RawUser>) =
    database.file.getFileRecordByIds(
        rawResults.mapNotNull {
            it.user.icon
        },
    ).andThen(UserError::fromError) { medias ->
        processFileRecordToFileInfo(medias).map { list ->
            val mediaInfoMap = list.associateBy { it.id }
            rawResults.map { rawUser ->
                rawUser.toUserInfo(avatar = rawUser.user.icon?.let { mediaInfoMap[it] })
            }
        }
    }

suspend fun Backend.getUserOverview(uid: PrimaryKey) =
    database.getUserOverview(uid).andThen(UserError::fromError) {
        processRawUserOverviewToUserOverview(it)
    }

suspend fun Backend.processRawUserOverviewToUserOverview(raw: RawUserOverview): CustomResult<UserOverview, UserError> =
    processRawUserToUserInfo(
        listOf(raw.rawUser),
    ).map { users ->
        val userInfo = users.first()
        UserOverview(
            raw.subscriptionCount,
            raw.favoriteCount,
            raw.acg,
            raw.childAccountCount,
            raw.reactionRecordCount,
            raw.commentCount,
            raw.hasUnreadChildRoomMessage,
            userInfo,
        )
    }.mapError(UserError::fromError)

suspend fun Backend.getAllUsers(primaryKeyFetch: PrimaryKeyFetch) =
    database.user.getAllUsers(primaryKeyFetch).mapPagingResultNotNull(UserError::fromError) { list ->
        processRawUserToUserInfo(list)
    }

suspend fun Backend.getUserById(id: PrimaryKey) = getUserInfo(ObjectFetch.IdFetch(id))

suspend fun Backend.getUserLogs(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
    database.user.getUserLogs(uid, fetch).andThen(UserError::fromError) { (list, total) ->
        buildUserLogItems(list, uid).pagingNotNull(total)
    }

private suspend fun Backend.buildUserLogItems(list: List<UserLog>, uid: PrimaryKey) =
    captureFailure(UserError::from) {
        val userIds = mutableListOf<PrimaryKey>()
        val communityIds = mutableListOf<PrimaryKey>()
        val roomIds = mutableListOf<PrimaryKey>()
        val topicIds = mutableListOf<PrimaryKey>()
        list.forEach { log ->
            when (log.objectType) {
                ObjectType.USER -> userIds += log.objectId
                ObjectType.COMMUNITY -> communityIds += log.objectId
                ObjectType.ROOM -> roomIds += log.objectId
                ObjectType.TOPIC -> topicIds += log.objectId
                else -> {}
            }
        }
        val users = getUserInfoList(ObjectListFetch.IdListFetch(userIds)).getOrThrow()
        val communities =
            database.community.getRawCommunities(ObjectListFetch.IdListFetch(communityIds))
                .andThen(UserError::fromError) { processRawCommunityToCommunityInfo(it) }.getOrThrow()
                .orEmpty()
        val rooms = getRoomInfoList(ObjectListFetch.IdListFetch(roomIds)).getOrThrow()
        val topics = getTopicByIds(topicIds, uid).getOrThrow().orEmpty()
        val userMap = users.associateBy { it.id }
        val communityMap = communities.associateBy { it.id }
        val roomMap = rooms.associateBy { it.id }
        val topicMap = topics.associateBy { it.id }
        list.map { log ->
            val projection =
                when (log.objectType) {
                    ObjectType.USER -> UserLogItem(log.toUserLogInfo(), user = userMap[log.objectId])
                    ObjectType.COMMUNITY -> UserLogItem(log.toUserLogInfo(), community = communityMap[log.objectId])
                    ObjectType.ROOM -> UserLogItem(log.toUserLogInfo(), room = roomMap[log.objectId])
                    ObjectType.TOPIC -> UserLogItem(log.toUserLogInfo(), topic = topicMap[log.objectId])
                    else -> null
                }
            projection ?: UserLogItem(log.toUserLogInfo())
        }
    }

fun Char.isEnglishLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

suspend fun Backend.getUserReactions(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
    database.reaction.getUserReactionRecordsPaginationResult(
        uid,
        fetch,
    ).mapPagingResultNotNull(UserError::fromError) { list ->
        CustomResult.success(
            list.map { record ->
                ReactionRecordInfo(
                    record.id,
                    record.emoji,
                    record.objectId,
                    record.objectType,
                    record.createdTime,
                    record.uid,
                )
            },
        )
    }

suspend fun Backend.getUserCommentedTopics(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
    database.topic.getUserCommentedTopicsPaginationResult(uid, fetch).andThen(UserError::fromError) { (list, total) ->
        processTopicActivities(
            list.map { topic ->
                database.processTopicToRawTopic(uid, listOf(topic)).getOrThrow().first()
            },
            uid,
        ).paging(total)
    }

suspend fun Backend.hasUnreadRooms(uid: PrimaryKey): CustomResult<UnreadRoomsResponse, UserError> {
    val unreadCount = database.container.getUserUnreadRoomCount(uid).getOrThrow()
    return CustomResult.success(UnreadRoomsResponse(hasUnread = unreadCount > 0, unreadCount = unreadCount))
}
