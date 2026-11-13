/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.room

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

private val transactionFinalizers = setOf("ROLLBACK", "END", "COMMIT", "RELEASE")

internal class CancellationSafeSQLiteDriver(private val delegate: SQLiteDriver) : SQLiteDriver by delegate {
    override suspend fun open(fileName: String): SQLiteConnection =
        awaitWorkerResponse(onCancelled = { it.close() }) {
            CancellationSafeSQLiteConnection(delegate.open(fileName))
        }
}

private class CancellationSafeSQLiteConnection(private val delegate: SQLiteConnection) : SQLiteConnection by delegate {
    override suspend fun prepare(sql: String): SQLiteStatement {
        val finalizer =
            sql.trimStart().takeWhile { !it.isWhitespace() && it != ';' }.uppercase() in transactionFinalizers
        return if (finalizer) {
            awaitWorkerFinalizer { CancellationSafeSQLiteStatement(delegate.prepare(sql), true) }
        } else {
            awaitWorkerResponse(onCancelled = { it.close() }) {
                CancellationSafeSQLiteStatement(delegate.prepare(sql), false)
            }
        }
    }
}

private class CancellationSafeSQLiteStatement(private val delegate: SQLiteStatement, private val finalizer: Boolean) :
    SQLiteStatement by delegate {
    override suspend fun step(): Boolean =
        if (finalizer) awaitWorkerFinalizer(delegate::step) else awaitWorkerResponse(request = delegate::step)
}

internal suspend fun <T> awaitWorkerResponse(onCancelled: (T) -> Unit = {}, request: suspend () -> T): T {
    currentCoroutineContext().ensureActive()
    val result = withContext(NonCancellable) { request() }
    var delivered = false
    try {
        currentCoroutineContext().ensureActive()
        delivered = true
        return result
    } finally {
        if (!delivered) onCancelled(result)
    }
}

// Room recycles a connection only after rollback succeeds. Cleanup must finish even when its caller
// is cancelled, otherwise the single-connection pool loses its permit and all later queries hang.
internal suspend fun <T> awaitWorkerFinalizer(request: suspend () -> T): T = withContext(NonCancellable) { request() }
