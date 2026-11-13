/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.serviceCatching
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.FileInfo
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readBuffer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.io.readByteArray

internal suspend fun transferDownload(
    session: UserSessionManager,
    files: FileEngine,
    path: String,
    file: FileInfo,
    progress: suspend (Long) -> Unit,
) {
    val offset = files.size(path) ?: 0
    session.serviceCatching {
        prepareGet(file.url) {
            if (offset > 0 && offset < file.size) header(HttpHeaders.Range, "bytes=$offset-")
        }.execute { response ->
            val partial = response.status == HttpStatusCode.PartialContent
            check(response.status == HttpStatusCode.OK || partial) { "Download failed: ${response.status.value}" }
            var received = if (partial) offset else 0
            val channel: ByteReadChannel = response.body()
            files.withWriter(path, append = partial) { writer ->
                while (!channel.isClosedForRead) {
                    currentCoroutineContext().ensureActive()
                    val bytes = channel.readBuffer(64 * 1024L).readByteArray()
                    if (bytes.isEmpty()) continue
                    received += bytes.size
                    check(received <= file.size) { "Downloaded file exceeds its metadata size" }
                    writer.write(bytes)
                    progress(received)
                }
            }
            check(received == file.size) { "Downloaded file size mismatch" }
        }
    }.getOrThrow()
}
