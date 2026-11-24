/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.storyteller_f.alphaheart.client.compose_core.media.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.media.saveRemoteResource
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.utils.LocalFileDialogSettings
import com.storyteller_f.result.CustomResult
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** Supplies a real platform save operation using the existing operation-failure dialog. */
@Composable
fun <C> rememberResourceDownloadAction(
    controller: CustomGlobalDialogController<C>,
    client: HttpClient,
): (ResourceDownloadRequest) -> Unit {
    val settings = LocalFileDialogSettings.current
    return remember(controller, client, settings) {
        { resource ->
            controller.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                controller.useResult {
                    CustomResult.success(saveRemoteResource(client, resource, settings))
                }
            }
            Unit
        }
    }
}
