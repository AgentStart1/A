/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.SubscriptionDatabase
import com.storyteller_f.alphaheart.backend.core.paginationFromResults
import com.storyteller_f.alphaheart.backend.core.types.SubscriptionSentLog
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.count
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.map
import com.storyteller_f.alphaheart.backend.exposed.query.bindPaginationQuery
import com.storyteller_f.alphaheart.backend.exposed.tables.SubscriptionSentLogs
import com.storyteller_f.alphaheart.backend.exposed.tables.UserSubscriptions
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll

class ExposedSubscriptionDatabase(private val databaseSession: ExposedDatabaseSession) : SubscriptionDatabase {
    override suspend fun getUserSubscriptions(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
        paginationFromResults(
            getSubscriptionListByPredicate {
                where { UserSubscriptions.uid eq uid }
                    .bindPaginationQuery(UserSubscriptions, fetch)
            },
            getSubscriptionCountByPredicate { where { UserSubscriptions.uid eq uid } },
        )

    override suspend fun addSubscription(userSubscription: UserSubscription) =
        databaseSession.dbQuery {
            check(
                UserSubscriptions.insert {
                    it[UserSubscriptions.id] = userSubscription.id
                    it[UserSubscriptions.uid] = userSubscription.uid
                    it[UserSubscriptions.objectId] = userSubscription.objectId
                    it[UserSubscriptions.objectType] = userSubscription.objectType
                    it[UserSubscriptions.createdTime] = userSubscription.createdTime
                }.insertedCount > 0,
            ) {
                "Insert subscription failed"
            }
            userSubscription
        }

    override suspend fun removeSubscription(id: PrimaryKey) =
        databaseSession.dbQuery {
            check(
                UserSubscriptions.deleteWhere {
                    UserSubscriptions.id eq id
                } > 0,
            ) {
                "Remove subscription failed"
            }
        }

    override suspend fun getSubscription(id: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UserSubscriptions.selectAll().where {
                    UserSubscriptions.id eq id
                }
            }
            first {
                UserSubscription.wrapRow(it)
            }
        }

    override suspend fun getSubscription(uid: PrimaryKey, objectId: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UserSubscriptions.selectAll().where {
                    UserSubscriptions.uid eq uid and (UserSubscriptions.objectId eq objectId)
                }
            }
            first {
                UserSubscription.wrapRow(it)
            }
        }

    override suspend fun getSubscriptionsByObjectId(objectId: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
        getSubscriptionListByPredicate {
            where { UserSubscriptions.objectId eq objectId }
                .bindPaginationQuery(UserSubscriptions, primaryKeyFetch)
        }

    override suspend fun insertSubscriptionSentLog(log: SubscriptionSentLog) =
        databaseSession.dbQuery {
            check(
                SubscriptionSentLogs.insert {
                    it[SubscriptionSentLogs.id] = log.id
                    it[SubscriptionSentLogs.uid] = log.uid
                    it[SubscriptionSentLogs.objectId] = log.objectId
                    it[SubscriptionSentLogs.objectType] = log.objectType
                    it[SubscriptionSentLogs.createdTime] = log.createdTime
                    it[SubscriptionSentLogs.subscriptionId] = log.subscriptionId
                }.insertedCount > 0,
            ) {
                "Insert subscription sent log failed"
            }
            log
        }

    override suspend fun getLatestSubscriptionSentLog(objectId: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                SubscriptionSentLogs.selectAll().where {
                    SubscriptionSentLogs.objectId eq objectId
                }.orderBy(SubscriptionSentLogs.subscriptionId to SortOrder.DESC)
            }
            first {
                SubscriptionSentLog.wrapRow(it)
            }
        }

    override suspend fun getHasSubscription(idList: ObjectListFetch.IdListFetch, uid: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UserSubscriptions.selectAll().where {
                    UserSubscriptions.uid eq uid and (UserSubscriptions.objectId inList idList.idList)
                }
            }
            map {
                UserSubscription.wrapRow(it)
            }
        }

    override suspend fun getUserSubscriptionCount(uid: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                UserSubscriptions.selectAll().where {
                    UserSubscriptions.uid eq uid
                }
            }
            count()
        }

    private suspend fun getSubscriptionListByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                UserSubscriptions.selectAll().queryBuilder()
            }
            map(UserSubscription::wrapRow)
        }

    private suspend fun getSubscriptionCountByPredicate(queryBuilder: Query.() -> Query = { this }) =
        databaseSession.dbSearch {
            search {
                UserSubscriptions.selectAll().queryBuilder()
            }
            count()
        }
}
