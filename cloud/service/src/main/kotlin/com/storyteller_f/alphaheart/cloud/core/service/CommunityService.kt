/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.COMMUNITY_NAME_LENGTH
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.JoinSearch
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.mapPagingResultIfNotNullNullable
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.paging
import com.storyteller_f.alphaheart.backend.core.service.CommunityDocument
import com.storyteller_f.alphaheart.backend.core.service.CommunityDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.MemberDocument
import com.storyteller_f.alphaheart.backend.core.service.MemberDocumentSearch
import com.storyteller_f.alphaheart.backend.core.types.Community
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.RawCommunity
import com.storyteller_f.alphaheart.backend.core.types.toCommunityIfo
import com.storyteller_f.alphaheart.backend.core.types.toMemberInfo
import com.storyteller_f.alphaheart.backend.core.types.toNestedMemberInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.CommunityError
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
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.CommunityMembershipItem
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FontSettingsWithInfo
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.MemberPolicy
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.obj.UpdateCommunityBody
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import io.github.aakira.napier.Napier

suspend fun Backend.getCommunity(objectFetch: ObjectFetch, id: PrimaryKey?, fillJoinInfo: Boolean?) =
    database.community.getRawCommunity(objectFetch, fillJoinInfo, id).andThenPresent(CommunityError::fromError) {
        processRawCommunityToCommunityInfo(listOf(it)).mapPresent(List<CommunityInfo>::first)
    }

suspend fun Backend.joinCommunity(uid: PrimaryKey, communityId: PrimaryKey) =
    getCommunity(ObjectFetch.IdFetch(communityId), uid, true).andThenPresent(CommunityError::fromError) { community ->
        val joinedMember = community.member?.takeIf { it.status == MemberStatus.JOINED }
        if (joinedMember != null) {
            CustomResult.success(community)
        } else {
            if (community.memberPolicy == MemberPolicy.INVITE_ONLY) {
                getJoinTitleByScope(uid, communityId)
            } else {
                CustomResult.Success(Unit)
            }.andThen(CommunityError::fromError) {
                database.container.getMember(communityId, uid).andThen(CommunityError::fromError) { member ->
                    joinCommunity(uid, communityId, community, member)
                }
            }
        }
    }

private suspend fun Backend.joinCommunity(
    uid: PrimaryKey,
    communityId: PrimaryKey,
    communityInfo: CommunityInfo,
    currentMember: Member?,
): CustomResult<CommunityInfo?, CommunityError> {
    val time = now()
    val member =
        if (currentMember == null) {
            Member(
                SnowflakeFactory.nextId(),
                uid,
                communityId,
                ObjectType.COMMUNITY,
                time,
                MemberStatus.JOINED,
                time,
            )
        } else {
            Member(
                currentMember.id,
                currentMember.uid,
                currentMember.objectId,
                currentMember.objectType,
                time,
                MemberStatus.JOINED,
                time,
                currentMember.invitedTime,
            )
        }
    val joinResult =
        if (currentMember == null) {
            database.container.addMember(member)
        } else {
            database.container.updateMemberStatus(member)
        }
    return joinResult.onSuccess {
        addUserLog(uid, UserLogType.JOIN, communityId ob ObjectType.COMMUNITY)
        saveMemberDocument(uid, member.id, communityId, communityInfo.name)
    }.andThen(CommunityError::fromError) {
        CustomResult.success(communityInfo.copy(member = member.toNestedMemberInfo()))
    }.recover(CommunityError::fromError) { failure ->
        if (failure is ConstraintFailure) {
            getCommunity(ObjectFetch.IdFetch(communityId), uid, true)
        } else {
            CustomResult.Failure(failure)
        }
    }.mapError(CommunityError::fromError)
}

suspend fun Backend.exitCommunity(communityId: PrimaryKey, id: PrimaryKey) =
    getCommunity(ObjectFetch.IdFetch(communityId), id, true).andThenPresent(CommunityError::fromError) { info ->
        if (info.member == null) {
            CustomResult.success(info)
        } else {
            database.container.deleteMember(communityId, id).onSuccess {
                addUserLog(id, UserLogType.EXIT, communityId ob ObjectType.COMMUNITY)
                // 从 MemberSearchService 删除
                memberSearchService.deleteDocument(id, communityId).onFailure { e ->
                    Napier.e(e.cause) {
                        "delete member document failed"
                    }
                }
            }.andThen(CommunityError::fromError) {
                CustomResult.success(info.copy(member = null))
            }
        }
    }

