/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import com.storyteller_f.alphaheart.client.compose_core.media.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.media.streamRemoteResource
import com.storyteller_f.shared.model.FileInfo
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.ktor.client.HttpClient
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
    streamRemoteResource(client, ResourceDownloadRequest(file.url, file.name, file.size), openSink)
}
