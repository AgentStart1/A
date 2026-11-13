/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import kotlinx.datetime.LocalDateTime

class Room(
    val id: PrimaryKey,
    val createdTime: LocalDateTime,
    val aid: String,
    val name: String,
    val creator: PrimaryKey,
    val icon: PrimaryKey? = null,
    val communityId: PrimaryKey? = null,
    val status: ObjectStatus = ObjectStatus.NORMAL,
) {
    val readOnly get() = status == ObjectStatus.READ_ONLY
    companion object
}

data class RawRoom(
    val room: Room,
    val joinedTime: LocalDateTime? = null,
    val lastRead: PrimaryKey? = null,
    val memberCount: Long? = null,
    val latestTopic: PrimaryKey? = null,
    val favoriteId: PrimaryKey? = null,
    val subscriptionId: PrimaryKey? = null,
) {
    val hasJoined = joinedTime != null
}

fun RawRoom.toRoomInfo(icon: FileInfo? = null): RoomInfo =
    RoomInfo(
        room.id,
        room.createdTime,
        room.name,
        room.aid,
        room.creator,
        memberCount ?: 0,
        icon = icon,
        joinedTime = joinedTime,
        communityId = room.communityId,
        lastRead = lastRead,
        latestTopic = latestTopic,
        favoriteId = favoriteId,
        subscriptionId = subscriptionId,
        status = room.status,
    )

fun buildUserNotificationRoom(user: User, adminUid: PrimaryKey): Room {
    val notificationAid = "${user.aid?.takeIf(String::isNotBlank) ?: user.id}_notification"
    return Room(
        id = user.notificationId,
        createdTime = now(),
        aid = notificationAid,
        name = "Notification",
        creator = adminUid,
        icon = null,
        communityId = null,
    )
}

suspend fun buildMemberForNotificationRoom(user: User, adminUid: PrimaryKey): List<Member> =
    listOf(
        Member(
            SnowflakeFactory.nextId(),
            user.id,
            user.notificationId,
            ObjectType.ROOM,
            now(),
            MemberStatus.INVITED,
            joinedTime = null,
            invitedTime = now(),
        ),
        Member(
            SnowflakeFactory.nextId(),
            adminUid,
            user.notificationId,
            ObjectType.ROOM,
            now(),
            MemberStatus.JOINED,
            joinedTime = now(),
            invitedTime = null,
        ),
    )
