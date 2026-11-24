/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.search

import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope")
@Serializable
sealed interface SearchScope {
    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.World")
    @Serializable
    data object World : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.MyCommunity")
    @Serializable
    data object MyCommunity : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.MyRoom")
    @Serializable
    data object MyRoom : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.CommunityTopic")
    @Serializable
    data class CommunityTopic(val communityId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.CommunityRoom")
    @Serializable
    data class CommunityRoom(val communityId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.RoomTopic")
    @Serializable
    data class RoomTopic(val roomId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.TopicTopic")
    @Serializable
    data class TopicTopic(val topicId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.CommunityMember")
    @Serializable
    data class CommunityMember(val communityId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.RoomMember")
    @Serializable
    data class RoomMember(val roomId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.Users")
    @Serializable
    data object Users : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.UserTopic")
    @Serializable
    data class UserTopic(val userId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.UserCommunities")
    @Serializable
    data class UserCommunities(val userId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.UserReceivedTitle")
    @Serializable
    data class UserReceivedTitle(val userId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.UserCreatedTitle")
    @Serializable
    data class UserCreatedTitle(val userId: PrimaryKey) : SearchScope

    @SerialName("com.storyteller_f.alphaheart.app.pages.search.SearchScope.UploadedFiles")
    @Serializable
    data class UploadedFiles(val objectId: PrimaryKey, val objectType: ObjectType) : SearchScope
}
