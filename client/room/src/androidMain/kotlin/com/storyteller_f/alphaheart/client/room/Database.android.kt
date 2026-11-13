/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.storyteller_f.shared.getAppContextRefValue
import kotlinx.coroutines.Dispatchers

actual fun getRoomDatabase(scope: String): AppDatabase {
    val ctx = checkNotNull(getAppContextRefValue()) { "Application context is not initialized" }
    val builder = getDatabaseBuilder(ctx, scope)
    return builder
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .fallbackToDestructiveMigration(true)
        .fallbackToDestructiveMigrationFrom(true)
        .setDriver(LoggingSQLiteDriver(BundledSQLiteDriver()))
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

fun getDatabaseBuilder(ctx: Context, scope: String): RoomDatabase.Builder<AppDatabase> {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("$scope.db")
    return Room.databaseBuilder<AppDatabase>(context = appContext, name = dbFile.absolutePath)
}
