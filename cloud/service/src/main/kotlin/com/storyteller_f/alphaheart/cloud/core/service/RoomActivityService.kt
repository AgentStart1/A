/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.api.MAX_PAGE_SIZE
import com.storyteller_f.alphaheart.api.RoomActivityInfo
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.api.UnreadMessageCountResponse
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.getRawTopicListByIds
import com.storyteller_f.alphaheart.backend.core.processTopicToRawTopic
import com.storyteller_f.alphaheart.backend.core.types.RoomActivityRecord
import com.storyteller_f.alphaheart.cloud.core.service.error.RoomActivityError
import com.storyteller_f.result.andThen
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.collections.immutable.toImmutableList

/** Adds activity only for eligible rooms from this exact page; private bodies stay encrypted. */
suspend fun Backend.fillRoomActivities(uid: PrimaryKey?, response: RoomInfoListResponse?) =
    captureFailure(RoomActivityError::from) {
        if (uid == null || response == null || response.data.isEmpty()) return@captureFailure response
        val records = database.container.getRoomActivityRecords(uid, response.data.map { it.id }).getOrThrow()
        val activities = processRoomActivities(uid, records).associateBy { it.room.id }
        response.copy(activities = response.data.mapNotNull { activities[it.id] }.toImmutableList())
    }

suspend fun Backend.getUnreadMessageCount(uid: PrimaryKey) =
    database.container.getUserUnreadMessageCount(uid).map { UnreadMessageCountResponse(it) }

suspend fun Backend.getUnreadRoomActivities(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
    captureFailure(RoomActivityError::from) {
        checkActivityPageSize(fetch)
        val page = database.container.getUnreadRoomActivityRecords(uid, fetch).getOrThrow()
        PaginationResult(processRoomActivities(uid, page.list), page.total)
    }

suspend fun Backend.getJoinedPinnedTopics(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
    captureFailure(RoomActivityError::from) {
        checkActivityPageSize(fetch)
        val page = database.topic.getJoinedCommunityPinnedTopics(uid, fetch).getOrThrow()
        val rawTopics = database.processTopicToRawTopic(uid, page.list).getOrThrow()
        PaginationResult(processTopicActivities(rawTopics, uid).getOrThrow().orEmpty(), page.total)
    }

private suspend fun Backend.processRoomActivities(
    uid: PrimaryKey,
    records: List<RoomActivityRecord>,
): List<RoomActivityInfo> {
    if (records.isEmpty()) return emptyList()
    val rooms =
        getRoomInfoList(ObjectListFetch.IdListFetch(records.map { it.roomId }), uid)
            .getOrThrow().associateBy { it.id }
    val messages =
        database.getRawTopicListByIds(
            uid,
            records.mapNotNull { it.latestMessageId },
        ).andThen(RoomActivityError::fromError) {
            processTopicActivities(it, uid)
        }.getOrThrow().orEmpty().associateBy { it.id }
    return records.mapNotNull { record ->
        rooms[record.roomId]?.let { room ->
            RoomActivityInfo(room, record.latestMessageId, messages[record.latestMessageId], record.unreadMessageCount)
        }
    }
}

private fun checkActivityPageSize(fetch: PrimaryKeyFetch) {
    if (fetch.size !in 1..MAX_PAGE_SIZE) throw CustomBadRequestException("Invalid activity page size")
}