suspend fun Backend.searchCommunities(
    uid: PrimaryKey?,
    search: CustomApi.Communities.CommunitySearchQuery,
    primaryKeyFetch: OffsetFetch,
): CustomResult<PaginationResult<CommunityMembershipItem>?, CommunityError> {
    val word = search.word.trim()
    if (word.isBlank()) {
        return CustomResult.success(null)
    }
    val joinSearch =
        if (search.target != null) {
            JoinStatusSearch.JOINED.toJoinSearch(search.target)
        } else {
            search.joinStatus.toJoinSearch(uid)
        }
    // word 不为空 && 搜索已加入的社区，使用 memberSearchService
    return if (joinSearch is JoinSearch.Joined) {
        memberSearchService.searchDocument(
            MemberDocumentSearch.CommunityMembers(uid = joinSearch.uid, objectName = word, fetch = primaryKeyFetch),
        ).mapPagingResultNotNull(CommunityError::fromError) { searchResults ->
            val communityIds =
                searchResults
                    .map { it.objectId }
            database.community.getRawCommunities(ObjectListFetch.IdListFetch(communityIds))
        }
    } else {
        // word 不为空 && 不是搜索已加入（Unspecified），使用 communitySearchService
        communitySearchService.searchDocument(
            CommunityDocumentSearch.Keyword(word, fetch = primaryKeyFetch),
        ).mapPagingResultNotNull(CommunityError::fromError) { list ->
            database.community.getRawCommunities(
                ObjectListFetch.IdListFetch(
                    list.map {
                        it.id
                    },
                ),
            )
        }
    }.mapPagingResultIfNotNullNullable(CommunityError::fromError) { communities ->
        processCommunities(communities, uid, search.target)
    }.mapError(CommunityError::fromError)
}

suspend fun Backend.searchUserJoinedCommunities(
    uid: PrimaryKey,
    search: CustomApi.Users.JoinedCommunities.UserCommunitiesSearchQuery,
    primaryKeyFetch: OffsetFetch,
): CustomResult<PaginationResult<CommunityMembershipItem>?, CommunityError> {
    val word = search.word.trim()
    if (word.isBlank()) {
        return CustomResult.success(null)
    }
    return memberSearchService.searchDocument(
        MemberDocumentSearch.CommunityMembers(uid = uid, objectName = word, fetch = primaryKeyFetch),
    ).mapPagingResultNotNull(CommunityError::fromError) { searchResults ->
        val communityIds =
            searchResults
                .map { it.objectId }
        database.community.getRawCommunities(ObjectListFetch.IdListFetch(communityIds))
    }.mapPagingResultIfNotNullNullable(CommunityError::fromError) { communities ->
        processCommunities(communities, uid, null)
    }.mapError(CommunityError::fromError)
}

private suspend fun Backend.processCommunities(
    communities: List<RawCommunity>,
    uid: PrimaryKey?,
    target: PrimaryKey?,
): CustomResult<List<CommunityMembershipItem>?, CommunityError> =
    processRawCommunityToCommunityInfo(communities).andThenPresent(CommunityError::fromError) { value ->
        when {
            target == null -> CustomResult.success(value.map { CommunityMembershipItem(it, null) })

            uid != null -> processUserJoinedTimeReplace(value, uid)

            else ->
                CustomResult.success(
                    value.map {
                        CommunityMembershipItem(it.copy(member = null), it.member)
                    },
                )
        }
    }.mapError(CommunityError::fromError)

private suspend fun Backend.processUserJoinedTimeReplace(
    communityInfos: List<CommunityInfo>,
    uid: PrimaryKey,
): CustomResult<List<CommunityMembershipItem>, CommunityError> {
    val communityIds =
        communityInfos.map {
            it.id
        }
    return database.container.getMemberByIds(uid, communityIds).map { joinedTimeList ->
        val map = joinedTimeList.associate { it }
        communityInfos.map {
            CommunityMembershipItem(it.copy(member = map[it.id]), it.member)
        }
    }.mapError(CommunityError::fromError)
}

