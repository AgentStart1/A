/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.shared.type.PrimaryKey

internal fun composeCommunityId(data: TopicComposeData): PrimaryKey? =
    when (data) {
        is TopicComposeData.Community -> data.communityId
        is TopicComposeData.PublicRoom -> data.communityId
        is TopicComposeData.PrivateRoom, is TopicComposeData.User -> null
    }

internal fun topicComposeScope(data: TopicComposeData): SearchScope =
    when (data) {
        is TopicComposeData.Community -> SearchScope.CommunityTopic(data.communityId)
        is TopicComposeData.PublicRoom -> SearchScope.RoomTopic(data.roomId)
        is TopicComposeData.PrivateRoom -> SearchScope.RoomTopic(data.roomId)
        is TopicComposeData.User -> SearchScope.UserTopic(data.uid)
    }
