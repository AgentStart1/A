/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.AdminDatabase
import com.storyteller_f.alphaheart.backend.core.InsertTopicTuple
import com.storyteller_f.alphaheart.backend.core.MemberAuthData
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.Community
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.Room
import com.storyteller_f.alphaheart.backend.core.types.TaskRecord
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.query.bindPaginationQuery
import com.storyteller_f.alphaheart.backend.exposed.tables.Aids
import com.storyteller_f.alphaheart.backend.exposed.tables.Communities
import com.storyteller_f.alphaheart.backend.exposed.tables.EncryptedKeys
import com.storyteller_f.alphaheart.backend.exposed.tables.Members
import com.storyteller_f.alphaheart.backend.exposed.tables.PanelLogs
import com.storyteller_f.alphaheart.backend.exposed.tables.Rooms
import com.storyteller_f.alphaheart.backend.exposed.tables.TaskRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.Topics
import com.storyteller_f.alphaheart.backend.exposed.tables.UserSubscriptions
import com.storyteller_f.alphaheart.backend.exposed.tables.Users
import com.storyteller_f.alphaheart.backend.exposed.tables.addTaskRecord
import com.storyteller_f.alphaheart.backend.exposed.tables.batchAddMembers
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.md5
import com.storyteller_f.shared.utils.now
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll

class ExposedAdminDatabase(val databaseSession: ExposedDatabaseSession) : AdminDatabase {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun batchAddUser(users: List<User>) {
        databaseSession.dbQuery {
            Users.batchInsert(users) {
                this[Users.id] = it.id
                this[Users.icon] = it.icon
                this[Users.nickname] = it.nickname
                this[Users.publicKey] = it.publicKey
                this[Users.publicKeyMd5] = md5(it.publicKey)
                this[Users.address] = it.address
                this[Users.encryptionPublicKey] = it.encryptionPublicKey
                this[Users.createdTime] = it.createdTime
                this[Users.passType] = it.passType
                this[Users.notificationId] = it.notificationId
                this[Users.algoType] = it.algoType
            }
            Aids.batchInsert(users) {
                this[Aids.value] = checkNotNull(it.aid) { "Imported user aid is missing" }
                this[Aids.objectId] = it.id
                this[Aids.objectType] = ObjectType.USER
            }
        }.getOrThrow()
    }

    override suspend fun batchAddCommunities(communities: List<Community>, members: List<Member>) {
        databaseSession.dbQuery {
            Communities.batchInsert(communities) {
                this[Communities.id] = it.id
                this[Communities.createdTime] = it.createdTime
                this[Communities.name] = it.name
                this[Communities.icon] = it.iconId
                this[Communities.owner] = it.owner
                it.fontSettings?.let { fs ->
                    this[Communities.fontSettings] = json.encodeToString(fs)
                }
                this[Communities.memberPolicy] = it.memberPolicy
            }
            Aids.batchInsert(communities) {
                this[Aids.value] = it.aid
                this[Aids.objectId] = it.id
                this[Aids.objectType] = ObjectType.COMMUNITY
            }
            batchAddMembers(members)
        }.getOrThrow()
    }

    override suspend fun batchAddRooms(roomList: List<Room>, membersList: List<Member>) {
        databaseSession.dbQuery {
            Rooms.batchInsert(roomList) {
                this[Rooms.id] = it.id
                this[Rooms.icon] = it.icon
                this[Rooms.name] = it.name
                this[Rooms.communityId] = it.communityId
                this[Rooms.creator] = it.creator
                this[Rooms.createdTime] = it.createdTime
            }
            Aids.batchInsert(roomList) {
                this[Aids.value] = it.aid
                this[Aids.objectId] = it.id
                this[Aids.objectType] = ObjectType.ROOM
            }
            batchAddMembers(membersList)
        }.getOrThrow()
    }

    override suspend fun getAllMembers(distinct: List<String>) =
        databaseSession.dbQuery {
            Members
                .join(Rooms, JoinType.INNER, Members.objectId, Rooms.id)
                .join(Aids, JoinType.INNER, Members.objectId, Aids.objectId)
                .join(Users, JoinType.INNER, Members.uid, Users.id)
                .select(Users.fields + Aids.value)
                .where {
                    Aids.value inList distinct
                }.toList().map {
                    MemberAuthData(
                        it[Users.publicKey],
                        it[Users.encryptionPublicKey],
                        it[Users.id],
                        it[Aids.value],
                        it[Users.algoType],
                    )
                }
        }

    override suspend fun batchAddEncryptTopicKeys(
        encryptedKeys: List<Triple<PrimaryKey, ByteArray, Long>>,
    ): CustomResult<Unit, DatabaseError> =
        databaseSession.dbQuery {
            EncryptedKeys.batchInsert(encryptedKeys) { (topicId, b, uid) ->
                this[EncryptedKeys.topicId] = topicId
                this[EncryptedKeys.encryptedAes] = ExposedBlob(b)
                this[EncryptedKeys.uid] = uid
            }
        }