suspend fun Backend.createCommunity(
    newCommunity: NewCommunity,
    uid: PrimaryKey,
): CustomResult<CommunityInfo?, CommunityError> =
    captureFailure(CommunityError::from) {
        checkAid(newCommunity.aid).getOrThrow()
        checkCommunityName(newCommunity).getOrThrow()
    }.andThen(CommunityError::fromError) {
        val id = SnowflakeFactory.nextId()
        val memberId = SnowflakeFactory.nextId()
        val community =
            Community(
                id,
                now(),
                newCommunity.aid,
                newCommunity.name,
                uid,
                newCommunity.memberPolicy,
                newCommunity.icon,
                null,
            )
        database.community.createCommunity(community, memberId).onSuccess {
            communitySearchService.saveDocument(listOf(CommunityDocument.fromCommunity(community)))
                .onFailure {
                    Napier.e(it.cause) {
                        "save community document failed"
                    }
                }
            addUserLog(uid, UserLogType.CREATE, community.id ob ObjectType.COMMUNITY)
            // 保存创建者到 MemberSearchService
            saveMemberDocument(uid, memberId, id, newCommunity.name)
        }
    }.andThen(CommunityError::fromError) { (community, member) ->
        val rawCommunity = RawCommunity(community, member, null, 0)
        processRawCommunityToCommunityInfo(listOf(rawCommunity))
    }.firstOrNull().mapError(CommunityError::fromError)

private suspend fun Backend.saveMemberDocument(uid: PrimaryKey, memberId: PrimaryKey, id: PrimaryKey, name: String) {
    getUserInfo(ObjectFetch.IdFetch(uid)).onPresent { userInfo ->
        memberSearchService.saveDocument(
            listOf(MemberDocument.fromUserInfo(memberId, userInfo, id, ObjectType.COMMUNITY, name)),
        ).onFailure { e ->
            Napier.e(e.cause) {
                "save member document failed"
            }
        }
    }
}

private fun checkCommunityName(newCommunity: NewCommunity): CustomResult<Unit, CommunityError> =
    when (
        checkNickname(
            newCommunity.name,
            1..COMMUNITY_NAME_LENGTH,
        )
    ) {
        StringCheckResult.RANGE_MISMATCH ->
            CustomResult.Failure(
                CommunityError.from(
                    CustomBadRequestException("community name must be between in 1 and $COMMUNITY_NAME_LENGTH"),
                ),
            )

        StringCheckResult.CONTAIN_INVALID_CHAR ->
            CustomResult.Failure(
                CommunityError.from(
                    CustomBadRequestException("community name must be visible"),
                ),
            )

        StringCheckResult.SUCCESS -> CustomResult.success(Unit)

        StringCheckResult.NULL, StringCheckResult.EMPTY ->
            CustomResult.Failure(CommunityError.from(CustomBadRequestException("name must be set")))
    }.mapError(CommunityError::fromError)

suspend fun Backend.updateCommunity(
    id: PrimaryKey,
    old: UpdateCommunityBody,
    uid: PrimaryKey,
): CustomResult<CommunityInfo?, CommunityError> {
    val newCommunity =
        old.copy(
            name = old.name?.trim(),
            icon = old.icon,
            poster = old.poster,
            fontSettings = old.fontSettings,
        )
    return checkCommunityAdminPermission(id, uid).andThenPresent(CommunityError::fromError) {
        checkBeforeUpdateCommunity(newCommunity)
    }.andThenPresent(CommunityError::fromError) {
        checkObjectWritable(ObjectType.COMMUNITY, id)
    }.andThenPresent(CommunityError::fromError) {
        database.community.updateCommunity(
            id,
            newCommunity,
        ).requireTrue(CommunityError::fromError) {
            CommunityError.from(
                run {
                    CustomBadRequestException("update failed")
                },
            )
        }
    }.andThenPresent(CommunityError::fromError) {
        database.community.getRawCommunity(ObjectFetch.IdFetch(id), true, uid)
    }.andThenPresent(CommunityError::fromError) { rawResult ->
        processRawCommunityToCommunityInfo(listOf(rawResult))
    }.firstOrNull().mapError(CommunityError::fromError)
}

