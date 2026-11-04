/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.common

import com.storyteller_f.shared.commonJson
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import no.synth.kmpzip.kotlinx.asSource
import no.synth.kmpzip.zip.ZipFile
import kotlin.uuid.Uuid

internal const val MAX_BROWSER_ARCHIVE_BYTES = 64L * 1024 * 1024
private const val MAX_BROWSER_ARCHIVE_ENTRIES = 2048

/** Browser ZIP decoding is bounded; ordinary non-archive downloads can be streamed without this limit. */
internal suspend fun DownloadFileStore.extractBrowserArchive(path: String) {
    val bytes = read(path, MAX_BROWSER_ARCHIVE_BYTES)
    publishArchive(path) { directory ->
        ZipFile(bytes).use { archive ->
            val entries = archive.entries.filterNot { it.isDirectory }
            require(entries.size <= MAX_BROWSER_ARCHIVE_ENTRIES) { "Too many archive entries" }
            val names = entries.map { safeArchiveName(it.name) }
            require(names.map { it.lowercase() }.distinct().size == names.size) { "Duplicate archive paths" }
            var remaining = MAX_BROWSER_ARCHIVE_BYTES
            entries.zip(names).map { (entry, name) ->
                require(entry.size in 0..remaining) { "Archive exceeds the decoded size limit" }
                val content =
                    archive.getInputStream(
                        entry,
                    ).asSource().buffered().use { it.readDownloadBytes(remaining) }
                check(content.size.toLong() == entry.size) { "Archive entry size mismatch" }
                remaining -= content.size
                val target = "$directory/$name"
                write(target, content)
                DownloadFileEntry(target, content.size.toLong())
            }
        }
    }
}

internal fun safeArchiveName(name: String): String {
    val parts = name.replace('\\', '/').split('/')
    require(parts.all { it.isNotEmpty() && it != "." && it != ".." && ':' !in it && '\u0000' !in it }) {
        "Unsafe archive path"
    }
    return parts.joinToString("/")
}

internal suspend fun DownloadFileStore.publishArchive(
    path: String,
    extract: suspend (String) -> List<DownloadFileEntry>,
) {
    val old = readArchiveIndex(path)
    val directory = "$path.extract-${Uuid.random().toHexString()}"
    var published = false
    withDownloadCleanup({ withContext(NonCancellable) { if (!published) delete(directory) } }) {
        val entries = extract(directory)
        currentCoroutineContext().ensureActive()
        withContext(NonCancellable) {
            write(
                "$path.extracted.json",
                commonJson.encodeToString(DownloadArchiveIndex(directory, entries)).encodeToByteArray(),
            )
            published = true
        }
    }
    old?.directory?.takeIf { it != directory }?.let { delete(it) }
}

internal suspend fun Source.readDownloadBytes(limit: Long): ByteArray {
    require(limit in 0..Int.MAX_VALUE.toLong()) { "Unsupported read size" }
    val output = Buffer()
    while (!exhausted()) {
        currentCoroutineContext().ensureActive()
        val count = readAtMostTo(output, minOf(64 * 1024L, limit - output.size + 1))
        check(count > 0) { "File read made no progress" }
        require(output.size <= limit) { "File exceeds the size limit" }
        yield()
    }
    return output.readByteArray()
}
