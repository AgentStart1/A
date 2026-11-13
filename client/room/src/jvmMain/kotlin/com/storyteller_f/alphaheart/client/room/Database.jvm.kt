/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import java.io.File

actual fun getRoomDatabase(scope: String): AppDatabase {
    val builder = getDatabaseBuilder(scope)
    return builder
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .fallbackToDestructiveMigration(true)
        .fallbackToDestructiveMigrationFrom(true)
        .setDriver(LoggingSQLiteDriver(BundledSQLiteDriver()))
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

fun getDatabaseBuilder(scope: String): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "$scope.db")
    Napier.i {
        "database path: ${dbFile.absolutePath}"
    }
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
}
