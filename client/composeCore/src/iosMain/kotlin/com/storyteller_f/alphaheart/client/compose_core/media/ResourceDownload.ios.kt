/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.sink
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

actual suspend fun saveRemoteResource(
    client: HttpClient,
    resource: ResourceDownloadRequest,
    dialogSettings: FileKitDialogSettings,
): ResourceSaveOutcome {
    val destination =
        withContext(Dispatchers.Main) {
            FileKit.openFileSaver(
                suggestedName = resource.fileName,
                defaultExtension = null,
                dialogSettings = dialogSettings,
            )
        } ?: return ResourceSaveOutcome.CANCELLED
    withContext(Dispatchers.IO) { streamRemoteResource(client, resource) { destination.sink() } }
    return ResourceSaveOutcome.SAVED
}