private suspend fun Backend.checkBeforeUpdateCommunity(
    newCommunity: UpdateCommunityBody,
): CustomResult<Unit, CommunityError> {
    captureFailure(CommunityError::from) {
        checkCommunityNameForUpdate(newCommunity).getOrThrow()
        checkCommunityIconForUpdate(newCommunity).getOrThrow()
        checkCommunityPosterForUpdate(newCommunity).getOrThrow()
        checkCommunityFontSettingsForUpdate(newCommunity).getOrThrow()
    }.exceptionOrNull()?.let {
        return CustomResult.Failure(CommunityError.from(it)).mapError(CommunityError::fromError)
    }
    return CustomResult.Success(Unit)
}

private suspend fun Backend.checkCommunityFontSettingsForUpdate(
    newCommunity: UpdateCommunityBody,
): CustomResult<Unit, CommunityError> {
    val fontSettings = newCommunity.fontSettings ?: return CustomResult.Success(Unit)
    val fontIds =
        listOfNotNull(
            fontSettings.contentFontId,
            fontSettings.codeFontId,
        )
    if (fontIds.isEmpty()) return CustomResult.Success(Unit)
    return database.file.getFileRecordByIds(fontIds).andThen(CommunityError::fromError) { files ->
        val foundIds = files.map { it.id }.toSet()
        val missingId = fontIds.firstOrNull { it !in foundIds }
        if (missingId != null) {
            CustomResult.Failure(CommunityError.from(CustomBadRequestException("font file not found: $missingId")))
        } else {
            val invalidFile =
                files.firstOrNull {
                    !it.contentType.startsWith("font/") && !it.contentType.startsWith("application/font-")
                }
            if (invalidFile != null) {
                CustomResult.Failure(
                    CommunityError.from(
                        CustomBadRequestException("invalid font content type: ${invalidFile.contentType}"),
                    ),
                )
            } else {
                CustomResult.Success(Unit)
            }
        }
    }.mapError(CommunityError::fromError)
}

private suspend fun Backend.checkCommunityPosterForUpdate(
    newCommunity: UpdateCommunityBody,
): CustomResult<Unit, CommunityError> =
    checkIcon(newCommunity.poster, Dimension.COMMUNITY_POSTER).andThen(CommunityError::fromError) { checkResult ->
        when (checkResult) {
            MediaCheckResult.NOT_FOUND ->
                CustomResult.Failure(
                    CommunityError.from(CustomBadRequestException("poster not found")),
                )

            MediaCheckResult.CONTENT_TYPE_MISMATCH ->
                CustomResult.Failure(
                    CommunityError.from(CustomBadRequestException("only support image")),
                )

            MediaCheckResult.DIMENSION_MISMATCH ->
                CustomResult.Failure(
                    CommunityError.from(CustomBadRequestException("dimension mismatch")),
                )

            MediaCheckResult.EMPTY, MediaCheckResult.SUCCESS, null -> CustomResult.Success(Unit)
        }
    }.mapError(CommunityError::fromError)

private suspend fun Backend.checkCommunityIconForUpdate(
    newCommunity: UpdateCommunityBody,
): CustomResult<Unit, CommunityError> =
    checkIcon(newCommunity.icon, Dimension.DEFAULT_DIMENSION).andThen(CommunityError::fromError) { checkResult ->
        when (checkResult) {
            MediaCheckResult.NOT_FOUND ->
                CustomResult.Failure(
                    CommunityError.from(CustomBadRequestException("icon not found")),
                )

            MediaCheckResult.CONTENT_TYPE_MISMATCH ->
                CustomResult.Failure(
                    CommunityError.from(CustomBadRequestException("only support image")),
                )

            MediaCheckResult.DIMENSION_MISMATCH ->
                CustomResult.Failure(
                    CommunityError.from(CustomBadRequestException("dimension mismatch")),
                )

            MediaCheckResult.EMPTY, MediaCheckResult.SUCCESS, null -> CustomResult.Success(Unit)
        }
    }.mapError(CommunityError::fromError)

