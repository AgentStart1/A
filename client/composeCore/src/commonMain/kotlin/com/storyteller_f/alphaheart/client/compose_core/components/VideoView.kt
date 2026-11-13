/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
expect fun VideoViewEmbed(remoteMediaItem: RemoteMediaItem)

@Composable
expect fun VideoViewFilled(remoteMediaItem: RemoteMediaItem)

@Composable
expect fun VideoViewFullScreen(remoteMediaItem: RemoteMediaItem)

@Composable
expect fun rememberIsInPipMode(): Boolean

interface VideoListener {
    fun onPlayStateChange(isPlaying: Boolean)
    fun onUpdateSize(size: CustomVideoSize)
    fun onUpdateLoading(isLoading: Boolean)
    fun onMediaItemChanged(mediaId: String?, currentMediaItemIndex: Int)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlaylistPicker(
    showSheet: Boolean,
    sheetState: SheetState,
    hideSheet: () -> Unit,
    playList: List<PlayItem>,
    onClick: (PlayItem, Int) -> Unit,
) {
    BaseSheet(showSheet, sheetState, hideSheet) {
        LazyColumn(
            modifier = Modifier.height(300.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(playList.size, key = {
                playList[it].id
            }) { index ->
                Row(
                    modifier = Modifier.height(40.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val icon = playList[index].icon
                    if (!icon.isNullOrBlank()) {
                        AsyncImage(icon, "icon", modifier = Modifier.height(30.dp))
                    } else {
                        Box(modifier = Modifier.height(30.dp))
                    }
                    Text(
                        playList[index].title ?: "unknown",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    IconButton({
                        onClick(playList[index], index)
                    }) {
                        Icon(Icons.AutoMirrored.Default.PlaylistPlay, "play")
                    }
                }
            }
        }
    }
}
