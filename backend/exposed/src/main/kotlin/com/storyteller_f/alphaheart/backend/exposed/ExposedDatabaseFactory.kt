/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed

import com.storyteller_f.alphaheart.backend.core.DatabaseConnection
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.exposed.tables.Aids
import com.storyteller_f.alphaheart.backend.exposed.tables.AssetTransactions
import com.storyteller_f.alphaheart.backend.exposed.tables.BackendConfigs
import com.storyteller_f.alphaheart.backend.exposed.tables.ChildAccounts
import com.storyteller_f.alphaheart.backend.exposed.tables.Communities
import com.storyteller_f.alphaheart.backend.exposed.tables.EncryptedKeys
import com.storyteller_f.alphaheart.backend.exposed.tables.FileRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.FileRefs
import com.storyteller_f.alphaheart.backend.exposed.tables.Members
import com.storyteller_f.alphaheart.backend.exposed.tables.PanelAccounts
import com.storyteller_f.alphaheart.backend.exposed.tables.PanelLogs
import com.storyteller_f.alphaheart.backend.exposed.tables.Quotas
import com.storyteller_f.alphaheart.backend.exposed.tables.ReactionRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.Reactions
import com.storyteller_f.alphaheart.backend.exposed.tables.Rooms
import com.storyteller_f.alphaheart.backend.exposed.tables.SubscriptionSentLogs
import com.storyteller_f.alphaheart.backend.exposed.tables.TaskRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.Titles
import com.storyteller_f.alphaheart.backend.exposed.tables.Topics
import com.storyteller_f.alphaheart.backend.exposed.tables.UploadRecords
import com.storyteller_f.alphaheart.backend.exposed.tables.UserDevices
import com.storyteller_f.alphaheart.backend.exposed.tables.UserFavorites
import com.storyteller_f.alphaheart.backend.exposed.tables.UserLogs
import com.storyteller_f.alphaheart.backend.exposed.tables.UserSubscriptions
import com.storyteller_f.alphaheart.backend.exposed.tables.UserTopicReads
import com.storyteller_f.alphaheart.backend.exposed.tables.UserTwoFactors
import com.storyteller_f.alphaheart.backend.exposed.tables.Users
import com.storyteller_f.alphaheart.backend.exposed.tables.WorkerTasks
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.obj.ExplainResult
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.Query
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.explain
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import java.io.PrintWriter
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.ExperimentalTime

class DatabaseSearchConfig<R, Q> {
    lateinit var searchFunc: () -> Q
    lateinit var transformFunc: suspend Q.() -> R

    fun search(block: () -> Q) {
        searchFunc = block
    }

    fun transform(block: suspend Q.() -> R) {
        transformFunc = block
    }
}

fun <R> DatabaseSearchConfig<R?, Query>.first(block: (ResultRow) -> R) {
    transformFunc = {
        firstOrNull()?.let {
            block(it)
        }
    }
}

fun <R> DatabaseSearchConfig<R, Query>.firstNotNull(block: (ResultRow) -> R) {
    transform {
        block(first())
    }
}

fun <T> DatabaseSearchConfig<List<T>, Query>.map(block: (ResultRow) -> T) {
    transform {
        toList().map {
            block(it)
        }.toList()
    }
}

fun DatabaseSearchConfig<Long, Query>.count() {
    transform {
        count()
    }
}

fun DatabaseSearchConfig<Boolean, Query>.isEmpty() {
    transform {
        count() == 0L
    }
}

fun DatabaseSearchConfig<Boolean, Query>.isNotEmpty() {
    transform {
        count() != 0L
    }
}

class ExposedDatabaseSession(val database: R2dbcDatabase, val port: Int?) {
    private fun handleDatabaseException(e: Throwable, anchor: Throwable): Throwable {
        if (e is UnauthorizedException || e.isDup()) {
            return e
        }
        anchor.initCause(e)
        return anchor
    }

