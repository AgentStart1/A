/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import kotlinx.coroutines.Dispatchers

internal val desktopVideoCoordination = Dispatchers.Default.limitedParallelism(1)

internal fun DesktopVideoState.toControlState(): MediaControlState {
    val ready = this as? DesktopVideoState.Ready
    return MediaControlState(
        playing = ready?.playing == true,
        loading = this == DesktopVideoState.Loading || ready?.buffering == true,
        failed = this == DesktopVideoState.Failed,
        enabled = ready != null,
        position = ready?.position ?: 0.0,
        duration = ready?.duration ?: 0.0,
        volume = ready?.volume ?: 1f,
        muted = ready?.volume == 0f,
    )
}
