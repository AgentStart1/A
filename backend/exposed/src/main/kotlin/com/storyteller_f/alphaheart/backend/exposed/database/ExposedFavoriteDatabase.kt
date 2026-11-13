/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.FavoriteDatabase
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.UserFavorite
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.query.bindPaginationQuery
import com.storyteller_f.alphaheart.backend.exposed.tables.UserFavorites
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll

class ExposedFavoriteDatabase(private val databaseSession: ExposedDatabaseSession) : FavoriteDatabase {
    override suspend fun getUserFavorites(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
        paginationFromResults(
            getFavoriteListByPredicate {
                where { UserFavorites.uid eq uid }
                    .bindPaginationQuery(UserFavorites, fetch)
            },
            getFavoriteCountByPredicate { where { UserFavorites.uid eq uid } },
        )

    override suspend fun addFavorite(userFavorite: UserFavorite) =
        databaseSession.dbQuery {
            check(
                UserFavorites.insert {
                    it[UserFavorites.id] = userFavorite.id
                    it[UserFavorites.uid] = userFavorite.uid
                    it[UserFavorites.objectId] = userFavorite.objectId
                    it[UserFavorites.objectType] = userFavorite.objectType
                    it[UserFavorites.createdTime] = userFavorite.createdTime
                }.insertedCount > 0,
            ) {
                "Insert favorite failed"
            }
            userFavorite
        }

    override suspend fun removeFavorite(id: PrimaryKey) =
        databaseSession.dbQuery {
            check(
                UserFavorites.deleteWhere {
                    UserFavorites.id eq id
                } > 0,
            ) {
                "Remove favorite failed"
            }
        }

    override suspend fun getFavorite(id: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UserFavorites.selectAll().where {
                    UserFavorites.id eq id
                }
            }
            first {
                UserFavorite.wrapRow(it)
            }
        }

    override suspend fun getFavorite(uid: PrimaryKey, objectId: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UserFavorites.selectAll().where {
                    UserFavorites.uid eq uid and (UserFavorites.objectId eq objectId)
                }
            }
            first {
                UserFavorite.wrapRow(it)
            }
        }

    override suspend fun getHasFavorite(idList: ObjectListFetch.IdListFetch, uid: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UserFavorites.selectAll().where {
                    UserFavorites.uid eq uid and (UserFavorites.objectId inList idList.idList)
                }
            }
            map {
                UserFavorite.wrapRow(it)
            }
        }

    override suspend fun getUserFavoriteCount() =
        databaseSession.dbSearch {
            search {
                UserFavorites.selectAll()
            }
            count()
        }

    private suspend fun getFavoriteListByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                UserFavorites.selectAll().queryBuilder()
            }
            map(UserFavorite::wrapRow)
        }

    private suspend fun getFavoriteCountByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                UserFavorites.selectAll().queryBuilder()
            }
            count()
        }
}
