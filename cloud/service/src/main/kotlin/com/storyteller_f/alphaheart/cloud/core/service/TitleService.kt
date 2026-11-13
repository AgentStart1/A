/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.mapPagingResultIfNotNullNullable
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.RawTitle
import com.storyteller_f.alphaheart.backend.core.types.Title
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.backend.core.types.isExpired
import com.storyteller_f.alphaheart.backend.core.types.toTitleInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.TitleError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import io.github.aakira.napier.Napier

suspend fun Backend.getUserTitles(
    uid: PrimaryKey,
    searchType: TitleSearchType,
    type: TitleType? = null,
    scopeId: PrimaryKey? = null,
    titleStatus: TitleWorkStatus? = null,
    fetch: PrimaryKeyFetch,
) = database.title.getTitlePaginationResult(
    fetch,
    uid,
    searchType,
    type,
    scopeId,
    titleStatus,
).mapPagingResultIfNotNullNullable(TitleError::fromError) { list ->
    processTitleList(list, uid)
}

private suspend fun Backend.processTitleList(
    list: List<RawTitle>,
    uid: PrimaryKey?,
): CustomResult<List<TitleDetail>?, TitleError> {
    val titleIds = list.map { it.title.id }
    val favoriteMap =
        if (uid != null && titleIds.isNotEmpty()) {
            database.favorite.getHasFavorite(ObjectListFetch.IdListFetch(titleIds), uid)
                .getOrNull()?.associateBy { it.objectId }.orEmpty()
        } else {
            emptyMap()
        }
    val subscriptionMap =
        if (uid != null && titleIds.isNotEmpty()) {
            database.subscription.getHasSubscription(ObjectListFetch.IdListFetch(titleIds), uid)
                .getOrNull()?.associateBy { it.objectId }.orEmpty()
        } else {
            emptyMap()
        }
    val listWithFavAndSub =
        list.map { rawTitle ->
            rawTitle.copy(
                favoriteId = favoriteMap[rawTitle.title.id]?.id,
                subscriptionId = subscriptionMap[rawTitle.title.id]?.id,
            )
        }
    val uidList = getUidList(list)
    val communityIdList = getCommunityIdList(list)
    val roomIdList = getRoomIdList(list)

    return getRelatedObject(uidList, communityIdList, roomIdList).andThen(TitleError::fromError) {
        val topicIdList =
            list.flatMap {
                val title = it.title
                buildList {
                    if (title.scopeType == ObjectType.TOPIC) {
                        add(title.scopeId)
                    }
                    if (title.descriptionTopicId != 0L) {
                        add(title.descriptionTopicId)
                    }
                }
            }.distinct()
        getTopicByIds(topicIdList, uid).mapPresent { topicList ->
            processTitleList(
                it.first.orEmpty(),
                it.third.orEmpty(),
                it.second.orEmpty(),
                listWithFavAndSub,
                topicList,
            )
        }
    }.mapError(TitleError::fromError)
}

private fun getUidList(list: List<RawTitle>): List<PrimaryKey> =
    list.flatMap {
        val title = it.title
        val listOf = listOf(title.receiver, title.creator)
        if (title.scopeType == ObjectType.USER) {
            listOf + title.scopeId
        } else {
            listOf
        }
    }.distinct()

private fun getCommunityIdList(list: List<RawTitle>): List<Long> =
    list.mapNotNull {
        val title = it.title
        if (title.scopeType == ObjectType.COMMUNITY) {
            title.scopeId
        } else {
            null
        }
    }.distinct()

private fun getRoomIdList(list: List<RawTitle>): List<Long> =
    list.mapNotNull {
        val title = it.title
        if (title.scopeType == ObjectType.ROOM) {
            title.scopeId
        } else {
            null
        }
    }.distinct()

private suspend fun Backend.getRelatedObject(
    uidList: List<PrimaryKey>,
    communityIdList: List<PrimaryKey>,
    roomIdList: List<PrimaryKey>,
): CustomResult<Triple<List<UserInfo>?, List<RoomInfo>?, List<CommunityInfo>?>, TitleError> =
    captureFailure(TitleError::from) {
        val r1 = getUserInfoList(ObjectListFetch.IdListFetch(uidList)).getOrThrow()
        val r2 = getRoomInfoList(ObjectListFetch.IdListFetch(roomIdList)).getOrThrow()
        val r3 =
            database.community.getRawCommunities(
                ObjectListFetch.IdListFetch(communityIdList),
            ).andThen(TitleError::fromError) {
                processRawCommunityToCommunityInfo(it)
            }.getOrThrow()
        Triple(r1, r2, r3)
    }.mapError(TitleError::fromError)

