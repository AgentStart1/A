/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import com.storyteller_f.shared.model.FileInfo
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.download
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode

internal actual suspend fun saveUploadedFile(
    client: HttpClient,
    file: FileInfo,
    dialogSettings: FileKitDialogSettings,
): FileSaveOutcome {
    val response = client.get(file.url)
    check(response.status == HttpStatusCode.OK) { "Download failed: ${response.status}" }
    val bytes = response.body<ByteArray>()
    check(bytes.size.toLong() == file.size) { "Downloaded file size does not match" }
    FileKit.download(bytes = bytes, fileName = file.name)
    return FileSaveOutcome.STARTED
}
