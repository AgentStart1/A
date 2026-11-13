/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.yield
import kotlinx.io.Sink
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import no.synth.kmpzip.kotlinx.asInputStream
import no.synth.kmpzip.zip.ZipInputStream
import no.synth.kmpzip.zip.safeEntrySegments

private const val MAX_DISK_ARCHIVE_BYTES = 64L * 1024 * 1024
private const val MAX_DISK_EXPANDED_BYTES = 80L * 1024 * 1024
private const val MAX_DISK_ARCHIVE_ENTRIES = 128
private const val ARCHIVE_BUFFER_BYTES = 64 * 1024

internal suspend fun extractDiskArchive(path: String, directory: String, createSink: (Path) -> Sink) {
    val source = Path(path)
    require(SystemFileSystem.metadataOrNull(source)?.size in 0..MAX_DISK_ARCHIVE_BYTES) {
        "Archive exceeds the source size limit"
    }
    val names = mutableSetOf<String>()
    var remaining = MAX_DISK_EXPANDED_BYTES
    SystemFileSystem.source(source).buffered().use { input ->
        ZipInputStream(input.asInputStream()).use { archive ->
            while (true) {
                currentCoroutineContext().ensureActive()
                val entry = archive.nextEntry ?: break
                val segments = safeEntrySegments(entry.name)
                require(segments.isNotEmpty()) { "Empty archive path" }
                // Windows folds these suffixes when opening files, even on parent directories.
                require(segments.none { it.endsWith('.') || it.endsWith(' ') }) {
                    "Archive path has a trailing dot or space"
                }
                require(names.size < MAX_DISK_ARCHIVE_ENTRIES) { "Too many archive entries" }
                require(names.add(segments.joinToString("/").lowercase())) { "Duplicate archive paths" }
                val target = segments.fold(Path(directory)) { parent, name -> Path(parent, name) }
                if (entry.isDirectory) {
                    require(archive.read() == -1) { "Directory archive entries must be empty" }
                    SystemFileSystem.createDirectories(target)
                } else {
                    require(entry.size in -1..remaining) { "Archive exceeds the decoded size limit" }
                    target.parent?.let { SystemFileSystem.createDirectories(it) }
                    createSink(target).use { output ->
                        remaining -= archive.copyArchiveEntry(output, remaining)
                    }
                }
            }
        }
    }
}

private suspend fun ZipInputStream.copyArchiveEntry(output: Sink, limit: Long): Long {
    val buffer = ByteArray(ARCHIVE_BUFFER_BYTES)
    var written = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        // Read one excess byte to detect overflow before any excess bytes reach disk.
        val count = read(buffer, 0, minOf(buffer.size.toLong(), limit - written + 1).toInt())
        if (count == -1) return written
        check(count > 0) { "Archive extraction made no progress" }
        require(count <= limit - written) { "Archive exceeds the decoded size limit" }
        output.write(buffer, 0, count)
        written += count
        yield()
    }
}
