/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray

internal suspend fun FileEngine.extractDownloadArchive(path: String, extract: suspend (String) -> Unit) {
    // The download record has left PROCESSED, and the caller holds the file lock.
    // A crash leaves an incomplete directory that must be removed before reprocessing.
    val directory = "$path.extracted"
    var completed = false
    withDownloadCleanup({ withContext(NonCancellable) { if (!completed) delete(directory) } }) {
        delete(directory)
        extract(directory)
        currentCoroutineContext().ensureActive()
        completed = true
    }
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
