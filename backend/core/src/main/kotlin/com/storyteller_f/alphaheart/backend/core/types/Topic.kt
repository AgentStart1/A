/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicInfo
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

// TODO 增加aid
class Topic(
    val id: PrimaryKey,
    val createdTime: LocalDateTime,
    val author: PrimaryKey,
    val parentId: PrimaryKey,
    val parentType: ObjectType,
    val rootId: PrimaryKey,
    val rootType: ObjectType,
    val content: ByteArray,
    val isEncrypted: Boolean,
    val level: Int,
    val isPin: Boolean = false,
    val lastModifiedTime: LocalDateTime? = null,
    val aid: String? = null,
    val status: ObjectStatus = ObjectStatus.NORMAL,
) {
    val readOnly get() = status == ObjectStatus.READ_ONLY
    companion object
}

data class RawTopic(
    val topic: Topic,
    val content: TopicContent,
    val commentCount: Long = 0,
    val hasComment: Boolean = false,
    val reactionCount: Long = 0,
    val lastRead: PrimaryKey? = null,
    val hasJoined: Boolean = false,
    val favoriteId: PrimaryKey? = null,
    val subscriptionId: PrimaryKey? = null,
)

fun RawTopic.toTopicInfo() =
    TopicInfo(
        id = topic.id,
        content = content,
        author = topic.author,
        rootId = topic.rootId,
        rootType = topic.rootType,
        parentId = topic.parentId,
        parentType = topic.parentType,
        hasJoined = false,
        createdTime = topic.createdTime,
        commentCount = commentCount,
        reactionCount = reactionCount,
        hasComment = hasComment,
        isEncrypted = topic.isEncrypted,
        level = topic.level,
        isPin = topic.isPin,
        lastModifiedTime = topic.lastModifiedTime,
        aid = topic.aid,
        lastRead = lastRead,
        favoriteId = favoriteId,
        subscriptionId = subscriptionId,
        status = topic.status,
    )
