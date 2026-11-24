/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*

interface VideoListener {
    fun onPlayStateChange(isPlaying: Boolean)
    fun onUpdateSize(size: CustomVideoSize)
    fun onUpdateLoading(isLoading: Boolean)
    fun onMediaItemChanged(mediaId: String?, currentMediaItemIndex: Int)
}
