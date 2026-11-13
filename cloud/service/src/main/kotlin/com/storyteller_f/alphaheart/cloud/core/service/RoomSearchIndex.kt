/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.Cursor
import com.storyteller_f.alphaheart.backend.core.ObjectFetch.IdFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.service.MemberDocument
import com.storyteller_f.alphaheart.backend.core.service.RoomDocument
import com.storyteller_f.alphaheart.cloud.core.service.error.RoomError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey

private const val MEMBER_INDEX_PAGE_SIZE = 100

internal suspend fun Backend.refreshRoomSearch(id: PrimaryKey) =
    captureFailure(RoomError::from) {
        val room = database.room.getRawRoom(IdFetch(id)).getOrThrow()?.room ?: return@captureFailure Unit
        if (room.communityId != null) roomSearchService.saveDocument(listOf(RoomDocument.fromRoom(room))).getOrThrow()
        var cursor: Cursor<PrimaryKey>? = null
        do {
            val page =
                database.container.getMemberWithUserPaginationResult(
                    id,
                    PrimaryKeyFetch(cursor, MEMBER_INDEX_PAGE_SIZE),
                ).getOrThrow().list
            val documents =
                page.filter { it.first.status == MemberStatus.JOINED }.map { (member, user) ->
                    MemberDocument(
                        id = member.id,
                        uid = member.uid,
                        objectId = id,
                        objectType = ObjectType.ROOM,
                        nickname = user.user.nickname,
                        objectName = room.name,
                        communityId = room.communityId,
                    )
                }
            if (documents.isNotEmpty()) memberSearchService.saveDocument(documents).getOrThrow()
            val lastUser = page.lastOrNull()?.second?.user
            cursor = lastUser?.id?.let { Cursor.DescCursor(it) }
        } while (page.size == MEMBER_INDEX_PAGE_SIZE)
    }
