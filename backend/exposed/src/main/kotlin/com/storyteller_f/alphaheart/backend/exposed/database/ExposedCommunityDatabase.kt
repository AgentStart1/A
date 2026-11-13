/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.CommunityDatabase
import com.storyteller_f.alphaheart.backend.core.ContainerDatabase
import com.storyteller_f.alphaheart.backend.core.JoinSearch
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.Community
import com.storyteller_f.alphaheart.backend.core.types.Member
import com.storyteller_f.alphaheart.backend.core.types.RawCommunity
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.query.bindPaginationQuery
import com.storyteller_f.alphaheart.backend.exposed.tables.Aids
import com.storyteller_f.alphaheart.backend.exposed.tables.Communities
import com.storyteller_f.alphaheart.backend.exposed.tables.Members
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.map
import com.storyteller_f.result.mapPresent
import com.storyteller_f.shared.model.PosterSearch
import com.storyteller_f.shared.obj.UpdateCommunityBody
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.andWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update

class ExposedCommunityDatabase(val databaseSession: ExposedDatabaseSession, val containerDatabase: ContainerDatabase) :
    CommunityDatabase {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getRawCommunity(
        objectFetch: ObjectFetch,
        fillJoinInfo: Boolean?,
        uid: PrimaryKey?,
    ): CustomResult<RawCommunity?, DatabaseError> {
        if (uid == null && fillJoinInfo == true) {
            return CustomResult.Failure(DatabaseError.from(UnauthorizedException()))
        }

        return getCommunityByPredicate {
            where {
                when (objectFetch) {
                    is ObjectFetch.AidFetch -> Aids.value eq objectFetch.aid
                    is ObjectFetch.IdFetch -> Communities.id eq objectFetch.id
                }
            }
        }.andThenPresent(DatabaseError::fromError) { community ->
            processCommunityToRawCommunity(uid, listOf(community))
        }.mapPresent {
            it.first()
        }
    }

    override suspend fun getJoinedCommunityIds(uid: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                Communities
                    .join(Members, JoinType.INNER, Communities.id, Members.objectId) {
                        Members.uid eq uid
                    }.select(Communities.id)
            }
            map {
                it[Communities.id]
            }
        }

    override suspend fun getCommunityPaginationResult(
        hasPosterSearch: PosterSearch?,
        primaryKeyFetch: PrimaryKeyFetch,
        joinSearch: JoinSearch,
    ) = paginationFromResults(
        getCommunityListByPredicate {
            buildCommunitySearchQuery(joinSearch, hasPosterSearch)
                .bindPaginationQuery(Communities, primaryKeyFetch)
        },
        getCommunityCountByPredicate {
            buildCommunitySearchQuery(joinSearch, hasPosterSearch)
        },
    ).mapPagingResultNotNull(DatabaseError::fromError) { list ->
        processCommunityToRawCommunity(
            when (joinSearch) {
                is JoinSearch.Joined -> joinSearch.uid
                is JoinSearch.Unspecified -> joinSearch.uid
            },
            list,
        )
    }

    suspend fun processCommunityToRawCommunity(uid: PrimaryKey?, communities: List<Community>) =
        containerDatabase.getContainerInfo(communities.map { it.id }, uid).map { map ->
            communities.map {
                val containerInfo = map[it.id]
                RawCommunity(
                    it,
                    containerInfo?.member,
                    containerInfo?.userTopicRead?.topicId,
                    containerInfo?.memberCount,
                    containerInfo?.latestTopicId,
                    containerInfo?.favoriteId,
                    containerInfo?.subscriptionId,
                )
            }
        }

    override suspend fun createCommunity(
        community: Community,
        memberId: PrimaryKey,
    ): CustomResult<Pair<Community, Member>, DatabaseError> =
        databaseSession.dbQuery {
            check(
                Communities.insert {
                    it[Communities.id] = community.id
                    it[Communities.name] = community.name
                    it[Communities.owner] = community.owner
                    it[Communities.createdTime] = community.createdTime
                    it[Communities.memberPolicy] = community.memberPolicy
                    community.fontSettings?.let { fs ->
                        it[Communities.fontSettings] = json.encodeToString(fs)
                    }
                }.insertedCount > 0,
            ) {
                "insert community failed"
            }
            check(
                Aids.insert {
                    it[value] = community.aid
                    it[objectId] = community.id
                    it[objectType] = ObjectType.COMMUNITY
                }.insertedCount > 0,
            ) {
                "insert aid failed"
            }
            val member =
                Member(
                    memberId,
                    community.owner,
                    community.id,
                    ObjectType.COMMUNITY,
                    community.createdTime,
                    MemberStatus.JOINED,
                    community.createdTime,
                )
            check(
                Members.insert {
                    it[id] = member.id
                    it[createdTime] = member.createdTime
                    it[joinedTime] = member.joinedTime
                    it[invitedTime] = member.invitedTime
                    it[uid] = member.uid
                    it[objectId] = member.objectId
                    it[objectType] = member.objectType
                    it[status] = member.status
                }.insertedCount > 0,
            ) {
                "join failed"
            }
            community to member
        }

    override suspend fun getRawCommunities(
        objectListFetch: ObjectListFetch,
    ): CustomResult<List<RawCommunity>, DatabaseError> {
        if (objectListFetch is ObjectListFetch.AidListFetch && objectListFetch.aidList.isEmpty()) {
            return CustomResult.success(emptyList())
        }
        if (objectListFetch is ObjectListFetch.IdListFetch && objectListFetch.idList.isEmpty()) {
            return CustomResult.success(emptyList())
        }
        return getCommunityListByPredicate {
            where {
                when (objectListFetch) {
                    is ObjectListFetch.AidListFetch -> Aids.value inList objectListFetch.aidList
                    is ObjectListFetch.IdListFetch -> Communities.id inList objectListFetch.idList
                }
            }
        }.andThen(DatabaseError::fromError) {
            processCommunityToRawCommunity(null, it)
        }
    }

    override suspend fun updateCommunity(id: PrimaryKey, body: UpdateCommunityBody) =
        databaseSession.dbQuery {
            val newIcon = body.icon
            val newName = body.name
            val newPoster = body.poster
            val newFontSettings = body.fontSettings
            if (!newName.isNullOrBlank() || newIcon != null || newPoster != null || newFontSettings != null) {
                Communities.update({
                    Communities.id eq id
                }) {
                    if (!newName.isNullOrBlank()) {
                        it[Communities.name] = newName
                    }
                    if (newIcon != null) {
                        it[Communities.icon] = newIcon
                    }
                    if (newPoster != null) {
                        it[Communities.poster] = newPoster
                    }
                    if (newFontSettings != null) {
                        it[Communities.fontSettings] = json.encodeToString(newFontSettings)
                    }
                } > 0
            } else {
                true
            }
        }

    override suspend fun updateCommunityStatus(id: PrimaryKey, status: ObjectStatus) =
        databaseSession.dbQuery {
            Communities.update({ Communities.id eq id }) {
                it[Communities.status] = status
            } > 0
        }

    override suspend fun getCommunityCount() =
        databaseSession.dbSearch {
            search {
                Communities.selectAll()
            }
            count()
        }

    private suspend fun getCommunityListByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                Communities
                    .join(Aids, JoinType.INNER, Communities.id, Aids.objectId)
                    .select(Communities.fields + Aids.value)
                    .queryBuilder()
            }
            map(Community::wrapRow)
        }

    private suspend fun getCommunityByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                Communities
                    .join(Aids, JoinType.INNER, Communities.id, Aids.objectId)
                    .select(Communities.fields + Aids.value)
                    .queryBuilder()
            }
            first(Community::wrapRow)
        }

    private suspend fun getCommunityCountByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                Communities.select(Communities.id).queryBuilder()
            }
            count()
        }

    fun Query.bindPosterSearch(hasPosterSearch: PosterSearch?): Query =
        when (hasPosterSearch) {
            PosterSearch.HAS_POSTER ->
                andWhere {
                    Communities.poster.isNotNull()
                }

            PosterSearch.NO_POSTER ->
                andWhere {
                    Communities.poster.isNull()
                }

            else -> {
                orderBy(Communities.poster.isNull(), SortOrder.ASC)
            }
        }

    fun Query.buildCommunitySearchQuery(joinSearch: JoinSearch, hasPosterSearch: PosterSearch?): Query {
        when (joinSearch) {
            is JoinSearch.Joined -> {
                adjustColumnSet {
                    join(Members, JoinType.INNER, Communities.id, Members.objectId) {
                        Members.uid eq joinSearch.uid
                    }
                }
            }

            else -> {
            }
        }
        return bindPosterSearch(hasPosterSearch)
    }
}
