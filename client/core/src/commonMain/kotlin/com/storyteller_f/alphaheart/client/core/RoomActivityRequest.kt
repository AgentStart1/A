/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.RoomActivityListResponse
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.endpoint4k.ktor.client.invoke
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.mapError
import kotlinx.collections.immutable.toImmutableList

suspend fun UserSessionManager.getUnreadRoomActivities(
    query: PaginationQuery,
): CustomResult<RoomActivityListResponse, RequestError> =
    serviceCatching {
        this@getUnreadRoomActivities.decryptRoomActivities(
            CustomApi.Users.Unread.activities(query),
        )
    }.mapError(RequestError::fromError)

suspend fun UserSessionManager.getJoinedPinnedTopics(query: PaginationQuery) =
    serviceCatching { CustomApi.Users.PinnedTopics.get(query) }

suspend fun UserSessionManager.getUnreadMessageCount() = serviceCatching { CustomApi.Users.Unread.messages() }

internal suspend fun UserSessionManager.decryptRoomList(response: RoomInfoListResponse): RoomInfoListResponse =
    response.copy(activities = decryptRoomActivities(RoomActivityListResponse(response.activities)).data)

private suspend fun UserSessionManager.decryptRoomActivities(
    response: RoomActivityListResponse,
): RoomActivityListResponse {
    val messages = decryptTopicActivities(response.data.mapNotNull { it.latestMessage }, this).associateBy { it.id }
    return response.copy(
        data =
        response.data.map { activity ->
            activity.copy(latestMessage = activity.latestMessage?.let { messages[it.id] })
        }.toImmutableList(),
    )
}
