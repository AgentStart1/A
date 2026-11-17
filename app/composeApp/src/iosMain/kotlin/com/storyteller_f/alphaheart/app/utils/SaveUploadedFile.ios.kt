/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import com.storyteller_f.alphaheart.client.compose_core.components.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.components.ResourceSaveOutcome
import com.storyteller_f.alphaheart.client.compose_core.components.saveRemoteResource
import com.storyteller_f.shared.model.FileInfo
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.ktor.client.HttpClient

internal actual suspend fun saveUploadedFile(
    client: HttpClient,
    file: FileInfo,
    dialogSettings: FileKitDialogSettings,
): FileSaveOutcome =
    when (saveRemoteResource(client, ResourceDownloadRequest(file.url, file.name, file.size), dialogSettings)) {
        ResourceSaveOutcome.SAVED -> FileSaveOutcome.SAVED
        ResourceSaveOutcome.HANDED_OFF -> FileSaveOutcome.STARTED
        ResourceSaveOutcome.CANCELLED -> FileSaveOutcome.CANCELLED
    }
