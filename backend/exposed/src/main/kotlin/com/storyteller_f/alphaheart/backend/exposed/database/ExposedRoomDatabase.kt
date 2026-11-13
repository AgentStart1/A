/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.ContainerDatabase
import com.storyteller_f.alphaheart.backend.core.JoinSearch
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.RoomDatabase
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.RawRoom
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.query.bindPaginationQuery
import com.storyteller_f.alphaheart.backend.exposed.tables.Aids
import com.storyteller_f.alphaheart.backend.exposed.tables.Members
import com.storyteller_f.alphaheart.backend.exposed.tables.Rooms
import com.storyteller_f.alphaheart.backend.exposed.tables.Users
import com.storyteller_f.alphaheart.backend.exposed.tables.batchAddMembers
import com.storyteller_f.alphaheart.backend.exposed.tables.findRoomById
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.map
import com.storyteller_f.result.mapPresent
import com.storyteller_f.shared.model.UserPubKeyInfo
import com.storyteller_f.shared.obj.UpdateRoomBody
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update

class ExposedRoomDatabase(val databaseSession: ExposedDatabaseSession, val containerDatabase: ContainerDatabase) :
    RoomDatabase {
    override suspend fun checkRoomIsPrivate(roomId: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                Room.findRoomById(roomId)
            }
            first {
                it[Rooms.communityId] == null
            }
        }

    override suspend fun getRoomPaginationResult(
        uid: PrimaryKey?,
        community: PrimaryKey?,
        primaryKeyFetch: PrimaryKeyFetch,
        joinSearch: JoinSearch,
    ) = paginationFromResults(
        getRoomListByPredicate {
            buildRoomSearchWhereQuery(joinSearch, community)
                .bindPaginationQuery(Rooms, primaryKeyFetch)
        },
        getRoomCountByPredicate {
            buildRoomSearchWhereQuery(joinSearch, community)
        },
    ).mapPagingResultNotNull(DatabaseError::fromError) {
        processRoomListToRawRoom(uid, it)
    }

    override suspend fun getRoomCommunityId(parentId: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                Room.findRoomById(parentId)
            }
            first {
                it[Rooms.communityId]
            }
        }

    override suspend fun getRoomPubKeyPaginationResult(roomId: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
        paginationFromResults(
            databaseSession.dbSearch {
                search {
                    Users.join(Members, JoinType.INNER, Users.id, Members.uid)
                        .select(Users.id, Users.publicKey, Users.algoType, Users.encryptionPublicKey).where {
                            Members.objectId eq roomId
                        }.bindPaginationQuery(Users, primaryKeyFetch)
                }
                map { row ->
                    UserPubKeyInfo(
                        row[Users.id],
                        row[Users.publicKey],
                        row[Users.algoType],
                        row[Users.encryptionPublicKey],
                    )
                }
            },
            databaseSession.dbSearch {
                search {
                    Users.join(Members, JoinType.INNER, Users.id, Members.uid)
                        .selectAll().where {
                            Members.objectId eq roomId
                        }
                }
                count()
            },
        )

    override suspend fun getRawRoom(
        objectFetch: ObjectFetch,
        fillJoinInfo: Boolean?,
        uid: PrimaryKey?,
    ): CustomResult<RawRoom?, DatabaseError> {
        if (uid == null && fillJoinInfo == true) {
            return CustomResult.Failure(
                DatabaseError.from(UnauthorizedException()),
            )
        }

        return getRoomByPredicate {
            where {
                when (objectFetch) {
                    is ObjectFetch.AidFetch -> Aids.value eq objectFetch.aid
                    is ObjectFetch.IdFetch -> Rooms.id eq objectFetch.id
                }
            }
        }.andThenPresent(DatabaseError::fromError) { room ->
            processRoomListToRawRoom(uid, listOf(room))
        }.mapPresent {
            it.first()
        }
    }

    suspend fun processRoomListToRawRoom(
        uid: PrimaryKey?,
        rooms: List<Room>,
    ): CustomResult<List<RawRoom>, DatabaseError> =
        containerDatabase.getContainerInfo(
            rooms.map {
                it.id
            },
            uid,
        ).map { map ->
            rooms.map { room ->
                val containerInfo = map[room.id]
                RawRoom(
                    room,
                    containerInfo?.member?.takeIf { it.status == MemberStatus.JOINED }?.joinedTime,
                    containerInfo?.userTopicRead?.topicId,
                    containerInfo?.memberCount,
                    containerInfo?.latestTopicId,
                    containerInfo?.favoriteId,
                    containerInfo?.subscriptionId,
                )
            }
        }

    override suspend fun createRoom(room: Room, members: List<Member>): CustomResult<Room, DatabaseError> =
        databaseSession.dbQuery {
            check(
                Rooms.insert { statement ->
                    statement[Rooms.id] = room.id
                    statement[Rooms.createdTime] = room.createdTime
                    statement[Rooms.name] = room.name
                    statement[Rooms.icon] = room.icon
                    statement[Rooms.creator] = room.creator
                    statement[Rooms.communityId] = room.communityId
                }.insertedCount > 0,
            ) {
                "create room failed"
            }
            check(
                Aids.insert {
                    it[value] = room.aid
                    it[objectId] = room.id
                    it[objectType] = ObjectType.ROOM
                }.insertedCount > 0,
            ) {
                "create aid failed"
            }
            batchAddMembers(members)
            room
        }

    override suspend fun getRawRooms(
        objectListFetch: ObjectListFetch,
        uid: PrimaryKey?,
    ): CustomResult<List<RawRoom>, DatabaseError> {
        if (objectListFetch is ObjectListFetch.AidListFetch && objectListFetch.aidList.isEmpty()) {
            return CustomResult.success(emptyList())
        }
        if (objectListFetch is ObjectListFetch.IdListFetch && objectListFetch.idList.isEmpty()) {
            return CustomResult.success(emptyList())
        }
        return getRoomListByPredicate {
            where {
                when (objectListFetch) {
                    is ObjectListFetch.AidListFetch -> Aids.value inList objectListFetch.aidList
                    is ObjectListFetch.IdListFetch -> Rooms.id inList objectListFetch.idList
                }
            }
        }.andThen(DatabaseError::fromError) {
            processRoomListToRawRoom(uid, it)
        }
    }

    override suspend fun getRoomList(objectListFetch: ObjectListFetch) =
        getRoomListByPredicate {
            where {
                when (objectListFetch) {
                    is ObjectListFetch.AidListFetch -> Aids.value inList objectListFetch.aidList
                    is ObjectListFetch.IdListFetch -> Rooms.id inList objectListFetch.idList
                }
            }
        }

    override suspend fun updateRoom(id: PrimaryKey, body: UpdateRoomBody) =
        databaseSession.dbQuery {
            listOf(
                suspend {
                    val newIcon = body.icon
                    val newName = body.name
                    if (!newName.isNullOrBlank() || newIcon != null) {
                        Rooms.update({
                            Rooms.id eq id
                        }) {
                            if (!newName.isNullOrBlank()) {
                                it[Rooms.name] = newName
                            }
                            if (newIcon != null) {
                                it[Rooms.icon] = newIcon
                            }
                        } > 0
                    } else {
                        true
                    }
                },
            ).all {
                it()
            }
        }

    override suspend fun updateRoomStatus(id: PrimaryKey, status: ObjectStatus) =
        databaseSession.dbQuery {
            Rooms.update({ Rooms.id eq id }) {
                it[Rooms.status] = status
            } > 0
        }

    override suspend fun getPrivateRoomCount() =
        databaseSession.dbSearch {
            search {
                Rooms.selectAll().where {
                    Rooms.communityId.isNull()
                }
            }
            count()
        }

    override suspend fun getPublicRoomCount() =
        databaseSession.dbSearch {
            search {
                Rooms.selectAll().where {
                    Rooms.communityId.isNotNull()
                }
            }
            count()
        }

    override suspend fun getPrivateRoomPaginationResult(primaryKeyFetch: PrimaryKeyFetch, word: String?) =
        paginationFromResults(
            getRoomListByPredicate {
                where { Rooms.communityId.isNull() }
                    .let { q ->
                        if (!word.isNullOrBlank()) {
                            q.andWhere { Rooms.name like "%$word%" }
                        } else {
                            q
                        }
                    }
                    .bindPaginationQuery(Rooms, primaryKeyFetch)
            },
            getRoomCountByPredicate {
                where { Rooms.communityId.isNull() }
                    .let { q ->
                        if (!word.isNullOrBlank()) {
                            q.andWhere { Rooms.name like "%$word%" }
                        } else {
                            q
                        }
                    }
            },
        ).mapPagingResultNotNull(DatabaseError::fromError) {
            processRoomListToRawRoom(null, it)
        }

    private suspend fun getRoomListByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                Rooms
                    .join(Aids, JoinType.INNER, Rooms.id, Aids.objectId)
                    .select(Rooms.fields + Aids.value)
                    .queryBuilder()
            }
            map(Room::wrapRow)
        }

    private suspend fun getRoomByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                Rooms
                    .join(Aids, JoinType.INNER, Rooms.id, Aids.objectId)
                    .select(Rooms.fields + Aids.value)
                    .queryBuilder()
            }
            first(Room::wrapRow)
        }

    private suspend fun getRoomCountByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                Rooms.select(Rooms.id).queryBuilder()
            }
            count()
        }

    fun Query.buildRoomSearchWhereQuery(joinStatusSearch: JoinSearch, community: PrimaryKey?): Query {
        if (community != null) {
            andWhere {
                Rooms.communityId eq community
            }
        }
        when (joinStatusSearch) {
            is JoinSearch.Joined ->
                adjustColumnSet {
                    this.join(Members, JoinType.INNER, Rooms.id, Members.objectId) {
                        Members.uid eq joinStatusSearch.uid
                    }
                }

            is JoinSearch.Unspecified -> {
                val uid = joinStatusSearch.uid
                if (uid != null) {
                    adjustColumnSet {
                        this.join(Members, JoinType.LEFT, Rooms.id, Members.objectId) {
                            Members.uid eq uid
                        }
                    }.andWhere {
                        (Members.uid.isNull() and Rooms.communityId.isNotNull()).or(Members.uid.isNotNull())
                    }
                } else {
                    andWhere {
                        Rooms.communityId.isNotNull()
                    }
                }
            }
        }
        return this
    }
}
