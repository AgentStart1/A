/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.storyteller_f.alphaheart.client.compose_core.error.UiOperationError
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.SimpleLoadingHandler
import com.storyteller_f.result.captureFailure

object DefaultMediaPlayListHandlerProvider : MediaPlayListHandlerProvider {
    @Composable
    override fun playListHandler(remoteMediaItem: RemoteMediaItem): LoadingHandler<List<ConstPlayItem>> {
        val scope = rememberCoroutineScope()
        return remember(remoteMediaItem) {
            SimpleLoadingHandler(scope) {
                captureFailure(UiOperationError::from) {
                    loadMediaPlayList(remoteMediaItem)
                }
            }
        }
    }
}

private fun loadMediaPlayList(remoteMediaItem: RemoteMediaItem): List<ConstPlayItem> =
    listOf(ConstPlayItem(remoteMediaItem.url, title = remoteMediaItem.title ?: remoteMediaItem.name))
