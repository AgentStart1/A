/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.addIfNotExists
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.types.UserFavorite
import com.storyteller_f.alphaheart.backend.core.types.toUserFavoriteInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.FavoriteError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.firstOrNull
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.UserFavoriteItem
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now

suspend fun Backend.addFavorite(uid: PrimaryKey, newFavorite: NewFavorite) =
    checkObjectWritable(newFavorite.objectType, newFavorite.objectId).andThenPresent(FavoriteError::fromError) {
        addIfNotExists({
            database.favorite.getFavorite(uid, newFavorite.objectId)
        }) {
            val id = SnowflakeFactory.nextId()
            val userFavorite = UserFavorite(id, uid, newFavorite.objectId, newFavorite.objectType, now())
            database.favorite.addFavorite(userFavorite).onSuccess {
                addUserLog(uid, UserLogType.ADD_FAVORITE, newFavorite.tuple())
            }
        }
    }.andThenPresent(FavoriteError::fromError) {
        buildFavoriteItems(uid, listOf(it))
    }.firstOrNull()

suspend fun Backend.deleteFavorite(uid: PrimaryKey, id: PrimaryKey) =
    database.favorite.getFavorite(id).andThenPresent(FavoriteError::fromError) { userFavorite ->
        if (userFavorite.uid == uid) {
            checkObjectWritable(
                userFavorite.objectType,
                userFavorite.objectId,
            ).andThenPresent(FavoriteError::fromError) {
                database.favorite.removeFavorite(id).onSuccess {
                    addUserLog(uid, UserLogType.REMOVE_FAVORITE, userFavorite.objectTuple())
                }
            }
        } else {
            CustomResult.Failure(FavoriteError.from(ForbiddenException()))
        }
    }

suspend fun Backend.deleteFavoriteByObject(uid: PrimaryKey, objectId: PrimaryKey) =
    database.favorite.getFavorite(uid, objectId).andThenPresent(FavoriteError::fromError) { userFavorite ->
        checkObjectWritable(userFavorite.objectType, userFavorite.objectId).andThenPresent(FavoriteError::fromError) {
            database.favorite.removeFavorite(userFavorite.id).onSuccess {
                addUserLog(uid, UserLogType.REMOVE_FAVORITE, userFavorite.objectTuple())
            }
        }
    }

suspend fun Backend.getFavorites(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
    database.favorite.getUserFavorites(uid, fetch).mapPagingResultNotNull(FavoriteError::fromError) {
        buildFavoriteItems(uid, it)
    }

suspend fun Backend.buildFavoriteItems(uid: PrimaryKey, userFavorite: List<UserFavorite>) =
    captureFailure(FavoriteError::from) {
        val topicIds = mutableListOf<PrimaryKey>()
        val communityIds = mutableListOf<PrimaryKey>()
        val roomIds = mutableListOf<PrimaryKey>()
        val userIds = mutableListOf<PrimaryKey>()
        val titleIds = mutableListOf<PrimaryKey>()
        val fileIds = mutableListOf<PrimaryKey>()

        userFavorite.forEach {
            when (it.objectType) {
                ObjectType.TOPIC -> topicIds.add(it.objectId)
                ObjectType.COMMUNITY -> communityIds.add(it.objectId)
                ObjectType.ROOM -> roomIds.add(it.objectId)
                ObjectType.USER -> userIds.add(it.objectId)
                ObjectType.TITLE -> titleIds.add(it.objectId)
                ObjectType.FILE -> fileIds.add(it.objectId)
                else -> {}
            }
        }

        val topics = getTopicByIds(topicIds, uid).getOrNull()?.associateBy { it.id }.orEmpty()
        val communities =
            database.community.getRawCommunities(ObjectListFetch.IdListFetch(communityIds))
                .andThen(FavoriteError::fromError) { processRawCommunityToCommunityInfo(it) }
                .getOrNull()?.associateBy { it.id }.orEmpty()
        val rooms =
            getRoomInfoList(ObjectListFetch.IdListFetch(roomIds), uid)
                .getOrNull()?.associateBy { it.id }.orEmpty()
        val users =
            getUserInfoList(ObjectListFetch.IdListFetch(userIds))
                .getOrNull()?.associateBy { it.id }.orEmpty()
        val titles = titleIds.mapNotNull { getTitleInfo(it).getOrNull() }.associateBy { it.id }
        val files =
            database.file.getFileRecordByIds(fileIds)
                .andThen(FavoriteError::fromError) { processFileRecordToFileInfo(it) }
                .getOrNull()?.associateBy { it.id }.orEmpty()

        userFavorite.map {
            val favoriteInfo = it.toUserFavoriteInfo()
            val projection =
                when (it.objectType) {
                    ObjectType.TOPIC -> UserFavoriteItem(favoriteInfo, topicInfo = topics[it.objectId])
                    ObjectType.COMMUNITY -> UserFavoriteItem(favoriteInfo, communityInfo = communities[it.objectId])
                    ObjectType.ROOM -> UserFavoriteItem(favoriteInfo, roomInfo = rooms[it.objectId])
                    ObjectType.USER -> UserFavoriteItem(favoriteInfo, userInfo = users[it.objectId])
                    ObjectType.TITLE -> UserFavoriteItem(favoriteInfo, titleInfo = titles[it.objectId])
                    ObjectType.FILE -> UserFavoriteItem(favoriteInfo, fileInfo = files[it.objectId])
                    else -> null
                }
            projection ?: UserFavoriteItem(favoriteInfo)
        }
    }
