/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.database

import com.storyteller_f.alphaheart.backend.core.PanelAccountDatabase
import com.storyteller_f.alphaheart.backend.core.UserAuthData
import com.storyteller_f.alphaheart.backend.core.types.PanelAccount
import com.storyteller_f.alphaheart.backend.core.types.RawPanelAccount
import com.storyteller_f.alphaheart.backend.exposed.ExposedDatabaseSession
import com.storyteller_f.alphaheart.backend.exposed.first
import com.storyteller_f.alphaheart.backend.exposed.isEmpty
import com.storyteller_f.alphaheart.backend.exposed.tables.PanelAccounts
import com.storyteller_f.alphaheart.backend.exposed.tables.wrapRow
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.md5
import com.storyteller_f.shared.utils.now
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll

class ExposedPanelAccountDatabase(val databaseSession: ExposedDatabaseSession) : PanelAccountDatabase {
    override suspend fun getPanelAccount(id: PrimaryKey) =
        databaseSession.dbSearch {
            search {
                PanelAccounts.selectAll().where {
                    PanelAccounts.id eq id
                }
            }
            first(PanelAccount::wrapRow)
        }

    override suspend fun addPanelAccount(panelAccount: PanelAccount) =
        databaseSession.dbQuery {
            check(
                PanelAccounts.insert {
                    it[id] = panelAccount.id
                    it[name] = panelAccount.name
                    it[createdTime] = now()
                    it[passType] = panelAccount.passType
                    it[algoType] = panelAccount.algoType
                    it[publicKey] = panelAccount.publicKey
                    it[publicKeyMd5] = md5(panelAccount.publicKey)
                    it[address] = panelAccount.address
                }.insertedCount > 0,
            ) {
                "Failed to add panel account"
            }
        }

    private suspend fun getUserAuthDataBy(predicate: () -> Op<Boolean>) =
        databaseSession.dbSearch {
            search {
                PanelAccounts.select(
                    listOf(PanelAccounts.publicKey, PanelAccounts.id, PanelAccounts.algoType),
                ).where(predicate)
            }
            first {
                UserAuthData(it[PanelAccounts.publicKey], it[PanelAccounts.id], it[PanelAccounts.algoType])
            }
        }

    override suspend fun getUserAuthDataById(id: PrimaryKey) =
        getUserAuthDataBy {
            PanelAccounts.id eq id
        }

    override suspend fun getUserAuthDataByAddress(address: String) =
        getUserAuthDataBy {
            PanelAccounts.address eq address
        }

    override suspend fun getRawUserAndPublicKeyByAddress(ad: String) =
        databaseSession.dbSearch {
            search {
                PanelAccounts
                    .selectAll()
                    .where {
                        PanelAccounts.address eq ad
                    }
            }
            first {
                val value = PanelAccount.wrapRow(it)
                Pair(RawPanelAccount(value.id, value.name, value.algoType), value.publicKey)
            }
        }

    override suspend fun isUserNotExistsByPublicKey(pk: String) =
        databaseSession.dbSearch {
            search {
                val publicKeyMd5 = md5(pk)
                PanelAccounts.selectAll().where {
                    PanelAccounts.publicKeyMd5 eq publicKeyMd5
                }
            }
            isEmpty()
        }
}
