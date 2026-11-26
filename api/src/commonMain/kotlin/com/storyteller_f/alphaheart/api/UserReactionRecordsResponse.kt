/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.api

import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.model.TopicActivity
import com.storyteller_f.shared.obj.ListResponse
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.shared.type.CustomImmutableList
import kotlinx.serialization.Serializable

@Serializable
data class UserReactionRecordsResponse(
    override val data: CustomImmutableList<ReactionRecordInfo>,
    override val pagination: Pagination<String>? = null,
    val topics: List<TopicActivity> = emptyList(),
) : ListResponse<ReactionRecordInfo>