    override suspend fun batchAddTopics(
        tuples: List<InsertTopicTuple>,
        userMap: Map<String, User>,
        objectType: ObjectType,
    ) = databaseSession.dbQuery {
        insertTopics(tuples, userMap, objectType)
    }

    override suspend fun createTaskRecord(record: TaskRecord) =
        databaseSession.dbQuery {
            addTaskRecord(record)
            record
        }

    override suspend fun getTaskRecords(
        type: TaskRecordType?,
        isSuccess: Boolean?,
        failureType: String?,
        fetch: PrimaryKeyFetch,
    ) = paginationFromResults(
        databaseSession.dbSearch {
            search {
                val query = TaskRecords.selectAll().filterTaskRecords(type, isSuccess, failureType)
                query.orderBy(TaskRecords.id, SortOrder.DESC).bindPaginationQuery(TaskRecords, fetch)
            }
            map(TaskRecord::wrapRow)
        },
        databaseSession.dbSearch {
            search {
                TaskRecords.select(TaskRecords.id).filterTaskRecords(type, isSuccess, failureType)
            }
            count()
        },
    )

    private fun Query.filterTaskRecords(type: TaskRecordType?, isSuccess: Boolean?, failureType: String?): Query {
        type?.let { value -> andWhere { TaskRecords.type eq value } }
        isSuccess?.let { value -> andWhere { TaskRecords.success eq value } }
        failureType?.let { value -> andWhere { TaskRecords.failureType eq value } }
        return this
    }

    override suspend fun batchAddSubscription(list: List<UserSubscription>): CustomResult<Unit, DatabaseError> =
        databaseSession.dbQuery {
            UserSubscriptions.batchInsert(list) {
                this[UserSubscriptions.id] = it.id
                this[UserSubscriptions.createdTime] = it.createdTime
                this[UserSubscriptions.uid] = it.uid
                this[UserSubscriptions.objectId] = it.objectId
                this[UserSubscriptions.objectType] = it.objectType
            }
        }

    override suspend fun insertPanelLog(
        log: com.storyteller_f.alphaheart.backend.core.types.PanelLog,
    ): CustomResult<Unit, DatabaseError> =
        databaseSession.dbQuery {
            check(
                PanelLogs.insert {
                    it[id] = log.id
                    it[adminId] = log.adminId
                    it[targetId] = log.targetId
                    it[objectType] = log.objectType
                    it[action] = log.action
                    it[createdTime] = log.createdTime
                }.insertedCount > 0,
            ) {
                "insert panel log failed"
            }
        }

    override suspend fun getPanelLogs(targetId: PrimaryKey, objectType: ObjectType, fetch: PrimaryKeyFetch) =
        paginationFromResults(
            databaseSession.dbSearch {
                search {
                    PanelLogs.selectAll().where {
                        PanelLogs.targetId eq targetId and (PanelLogs.objectType eq objectType)
                    }.orderBy(PanelLogs.id, SortOrder.DESC).bindPaginationQuery(PanelLogs, fetch)
                }
                map(com.storyteller_f.alphaheart.backend.core.types.PanelLog::wrapRow)
            },
            databaseSession.dbSearch {
                search {
                    PanelLogs.select(PanelLogs.id).where {
                        PanelLogs.targetId eq targetId and (PanelLogs.objectType eq objectType)
                    }
                }
                count()
            },
        )

    private suspend fun insertTopics(
        topicTuples: List<InsertTopicTuple>,
        userMap: Map<String, User>,
        rootType: ObjectType,
    ) {
        Topics.batchInsert(topicTuples) {
            val presetTopic = it.topic
            val id = it.id
            val level = it.level
            val index = it.originalIndex
            this[Topics.id] = id
            this[Topics.author] = userMap.getValue(presetTopic.author).id
            this[Topics.createdTime] = now()
            this[Topics.rootId] = it.rootId
            this[Topics.rootType] = rootType
            this[Topics.parentId] =
                if (level == 0) {
                    it.rootId
                } else {
                    val parentIndex = checkNotNull(presetTopic.parent) { "Nested imported topic parent is missing" }
                    topicTuples[index - parentIndex].id
                }
            this[Topics.parentType] = if (level == 0) rootType else ObjectType.TOPIC
            this[Topics.content] = ExposedBlob(it.content)
            this[Topics.isEncrypted] = it.isEncrypted
            this[Topics.level] = level + 1
        }
        Aids.batchInsert(
            topicTuples.filter {
                !it.topic.aid.isNullOrBlank()
            },
        ) {
            val first = it.topic
            val id = it.id
            this[Aids.value] = checkNotNull(first.aid) { "Imported topic aid is missing" }
            this[Aids.objectId] = id
            this[Aids.objectType] = ObjectType.TOPIC
        }
    }
}
