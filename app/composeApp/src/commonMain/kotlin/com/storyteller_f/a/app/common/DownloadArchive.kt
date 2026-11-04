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
import kotlinx.io.readByteArray
import kotlin.uuid.Uuid

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