    suspend fun <T> dbQuery(block: suspend R2dbcTransaction.() -> T): CustomResult<T, DatabaseError> {
        val anchor = Exception("dbQuery failed")
        return captureFailure(DatabaseError::from) {
            withContext(Dispatchers.IO) {
                suspendTransaction(db = database) {
                    maxAttempts = 1
                    block()
                }
            }
        }.mapError { error ->
            val cause = handleDatabaseException(error.cause, anchor)
            if (cause.isDup()) DatabaseError.ConstraintViolation(cause) else DatabaseError.from(cause)
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun <R> dbSearch(block: DatabaseSearchConfig<R, Query>.() -> Unit): CustomResult<R, DatabaseError> {
        val anchor = Exception("dbSearch failed")
        port?.let { explainQuery(it, block) }
        return captureFailure(DatabaseError::from) {
            withContext(Dispatchers.IO) {
                suspendTransaction(db = database) {
                    maxAttempts = 1
                    val databaseSearchConfig = DatabaseSearchConfig<R, Query>()
                    databaseSearchConfig.apply(block).let {
                        it.transformFunc(it.searchFunc())
                    }
                }
            }
        }.mapError { error ->
            val cause = handleDatabaseException(error.cause, anchor)
            if (cause.isDup()) DatabaseError.ConstraintViolation(cause) else DatabaseError.from(cause)
        }
    }

    private suspend fun <R> explainQuery(port: Int, query: DatabaseSearchConfig<R, Query>.() -> Unit) {
        val anchor = Exception("explainQuery failed ${database.dialect.name}")
        try {
            val explainResult =
                withContext(Dispatchers.IO) {
                    suspendTransaction(db = database) {
                        maxAttempts = 1
                        explainQuery {
                            val databaseSearchConfig = DatabaseSearchConfig<R, Query>()
                            databaseSearchConfig.apply(query).searchFunc()
                        }
                    }
                }
            if (explainResult != null) {
                val result = explainResult.copy(stackTraceString = anchor.stackTraceToString())
                suspendCancellableCoroutine { continuation ->
                    thread {
                        sendExplainResult(result, continuation, port)
                    }
                }
            }
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            throw handleDatabaseException(e, anchor)
        }
        error("")
    }

    private fun sendExplainResult(result: ExplainResult, continuation: CancellableContinuation<Unit>, port: Int) {
        captureFailure(DatabaseError::from) {
            Socket("localhost", port).use { socket ->
                socket.getOutputStream().use {
                    PrintWriter(it, true).use { writer ->
                        writer.println(commonJson.encodeToString(result))
                    }
                }
            }
        }.onSuccess {
            continuation.resume(Unit)
        }.onFailure {
            continuation.resumeWithException(it.cause)
        }
    }

    private suspend fun R2dbcTransaction.explainQuery(block: () -> Query): ExplainResult? {
        debug = true
        val result =
            explain {
                block()
            }.toList().joinToString("\n")
        assert(statements.isNotEmpty())
        val input =
            statements.toString().split("\n").firstOrNull { statement ->
                statement.isNotEmpty() && !statement.contains("INFORMATION_SCHEMA")
            }
        return if (input != null) {
            val s = "explain"
            val end = input.indexOf(s, ignoreCase = true) + s.length
            val trimmedStatements = input.substring(end).trim()
            ExplainResult(db.dialect.name, trimmedStatements, result, "")
        } else {
            null
        }
    }
}

object ExposedDatabaseFactory {
    private val tables =
        arrayOf(
            Aids,
            ChildAccounts,
            AssetTransactions,
            Communities,
            EncryptedKeys,
            FileRefs,
            FileRecords,
            Members,
            Reactions,
            ReactionRecords,
            Rooms,
            Titles,
            Topics,
            Users,
            PanelLogs,
            UserLogs,
            UserDevices,
            UserTopicReads,
            UserTwoFactors,
            UserFavorites,
            UserSubscriptions,
            SubscriptionSentLogs,
            WorkerTasks,
            TaskRecords,
            Quotas,
            UploadRecords,
            PanelAccounts,
            BackendConfigs,
        )

    fun connect(connection: DatabaseConnection): R2dbcDatabase {
        Napier.d {
            "connect $connection"
        }
        return R2dbcDatabase.connect(connection.uri, connection.driver, connection.user, connection.password)
    }

    suspend fun init(database: R2dbcDatabase) {
        Napier.i(tag = "database") {
            "init"
        }
        suspendTransaction(db = database) {
            Napier.i(tag = "database") {
                "create tables"
            }
            SchemaUtils.create(*tables)
        }
    }

    suspend fun clean(database: R2dbcDatabase) {
        Napier.i(tag = "database") {
            "clean"
        }
        suspendTransaction(db = database) {
            Napier.i(tag = "database") {
                "drop tables"
            }
            SchemaUtils.drop(*tables)
        }
    }

    suspend fun migration(database: R2dbcDatabase) {
        Napier.i(tag = "database") {
            "migration"
        }
        suspendTransaction(database) {
            val statements = MigrationUtils.statementsRequiredForDatabaseMigration(*tables)
            statements.forEach {
                Napier.i(tag = "database") { "migration: $it" }
                exec(it)
            }
        }
    }
}
