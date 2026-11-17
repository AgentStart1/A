/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.room

import androidx.room3.useReaderConnection
import androidx.room3.withWriteTransaction
import kotlinx.browser.window
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.resume
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class BrowserDatabaseConcurrencyTest {
    @Test
    fun browserDatabaseUsesConcurrentJournalMode() =
        runTest {
            val database = getRoomDatabase("journal-${Random.nextLong()}")
            try {
                val mode =
                    database.useReaderConnection { connection ->
                        connection.usePrepared("PRAGMA journal_mode") { statement ->
                            statement.step()
                            statement.getText(0)
                        }
                    }
                assertEquals("truncate", mode)
            } finally {
                database.close()
            }
        }

    @Test
    fun concurrentTransactionWaitsUntilCommit() =
        runTest {
            val name = "transaction-${Random.nextLong()}"
            val first = getRoomDatabase(name)
            val second = getRoomDatabase(name)
            try {
                first.getCommonDao().get("shared", "first")
                second.getCommonDao().get("shared", "first")
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                coroutineScope {
                    val owner =
                        async {
                            first.withWriteTransaction {
                                first.getCommonDao().insert(CommonEntity("first", "shared", "committed"))
                                entered.complete(Unit)
                                release.await()
                            }
                        }
                    entered.await()
                    val waiting =
                        async {
                            second.getCommonDao().insert(CommonEntity("second", "shared", "committed"))
                            "finished"
                        }
                    try {
                        waitPastSQLiteBusyTimeout()
                        assertFalse(waiting.isCompleted)
                        assertEquals(
                            1,
                            databaseLockProbe().pending(
                                "alphaheart.sqlite:/$name.db".toJsString(),
                            ).await().toDouble().toInt(),
                        )
                    } finally {
                        release.complete(Unit)
                    }
                    owner.await()
                    waiting.await()
                }
                assertEquals("committed", first.getCommonDao().get("shared", "second")?.data)
            } finally {
                first.close()
                second.close()
            }
        }

    @Test
    fun concurrentWorkersPersistEveryTransaction() =
        runTest {
            val name = "concurrent-${Random.nextLong()}"
            val databases = List(3) { getRoomDatabase(name) }
            try {
                coroutineScope {
                    databases.mapIndexed { writer, database ->
                        async {
                            repeat(12) { index ->
                                val id = "$writer-$index"
                                database.getCommonDao().insert(CommonEntity(id, "shared", id))
                            }
                        }
                    }.awaitAll()
                }
                databases.forEach { database ->
                    repeat(3) { writer ->
                        repeat(12) { index ->
                            val id = "$writer-$index"
                            assertEquals(id, database.getCommonDao().get("shared", id)?.data)
                        }
                    }
                }
            } finally {
                databases.forEach { it.close() }
            }
            val reopened = getRoomDatabase(name)
            try {
                assertEquals("2-11", reopened.getCommonDao().get("shared", "2-11")?.data)
            } finally {
                reopened.close()
            }
        }
    private suspend fun waitPastSQLiteBusyTimeout() =
        suspendCancellableCoroutine { continuation ->
            // Use a browser clock: coroutine virtual time cannot prove contention in real workers.
            val timer =
                window.setTimeout({
                    continuation.resume(Unit)
                    null
                }, 4_000)
            continuation.invokeOnCancellation { window.clearTimeout(timer) }
        }
}

private external interface DatabaseLockProbe : JsAny {
    fun pending(key: JsString): Promise<JsNumber>
}

private fun databaseLockProbe(): DatabaseLockProbe =
    js(
        """({async pending(key) {
        const locks = await navigator.locks.query();
        return locks.pending.filter(lock => lock.name === key).length;
    }})""",
    )
