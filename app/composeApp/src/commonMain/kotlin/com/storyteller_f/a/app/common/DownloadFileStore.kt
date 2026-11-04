/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.common

import com.storyteller_f.shared.commonJson
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

/** Files are private download-cache entries. A location is interpreted only by its platform store. */
internal interface DownloadFileStore {
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

@Serializable
internal data class DownloadArchiveIndex(val directory: String, val entries: List<DownloadFileEntry>)

internal expect fun createDownloadFileStore(): DownloadFileStore

internal suspend fun <T> DownloadFileStore.withLock(key: String, block: suspend () -> T): T {
    val lease = acquire(key)
    return withDownloadCleanup({ withContext(NonCancellable) { lease.close() } }) { block() }
}

internal suspend fun <T> DownloadFileStore.withWriter(
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

internal suspend fun DownloadFileStore.write(path: String, bytes: ByteArray) {
    withWriter(path, append = false) { it.write(bytes) }
}

internal suspend fun DownloadFileStore.readArchiveIndex(path: String): DownloadArchiveIndex? {
    val index = "$path.extracted.json"
    if (size(index) == null) return null
    return try {
        commonJson.decodeFromString<DownloadArchiveIndex>(read(index, 64L * 1024 * 1024).decodeToString())
    } catch (_: SerializationException) {
        null
    }
}

/** A complete manifest points to one fully written generation, never a partially extracted directory. */
internal suspend fun DownloadFileStore.extractedFiles(path: String): List<DownloadFileEntry>? {
    val index = readArchiveIndex(path)
    val entries = index?.entries ?: listFiles("$path.extracted").takeIf { it.isNotEmpty() } ?: return null
    return entries.takeIf { files -> files.all { size(it.path) == it.size } }
}