private fun checkCommunityNameForUpdate(newCommunity: UpdateCommunityBody): CustomResult<Unit, CommunityError> =
    when (checkNickname(newCommunity.name, 1..COMMUNITY_NAME_LENGTH)) {
        StringCheckResult.RANGE_MISMATCH ->
            CustomResult.Failure(
                CommunityError.from(
                    CustomBadRequestException("community name must be between in 1 and 20"),
                ),
            )

        StringCheckResult.CONTAIN_INVALID_CHAR ->
            CustomResult.Failure(
                CommunityError.from(
                    CustomBadRequestException("community name must be visible"),
                ),
            )

        StringCheckResult.NULL, StringCheckResult.EMPTY, StringCheckResult.SUCCESS -> CustomResult.Success(Unit)
    }.mapError(CommunityError::fromError)

suspend fun Backend.processRawCommunityToCommunityInfo(
    list: List<RawCommunity>,
): CustomResult<List<CommunityInfo>?, CommunityError> =
    database.file.getFileRecordByIds(
        list.flatMap { (community) ->
            community.fontSettings?.let { fs ->
                listOfNotNull(fs.contentFontId, fs.codeFontId)
            }.orEmpty() + listOf(community.iconId, community.posterId)
        }.filterNotNull(),
    ).andThenPresent(CommunityError::fromError) { medias ->
        processFileRecordToFileInfo(medias).map { mediaList ->
            val map = mediaList.associateBy { it.id }
            list.map { rawResult ->
                val fs = rawResult.community.fontSettings
                val fontSettingsWithInfo =
                    fs?.let {
                        val contentFont = if (it.contentFontId == null) null else map[it.contentFontId]
                        val codeFont = if (it.codeFontId == null) null else map[it.codeFontId]
                        FontSettingsWithInfo(
                            settings = it,
                            contentFont = contentFont,
                            codeFont = codeFont,
                        )
                    }
                rawResult.toCommunityIfo(
                    rawResult.community.iconId?.let { map[it] },
                    rawResult.community.posterId?.let { map[it] },
                    fontSettingsWithInfo,
                )
            }
        }
    }.mapError(CommunityError::fromError)

fun JoinStatusSearch?.toJoinSearch(uid: PrimaryKey?): JoinSearch {
    if (this != JoinStatusSearch.JOINED) return JoinSearch.Unspecified(uid)
    if (uid == null) throw UnauthorizedException()
    return JoinSearch.Joined(uid)
}

suspend fun Backend.getAllCommunities(primaryKeyFetch: PrimaryKeyFetch) =
    database.community.getCommunityPaginationResult(
        hasPosterSearch = PosterSearch.UNSPECIFIED,
        primaryKeyFetch = primaryKeyFetch,
        joinSearch = JoinSearch.Unspecified(null),
    ).andThenPresent(CommunityError::fromError) { (list, total) ->
        processRawCommunityToCommunityInfo(list).paging(total)
    }

suspend fun Backend.getUserJoinedCommunities(
    uid: PrimaryKey?,
    target: PrimaryKey,
    primaryKeyFetch: PrimaryKeyFetch,
    hasPosterSearch: PosterSearch? = null,
): CustomResult<PaginationResult<CommunityMembershipItem>?, CommunityError> =
    database.community.getCommunityPaginationResult(
        hasPosterSearch = hasPosterSearch ?: PosterSearch.UNSPECIFIED,
        primaryKeyFetch = primaryKeyFetch,
        joinSearch = JoinSearch.Joined(target),
    ).mapPagingResultIfNotNullNullable(CommunityError::fromError) {
        processCommunities(it, uid, target)
    }.mapError(CommunityError::fromError)

suspend fun Backend.getCommunityMemberInfos(
    communityId: PrimaryKey,
    primaryKeyFetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<MemberInfo>, CommunityError> =
    getContainerMemberInfos(
        communityId,
        primaryKeyFetch,
    ).mapError(CommunityError::fromError)

suspend fun Backend.getContainerMemberInfos(objectId: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
    database.container.getMemberWithUserPaginationResult(objectId, primaryKeyFetch)
        .mapPagingResultNotNull(CommunityError::fromError) { list ->
            val rawUsers = list.map { it.second }
            processRawUserToUserInfo(rawUsers).map { users ->
                val userMap = users.associateBy { it.id }
                list.map { (member, rawUser) ->
                    member.toMemberInfo(userMap.getValue(rawUser.user.id))
                }
            }
        }
