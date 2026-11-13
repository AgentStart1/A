/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.Cursor
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.RoomActivityRecord
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.tables.Members
import com.storyteller_f.alphaheart.backend.exposed.tables.Rooms
import com.storyteller_f.alphaheart.backend.exposed.tables.Topics
import com.storyteller_f.alphaheart.backend.exposed.tables.UserTopicReads
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.select

private val latestMessageColumn = Topics.id.max()
private val unreadMessageColumn = Topics.id.countDistinct()

internal suspend fun ExposedContainerDatabase.queryRoomActivityRecords(uid: PrimaryKey, roomIds: List<PrimaryKey>) =
    captureFailure(DatabaseError::from) {
        if (roomIds.isEmpty()) return@captureFailure emptyList()
        val latest =
            databaseSession.dbSearch {
                search {
                    Members.join(Rooms, JoinType.INNER, Members.objectId, Rooms.id)
                        .join(Topics, JoinType.LEFT, Rooms.id, Topics.parentId) { Topics.parentType eq ObjectType.ROOM }
                        .select(Members.objectId, latestMessageColumn).where {
                            Members.uid eq uid and (Members.objectType eq ObjectType.ROOM) and
                                (Members.status eq MemberStatus.JOINED) and Members.joinedTime.isNotNull() and
                                (Members.objectId inList roomIds)
                        }.groupBy(Members.objectId)
                }
                map { it[Members.objectId] to it[latestMessageColumn] }
            }.getOrThrow()
        val unread =
            databaseSession.dbSearch {
                search { unreadRoomActivityQuery(uid).andWhere { Members.objectId inList roomIds } }
                map { it[Members.objectId] to it[unreadMessageColumn] }
            }.getOrThrow().toMap()
        latest.map { (roomId, messageId) -> RoomActivityRecord(roomId, messageId, unread[roomId] ?: 0) }
    }

internal suspend fun ExposedContainerDatabase.queryUnreadRoomActivityRecords(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
    paginationFromResults(
        databaseSession.dbSearch {
            search {
                val query = unreadRoomActivityQuery(uid)
                val order =
                    when (val cursor = fetch.cursor) {
                        is Cursor.DescCursor -> {
                            query.having { latestMessageColumn less cursor.value }
                            SortOrder.DESC
                        }

                        is Cursor.AscCursor -> {
                            query.having { latestMessageColumn greater cursor.value }
                            SortOrder.ASC
                        }

                        null -> SortOrder.DESC
                    }
                query.orderBy(latestMessageColumn to order).limit(fetch.size)
            }
            map { RoomActivityRecord(it[Members.objectId], it[latestMessageColumn], it[unreadMessageColumn]) }
        },
        countUnreadRoomActivities(uid),
    ).map { page ->
        if (fetch.cursor is Cursor.AscCursor) page.copy(list = page.list.reversed()) else page
    }

internal suspend fun ExposedContainerDatabase.countUnreadRoomActivities(uid: PrimaryKey) =
    databaseSession.dbSearch {
        search { unreadRoomActivityQuery(uid) }
        count()
    }

/** Filter the complete joined-room set before grouping and pagination, never a loaded room page. */
private fun unreadRoomActivityQuery(uid: PrimaryKey): Query = unreadRoomMessageQuery(uid).groupBy(Members.objectId)

internal suspend fun ExposedContainerDatabase.countUnreadRoomMessages(uid: PrimaryKey) =
    databaseSession.dbSearch {
        search { unreadRoomMessageQuery(uid, aggregate = true) }
        map { it[unreadMessageColumn] }
    }.map { it.single() }

private fun unreadRoomMessageQuery(uid: PrimaryKey, aggregate: Boolean = false): Query =
    Members.join(Rooms, JoinType.INNER, Members.objectId, Rooms.id)
        .join(Topics, JoinType.INNER, Rooms.id, Topics.parentId) { Topics.parentType eq ObjectType.ROOM }
        .join(UserTopicReads, JoinType.LEFT, Rooms.id, UserTopicReads.objectId) {
            UserTopicReads.uid eq Members.uid and (UserTopicReads.objectType eq ObjectType.ROOM)
        }.select(
            if (aggregate) {
                listOf(
                    unreadMessageColumn,
                )
            } else {
                listOf(Members.objectId, latestMessageColumn, unreadMessageColumn)
            },
        ).where {
            Members.uid eq uid and (Members.objectType eq ObjectType.ROOM) and
                (Members.status eq MemberStatus.JOINED) and Members.joinedTime.isNotNull() and
                (UserTopicReads.topicId.isNull() or (UserTopicReads.topicId less Topics.id))
        }
