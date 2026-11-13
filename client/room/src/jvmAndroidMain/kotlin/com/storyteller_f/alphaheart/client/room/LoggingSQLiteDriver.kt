/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import io.github.aakira.napier.Napier

class LoggingSQLiteDriver(private val delegate: SQLiteDriver) : SQLiteDriver {
    override fun open(fileName: String): SQLiteConnection = LoggingSQLiteConnection(delegate.open(fileName))
}

private class LoggingSQLiteConnection(private val delegate: SQLiteConnection) : SQLiteConnection {
    override fun inTransaction(): Boolean = delegate.inTransaction()

    override fun prepare(sql: String): SQLiteStatement = LoggingSQLiteStatement(delegate.prepare(sql), sql)

    override fun close() = delegate.close()
}

private class LoggingSQLiteStatement(private val delegate: SQLiteStatement, private val sql: String) :
    SQLiteStatement {
    private val bindArgs = mutableMapOf<Int, Any?>()

    override fun bindBlob(index: Int, value: ByteArray) {
        bindArgs[index] = "<blob ${value.size} bytes>"
        delegate.bindBlob(index, value)
    }

    override fun bindDouble(index: Int, value: Double) {
        bindArgs[index] = value
        delegate.bindDouble(index, value)
    }

    override fun bindLong(index: Int, value: Long) {
        bindArgs[index] = value
        delegate.bindLong(index, value)
    }

    override fun bindText(index: Int, value: String) {
        bindArgs[index] = value
        delegate.bindText(index, value)
    }

    override fun bindNull(index: Int) {
        bindArgs[index] = null
        delegate.bindNull(index)
    }

    override fun getBlob(index: Int) = delegate.getBlob(index)
    override fun getDouble(index: Int) = delegate.getDouble(index)
    override fun getLong(index: Int) = delegate.getLong(index)
    override fun getText(index: Int) = delegate.getText(index)
    override fun isNull(index: Int) = delegate.isNull(index)
    override fun getColumnCount() = delegate.getColumnCount()
    override fun getColumnName(index: Int) = delegate.getColumnName(index)
    override fun getColumnType(index: Int) = delegate.getColumnType(index)

    override fun step(): Boolean {
        log()
        return delegate.step()
    }

    override fun reset() {
        bindArgs.clear()
        delegate.reset()
    }

    override fun clearBindings() {
        bindArgs.clear()
        delegate.clearBindings()
    }

    override fun close() = delegate.close()

    private fun log() {
        if (bindArgs.isEmpty()) {
            Napier.d(tag = "RoomQuery") { sql }
        } else {
            val args = bindArgs.entries.sortedBy { it.key }.joinToString { "${it.value ?: "<none>"}" }
            Napier.d(tag = "RoomQuery") { "$sql [$args]" }
        }
    }
}
