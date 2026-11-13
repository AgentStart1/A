/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable

@Composable
expect fun AudioViewEmbed(remoteMediaItem: RemoteMediaItem)

@Composable
expect fun AudioViewFilled(remoteMediaItem: RemoteMediaItem)

@Composable
expect fun AudioViewFullScreen(remoteMediaItem: RemoteMediaItem)
