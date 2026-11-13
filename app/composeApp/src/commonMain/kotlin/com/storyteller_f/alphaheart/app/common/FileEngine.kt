/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** Files are private download-cache entries. A location is interpreted only by its platform engine. */
internal interface FileEngine {
    fun location(key: String): String
    suspend fun size(path: String): Long?
    suspend fun read(path: String, limit: Long): ByteArray
    suspend fun openWriter(path: String, append: Boolean): DownloadFileWriter
    suspend fun listFiles(path: String): List<DownloadFileEntry>
    suspend fun delete(path: String)
    suspend fun acquire(key: String): DownloadFileLease
    suspend fun extractArchive(path: String)
}

internal interface DownloadFileWriter {
    suspend fun write(bytes: ByteArray)
    suspend fun close()
}

internal fun interface DownloadFileLease {
    suspend fun close()
}

@Serializable
internal data class DownloadFileEntry(val path: String, val size: Long)

internal expect fun createFileEngine(): FileEngine

internal suspend fun <T> FileEngine.withLock(key: String, block: suspend () -> T): T {
    val lease = acquire(key)
    return withDownloadCleanup({ withContext(NonCancellable) { lease.close() } }) { block() }
}

internal suspend fun <T> FileEngine.withWriter(
    path: String,
    append: Boolean,
    block: suspend (DownloadFileWriter) -> T,
): T {
    val writer = openWriter(path, append)
    return withDownloadCleanup({ withContext(NonCancellable) { writer.close() } }) { block(writer) }
}

/** Like use: preserve the operation failure if closing also fails. Callers own cleanup dispatching. */
internal inline fun <T> withDownloadCleanup(cleanup: () -> Unit, block: () -> T): T =
    withDownloadFinalizer({ cleanup() }, block)

/** Finalize with the primary failure, then unconditionally propagate that failure, including cancellation. */
internal inline fun <T> withDownloadFinalizer(finalize: (Throwable?) -> Unit, block: () -> T): T {
    val result =
        try {
            Result.success(block())
        } catch (failure: Throwable) {
            Result.failure(failure)
        }
    val failure = result.exceptionOrNull()
    completeDownloadCleanup(failure) { finalize(failure) }
    return result.getOrThrow()
}

/** Preserve a primary operation failure when cleanup also fails. Callers own cleanup dispatching. */
internal inline fun completeDownloadCleanup(failure: Throwable?, cleanup: () -> Unit) {
    try {
        cleanup()
    } catch (closeFailure: Throwable) {
        if (failure == null) throw closeFailure
        if (closeFailure !== failure) failure.addSuppressed(closeFailure)
    }
}

internal suspend fun FileEngine.write(path: String, bytes: ByteArray) {
    withWriter(path, append = false) { it.write(bytes) }
}

/** Callers hold the server/file lock and check PROCESSED before consuming this directory. */
internal suspend fun FileEngine.extractedFiles(path: String): List<DownloadFileEntry>? =
    listFiles("$path.extracted").takeIf { it.isNotEmpty() }
