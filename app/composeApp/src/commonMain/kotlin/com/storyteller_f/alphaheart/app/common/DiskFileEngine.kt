/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory

internal class DiskFileEngine(
    private val dispatcher: CoroutineDispatcher,
    private val createArchiveSink: (Path) -> Sink,
) : FileEngine {
    override fun location(key: String): String = Path(SystemTemporaryDirectory, "downloads", key).toString()

    override suspend fun size(path: String): Long? =
        withContext(dispatcher) {
            SystemFileSystem.metadataOrNull(Path(path))?.takeUnless { it.isDirectory }?.size
        }

    override suspend fun read(path: String, limit: Long): ByteArray =
        withContext(dispatcher) {
            SystemFileSystem.source(Path(path)).buffered().use { it.readDownloadBytes(limit) }
        }

    override suspend fun openWriter(path: String, append: Boolean): DownloadFileWriter {
        var opened: DownloadFileWriter? = null
        var delivered = false
        return withDownloadCleanup({ withContext(NonCancellable) { if (!delivered) opened?.close() } }) {
            withContext(dispatcher) {
                val target = Path(path)
                target.parent?.let { SystemFileSystem.createDirectories(it) }
                DiskDownloadWriter(
                    SystemFileSystem.sink(target, append = append).buffered(),
                    dispatcher,
                ).also { opened = it }
            }.also { delivered = true }
        }
    }

    override suspend fun listFiles(path: String): List<DownloadFileEntry> =
        withContext(dispatcher) {
            fun list(target: Path): List<DownloadFileEntry> {
                val metadata = SystemFileSystem.metadataOrNull(target) ?: return emptyList()
                return if (metadata.isDirectory) {
                    SystemFileSystem.list(target).flatMap(::list)
                } else {
                    listOf(DownloadFileEntry(target.toString(), metadata.size))
                }
            }
            list(Path(path))
        }

    override suspend fun delete(path: String) =
        withContext(dispatcher) {
            fun remove(target: Path) {
                val metadata = SystemFileSystem.metadataOrNull(target) ?: return
                if (metadata.isDirectory) SystemFileSystem.list(target).forEach(::remove)
                SystemFileSystem.delete(target)
            }
            remove(Path(path))
        }

    override suspend fun acquire(key: String): DownloadFileLease = acquireDiskDownloadLock(key)

    override suspend fun extractArchive(path: String) =
        withContext(dispatcher) {
            extractDownloadArchive(path) { directory ->
                extractDiskArchive(path, directory, createArchiveSink)
            }
        }
}

private class DiskDownloadWriter(private val sink: Sink, private val dispatcher: CoroutineDispatcher) :
    DownloadFileWriter {
    override suspend fun write(bytes: ByteArray) = withContext(dispatcher) { sink.write(bytes) }
    override suspend fun close() = withContext(dispatcher) { sink.close() }
}

private val diskLockRegistry = Mutex()
private val diskLocks = mutableMapOf<String, DiskDownloadLock>()
private class DiskDownloadLock {
    val mutex = Mutex()
    private var users = 0

    fun retain() {
        users++
    }

    fun release(): Boolean {
        users--
        return users == 0
    }
}

private suspend fun acquireDiskDownloadLock(key: String): DownloadFileLease {
    val lock = diskLockRegistry.withLock { diskLocks.getOrPut(key) { DiskDownloadLock() }.also { it.retain() } }
    var acquired = false
    withDownloadCleanup({ withContext(NonCancellable) { if (!acquired) releaseDiskDownloadLock(key, lock) } }) {
        lock.mutex.lock()
        acquired = true
    }
    return DownloadFileLease {
        lock.mutex.unlock()
        releaseDiskDownloadLock(key, lock)
    }
}

private suspend fun releaseDiskDownloadLock(key: String, lock: DiskDownloadLock) {
    diskLockRegistry.withLock {
        if (lock.release()) diskLocks.remove(key)
    }
}
