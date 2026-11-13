/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import com.storyteller_f.shared.model.FileInfo
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.readBuffer
import kotlinx.io.RawSink

internal enum class FileSaveOutcome { SAVED, STARTED, CANCELLED }

/** Saves an uploaded file through the platform's save dialog or browser download manager. */
internal expect suspend fun saveUploadedFile(
    client: HttpClient,
    file: FileInfo,
    dialogSettings: FileKitDialogSettings,
): FileSaveOutcome

/** Streams once into the selected destination; failures and cancellation never replay the request. */
internal suspend fun streamUploadedFile(client: HttpClient, file: FileInfo, openSink: () -> RawSink) {
    client.prepareGet(file.url).execute { response ->
        check(response.status == HttpStatusCode.OK) { "Download failed: ${response.status}" }
        val channel = response.body<ByteReadChannel>()
        openSink().use { sink ->
            var received = 0L
            while (!channel.isClosedForRead) {
                val chunk = channel.readBuffer(64 * 1024L)
                received += chunk.remaining
                chunk.transferTo(sink)
            }
            check(received == file.size) { "Downloaded file size does not match" }
        }
    }
}
