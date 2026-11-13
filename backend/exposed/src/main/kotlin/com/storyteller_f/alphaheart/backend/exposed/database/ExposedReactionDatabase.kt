/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.Cursor
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.ReactionDatabase
import com.storyteller_f.alphaheart.backend.core.ReactionFetch
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.Reaction
import com.storyteller_f.alphaheart.backend.core.types.ReactionRecord
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.isNotEmpty
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.query.bindPaginationQuery
import com.storyteller_f.alphaheart.backend.exposed.tables.ReactionRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.Reactions
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.map
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.obj.ReactionCursorKey
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.groupByPair
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.countDistinct
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.max
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.upsert

class ExposedReactionDatabase(val databaseSession: ExposedDatabaseSession) : ReactionDatabase {
    override suspend fun statsReactionRecord(objectId: PrimaryKey, emoji: String, objectType: ObjectType) =
        getReactionCountForEmoji(listOf(objectId), emoji).andThen(DatabaseError::fromError) { reactionCountList ->
            val triple = reactionCountList.firstOrNull()
            if (triple == null) {
                databaseSession.dbQuery {
                    Reactions.deleteWhere {
                        Reactions.objectId eq objectId and (Reactions.emoji eq emoji)
                    }
                    Unit
                }
            } else {
                databaseSession.dbQuery {
                    check(
                        Reactions.upsert(Reactions.objectId, Reactions.emoji) {
                            it[Reactions.objectId] = objectId
                            it[Reactions.emoji] = emoji
                            it[Reactions.count] = triple.second
                            it[Reactions.objectType] = objectType
                            it[Reactions.lastReactionId] = triple.third
                        }.insertedCount > 0,
                    ) {
                        "insert reaction failed"
                    }
                }
            }
        }

    override suspend fun getReactionInfoPaginationResult(
        objectId: List<PrimaryKey>,
        uid: PrimaryKey?,
        reactionFetch: ReactionFetch,
    ) = paginationFromResults(
        databaseSession.dbSearch {
            search {
                buildReactionInfoQuery(objectId, reactionFetch).limit(reactionFetch.size)
                    .orderBy(
                        Reactions.count to SortOrder.DESC,
                        Reactions.lastReactionId to SortOrder.ASC,
                    )
            }
            map(Reaction::wrapRow)
        },
        databaseSession.dbSearch {
            search {
                buildReactionInfoQuery(objectId, reactionFetch)
            }
            count()
        },
    ).mapPagingResultNotNull(DatabaseError::fromError) { list ->
        processReactionToReactionInfo(uid, list)
    }

    private suspend fun processReactionToReactionInfo(uid: PrimaryKey?, list: List<Reaction>) =
        (
            if (uid == null) {
                CustomResult.success(emptyMap())
            } else {
                val objectIdList = list.map { it.objectId }.distinct()
                hasReactedEmoji(objectIdList, uid).map {
                    it.groupByPair().mapValues { v ->
                        v.value.toSet()
                    }
                }
            }
            ).map { reactedMap ->
            list.map { reaction ->
                ReactionInfo(
                    reaction.emoji,
                    reaction.objectId,
                    reaction.count,
                    reactedMap[reaction.objectId]?.contains(reaction.emoji) == true,
                    reaction.lastReactionId,
                )
            }
        }

    override suspend fun hasReactedEmoji(
        objectIdList: List<PrimaryKey>,
        uid: PrimaryKey,
    ): CustomResult<List<Pair<Long, String>>, DatabaseError> {
        if (objectIdList.isEmpty()) return CustomResult.success(emptyList())
        return databaseSession.dbSearch {
            search {
                ReactionRecords.select(ReactionRecords.objectId, ReactionRecords.emoji).where {
                    ReactionRecords.objectId inList objectIdList and (ReactionRecords.uid eq uid)
                }.groupBy(ReactionRecords.objectId, ReactionRecords.emoji)
            }
            map {
                it[ReactionRecords.objectId] to it[ReactionRecords.emoji]
            }
        }
    }

    override suspend fun getReactionInfo(uid: PrimaryKey, objectId: PrimaryKey, emojiText: String) =
        databaseSession.dbSearch {
            search {
                Reactions.selectAll().where {
                    Reactions.objectId eq objectId and (Reactions.emoji eq emojiText)
                }
            }
            first { Reaction.wrapRow(it) }
        }.andThenPresent(DatabaseError::fromError) {
            hasReactedForEmoji(objectId, uid, emojiText).map { hasReacted ->
                ReactionInfo(it.emoji, it.objectId, it.count, hasReacted, it.lastReactionId)
            }
        }