private fun processTitleList(
    userList: List<UserInfo>,
    communityList: List<CommunityInfo>,
    roomList: List<RoomInfo>,
    list: List<RawTitle>,
    topicList: List<TopicDetail>,
): List<TitleDetail> {
    val userMap = userList.associateBy { it.id }
    val communityMap = communityList.associateBy { it.id }
    val roomMap = roomList.associateBy { it.id }
    val topicMap = topicList.associateBy { it.id }
    val currentTime = now()
    return list.map { rawTitle ->
        val title = rawTitle.title
        val projection =
            TitleDetail(
                title.toTitleInfo(rawTitle.favoriteId, rawTitle.subscriptionId, currentTime),
                userMap.getValue(title.creator),
                userMap.getValue(title.receiver),
                topicMap.getValue(title.descriptionTopicId),
                when (title.scopeType) {
                    ObjectType.COMMUNITY -> communityMap[title.scopeId]
                    else -> null
                },
                when (title.scopeType) {
                    ObjectType.ROOM -> roomMap[title.scopeId]
                    else -> null
                },
                when (title.scopeType) {
                    ObjectType.USER -> userMap[title.scopeId]
                    else -> null
                },
                when (title.scopeType) {
                    ObjectType.TOPIC -> topicMap[title.scopeId]
                    else -> null
                },
            )
        projection
    }
}

suspend fun Backend.getAllTitles(
    primaryKeyFetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<TitleDetail>, TitleError> =
    database.title.getAllRawTitles(primaryKeyFetch).mapPagingResultNotNull(TitleError::fromError) { list ->
        processTitleList(list, null).map { it.orEmpty() }
    }.mapError(TitleError::fromError)

suspend fun Backend.getTitleInfo(id: PrimaryKey): CustomResult<TitleDetail?, TitleError> =
    database.title.getTitle(id).andThenPresent(TitleError::fromError) { raw ->
        processTitleList(listOf(raw), null).map { it?.firstOrNull() }
    }.mapError(TitleError::fromError)

suspend fun Backend.createTitle(newTitle: NewTitle, uid: PrimaryKey) =
    checkRootAdminPermission(newTitle.scopeType, newTitle.scopeId, uid).andThenPresent(TitleError::fromError) {
        val title = toTitle(newTitle, uid)
        val topic =
            Topic(
                title.descriptionTopicId,
                now(),
                uid,
                title.id,
                ObjectType.TITLE,
                title.id,
                ObjectType.TITLE,
                newTitle.description.encodeToByteArray(),
                isEncrypted = false,
                level = 1,
                isPin = false,
                lastModifiedTime = null,
            )
        database.topic.createTitle(title, topic).onSuccess {
            // 如果发送的title 类型是JOIN，插入一条Member 记录
            inviteMemberIfNeed(title)
        }.andThen(TitleError::fromError) {
            addUserLog(uid, UserLogType.CREATE, ObjectTuple(title.id, ObjectType.TITLE))
            processTitleList(listOf(RawTitle(title)), uid).mapPresent {
                it.first()
            }
        }
    }

private suspend fun Backend.inviteMemberIfNeed(title: Title) {
    if (title.type == TitleType.JOIN && !title.isExpired()) {
        val memberId = SnowflakeFactory.nextId()
        val invitedTime = now()
        database.container.addMember(
            Member(
                memberId,
                title.receiver,
                title.scopeId,
                title.scopeType,
                invitedTime,
                MemberStatus.INVITED,
                null,
                invitedTime,
            ),
        ).onFailure {
            Napier.e(it.cause) {
                "create join title member failed $it"
            }
        }
    }
}

private suspend fun toTitle(newTitle: NewTitle, id: PrimaryKey): Title {
    val titleId = SnowflakeFactory.nextId()
    val descriptionTopicId = SnowflakeFactory.nextId()
    return Title(
        titleId,
        now(),
        newTitle.name,
        id,
        newTitle.receiver,
        newTitle.type,
        newTitle.scopeId,
        newTitle.scopeType,
        TitleWorkStatus.OK,
        descriptionTopicId,
        newTitle.expiresAt,
    )
}

/** Title types supported for each object scope. */
val titleMap: MutableMap<ObjectType, List<TitleType>> =
    mutableMapOf(
        ObjectType.COMMUNITY to listOf(TitleType.REGULAR, TitleType.JOIN),
        ObjectType.ROOM to listOf(TitleType.REGULAR, TitleType.JOIN),
        ObjectType.USER to listOf(TitleType.REGULAR),
        ObjectType.TOPIC to listOf(TitleType.REGULAR),
    )

suspend fun createTitle(title: NewTitle, backend: Backend, uid: PrimaryKey): CustomResult<TitleDetail?, TitleError> {
    val supportType = titleMap[title.scopeType] ?: return CustomResult.success(null)
    if (!supportType.contains(title.type)) {
        return CustomResult.Failure(
            TitleError.from(ForbiddenException("unsupported title type ${title.type} in ${title.scopeType}")),
        ).mapError(TitleError::fromError)
    }
    return backend.createTitle(title, uid).mapError(TitleError::fromError)
}
