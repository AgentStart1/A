/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.api

import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicActivity
import com.storyteller_f.shared.obj.ListResponse
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.shared.type.CustomImmutableList
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.serialization.Serializable

/** Counts existing direct room messages after the read marker; replies are excluded. */
@Serializable
data class RoomActivityInfo(
    val room: RoomInfo,
    val latestMessageId: PrimaryKey?,
    val latestMessage: TopicActivity?,
    val unreadMessageCount: Long,
)

@Serializable
data class RoomActivityListResponse(
    override val data: CustomImmutableList<RoomActivityInfo>,
    override val pagination: Pagination<String>? = null,
) : ListResponse<RoomActivityInfo>

/** Total direct unread messages across all joined rooms, independent of list pagination. */
@Serializable
data class UnreadMessageCountResponse(val unreadMessageCount: Long)

@Serializable
data class TopicActivityListResponse(
    override val data: CustomImmutableList<TopicActivity>,
    override val pagination: Pagination<String>? = null,
) : ListResponse<TopicActivity>
