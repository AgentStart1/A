/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import com.storyteller.alphaheart.client.room.CancellationSafeSQLiteDriver
import io.github.aakira.napier.Napier
import org.w3c.dom.Worker
import kotlin.js.ExperimentalWasmJsInterop

actual fun getRoomDatabase(scope: String): AppDatabase {
    Napier.i { "wasmJs room database: $scope.db" }
    return getDatabaseBuilder(scope)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .fallbackToDestructiveMigration(true)
        .fallbackToDestructiveMigrationFrom(true)
        .setDriver(CancellationSafeSQLiteDriver(WebWorkerSQLiteDriver(createWorker())))
        .setSingleConnectionPool()
        .build()
}

fun getDatabaseBuilder(scope: String): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(
        name = "$scope.db",
    )

// 由本地 npm 包 sqlite-web-worker 提供，实现 WebWorkerSQLiteDriver 协议（SQLite WASM + OPFS）。
// worker 是 ES module，需以 { type: "module" } 创建；URL 由 webpack 解析并单独打包。
@OptIn(ExperimentalWasmJsInterop::class)
private fun createWorker(): Worker =
    js("""new Worker(new URL("sqlite-web-worker/worker.js", import.meta.url), { type: "module" })""")
