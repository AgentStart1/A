/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared.model

import com.storyteller_f.shared.type.CustomImmutableList
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

/** Shared read access for topic projections; the wire format keeps the base object nested. */
sealed interface TopicProjection : ModelObject {
    val topicInfo: TopicInfo
    val authorInfo: UserInfo?
    override val id: PrimaryKey get() = topicInfo.id
    val content: TopicContent get() = topicInfo.content
    val author: PrimaryKey get() = topicInfo.author
    val rootId: PrimaryKey get() = topicInfo.rootId
    val rootType: ObjectType get() = topicInfo.rootType
    val parentId: PrimaryKey get() = topicInfo.parentId
    val parentType: ObjectType get() = topicInfo.parentType
    val hasJoined: Boolean get() = topicInfo.hasJoined
    val createdTime: LocalDateTime get() = topicInfo.createdTime
    val commentCount: Long get() = topicInfo.commentCount
    val reactionCount: Long get() = topicInfo.reactionCount
    val hasComment: Boolean get() = topicInfo.hasComment
    val isEncrypted: Boolean get() = topicInfo.isEncrypted
    val level: Int get() = topicInfo.level
    val isPin: Boolean get() = topicInfo.isPin
    val lastModifiedTime: LocalDateTime? get() = topicInfo.lastModifiedTime
    val aid: String? get() = topicInfo.aid
    val lastRead: PrimaryKey? get() = topicInfo.lastRead
    val favoriteId: PrimaryKey? get() = topicInfo.favoriteId
    val subscriptionId: PrimaryKey? get() = topicInfo.subscriptionId
    val status: ObjectStatus get() = topicInfo.status
    val readOnly: Boolean get() = topicInfo.readOnly
    override val objectType: ObjectType get() = topicInfo.objectType
}

/** Detail and list endpoints load the same relations. */
@Serializable
data class TopicDetail(
    override val topicInfo: TopicInfo,
    override val authorInfo: UserInfo?,
    val subTopics: CustomImmutableList<TopicReplyPreview>,
    val reactions: CustomImmutableList<ReactionInfo>,
    val roomInfo: RoomInfo?,
) : TopicProjection {
    companion object {
        val EMPTY = TopicDetail(TopicInfo.EMPTY, null, persistentListOf(), persistentListOf(), null)
    }
}

/** Nested reply previews do not load reactions, rooms or further replies. */
@Serializable
data class TopicReplyPreview(override val topicInfo: TopicInfo, override val authorInfo: UserInfo?) : TopicProjection

/** Creation responses and new-topic events carry only the topic and its author. */
@Serializable
data class TopicCreated(override val topicInfo: TopicInfo, override val authorInfo: UserInfo?) : TopicProjection

/** A newly created topic has no replies or reactions yet. */
fun TopicCreated.toInitialDetail(): TopicDetail =
    TopicDetail(topicInfo, authorInfo, persistentListOf(), persistentListOf(), null)

/** Activity queries may include reply previews for topic card lists. */
@Serializable
data class TopicActivity(
    override val topicInfo: TopicInfo,
    override val authorInfo: UserInfo?,
    val reactions: CustomImmutableList<ReactionInfo>,
    val roomInfo: RoomInfo?,
    val subTopics: CustomImmutableList<TopicReplyPreview>? = null,
) : TopicProjection {
    fun withCachedReplies(cached: TopicDetail?): TopicDetail =
        TopicDetail(topicInfo, authorInfo, subTopics ?: cached?.subTopics ?: persistentListOf(), reactions, roomInfo)
}
