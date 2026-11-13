/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import com.storyteller_f.shared.model.FileInfo
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun saveUploadedFile(
    client: HttpClient,
    file: FileInfo,
    dialogSettings: FileKitDialogSettings,
): FileSaveOutcome {
    val destination =
        withContext(Dispatchers.Main) {
            FileKit.openFileSaver(suggestedName = file.name, defaultExtension = null, dialogSettings = dialogSettings)
        } ?: return FileSaveOutcome.CANCELLED
    withContext(Dispatchers.IO) { streamUploadedFile(client, file) { destination.sink() } }
    return FileSaveOutcome.SAVED
}