    override suspend fun hasReactedForEmoji(objectId: PrimaryKey, uid: PrimaryKey, emoji: String) =
        databaseSession.dbSearch {
            search {
                ReactionRecords.selectAll().where {
                    ReactionRecords.objectId eq objectId and
                        (ReactionRecords.emoji eq emoji) and
                        (ReactionRecords.uid eq uid)
                }
            }
            isNotEmpty()
        }

    override suspend fun deleteReaction(uid: PrimaryKey, emoji: String, objectId: PrimaryKey) =
        getReactionRecordInfo(uid, emoji, objectId).andThen(DatabaseError::fromError) { recordInfo ->
            if (recordInfo == null) {
                CustomResult.success(true)
            } else {
                deleteReaction(recordInfo.id)
            }
        }

    override suspend fun getReactionRecordInfo(uid: PrimaryKey, emoji: String, objectId: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                ReactionRecords.selectAll().where {
                    ReactionRecords.objectId eq objectId and
                        (ReactionRecords.emoji eq emoji) and
                        (ReactionRecords.uid eq uid)
                }
            }
            first { row ->
                val reactionRecord = ReactionRecord.wrapRow(row)
                ReactionRecordInfo(
                    reactionRecord.id,
                    emoji,
                    reactionRecord.objectId,
                    reactionRecord.objectType,
                    reactionRecord.createdTime,
                    uid,
                )
            }
        }

    override suspend fun deleteReaction(reactionId: PrimaryKey) =
        databaseSession.dbQuery {
            ReactionRecords.deleteWhere { ReactionRecords.id eq reactionId }
        }.map { value -> value > 0 }

    override suspend fun insertReaction(reactionRecord: ReactionRecord) =
        databaseSession.dbQuery {
            check(
                ReactionRecords.insert { statement ->
                    statement[ReactionRecords.id] = reactionRecord.id
                    statement[ReactionRecords.uid] = reactionRecord.uid
                    statement[ReactionRecords.objectId] = reactionRecord.objectId
                    statement[ReactionRecords.objectType] = reactionRecord.objectType
                    statement[ReactionRecords.emoji] = reactionRecord.emoji
                    statement[ReactionRecords.createdTime] = reactionRecord.createdTime
                }.insertedCount > 0,
            ) {
                "insert reaction failed"
            }
        }

    override suspend fun getReactionCount(
        objectIdList: List<PrimaryKey>,
    ): CustomResult<List<Pair<Long, Long>>, DatabaseError> {
        if (objectIdList.isEmpty()) return CustomResult.success(emptyList())
        return databaseSession.dbSearch {
            search {
                Reactions.selectAll().where { Reactions.objectId inList objectIdList }
            }
            map {
                it[Reactions.objectId] to it[Reactions.count]
            }
        }
    }

    override suspend fun getReactionCountForEmoji(objectId: List<PrimaryKey>, emoji: String) =
        databaseSession.dbSearch {
            val column = ReactionRecords.emoji.countDistinct()
            val lastReactionId = ReactionRecords.id.max()
            search {
                ReactionRecords.select(ReactionRecords.objectId, column, lastReactionId).where {
                    ReactionRecords.objectId inList objectId and (ReactionRecords.emoji eq emoji)
                }.groupBy(ReactionRecords.objectId)
            }
            map { Triple(it[ReactionRecords.objectId], it[column], it[lastReactionId] ?: 0) }
        }

    fun buildReactionInfoQuery(objectId: List<PrimaryKey>, reactionFetch: ReactionFetch): Query {
        val query =
            Reactions.selectAll().where {
                Reactions.objectId inList objectId
            }
        when (val cursor = reactionFetch.cursor) {
            is Cursor.DescCursor<ReactionCursorKey> ->
                query.andWhere {
                    val value = cursor.value
                    Reactions.count greaterEq value.count and (Reactions.lastReactionId less value.reactionId)
                }

            is Cursor.AscCursor<ReactionCursorKey> ->
                query.andWhere {
                    val value = cursor.value
                    Reactions.count lessEq value.count and (Reactions.lastReactionId greater value.reactionId)
                }

            null -> {}
        }
        return query
    }

    override suspend fun getUserReactionRecordsPaginationResult(uid: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
        paginationFromResults(
            databaseSession.dbSearch {
                search {
                    ReactionRecords.selectAll().where {
                        ReactionRecords.uid eq uid
                    }.orderBy(ReactionRecords.id to SortOrder.DESC)
                        .bindPaginationQuery(ReactionRecords, primaryKeyFetch)
                }
                map(ReactionRecord::wrapRow)
            },
            getUserReactionRecordCountByUid(uid),
        )

    override suspend fun getUserReactionRecordCount(uid: PrimaryKey) = getUserReactionRecordCountByUid(uid)

    private suspend fun getUserReactionRecordCountByUid(uid: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                ReactionRecords.selectAll().where {
                    ReactionRecords.uid eq uid
                }
            }
            count()
        }
}
