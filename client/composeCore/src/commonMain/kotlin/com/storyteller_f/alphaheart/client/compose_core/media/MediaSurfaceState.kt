/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

internal val LocalMediaPreviewAction = staticCompositionLocalOf<(() -> Unit)?> { null }

internal val LocalMediaDownloadEnabled = staticCompositionLocalOf { true }

internal val LocalMediaCopyEnabled = staticCompositionLocalOf { true }

internal val LocalMediaHeadingVisible = staticCompositionLocalOf { true }

/** Times are seconds; volume is normalized to 0..1. Unknown duration disables seeking. */
internal data class MediaControlState(
    val playing: Boolean = false,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val enabled: Boolean = true,
    val volumeEnabled: Boolean = true,
    val expansionEnabled: Boolean = true,
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val title: String? = null,
    val durationKnown: Boolean = true,
    val showTime: Boolean = false,
    val ended: Boolean = false,
)

internal data class MediaControlActions(
    val togglePlayback: () -> Unit,
    val seek: (Double) -> Unit,
    val setVolume: (Float) -> Unit,
    val toggleMute: () -> Unit,
    val retry: (() -> Unit)? = null,
)

internal fun formatMediaTime(seconds: Double): String {
    val total = if (seconds.isFinite()) seconds.toLong().coerceAtLeast(0) else 0
    val minutes = (total / 60 % 60).toString().padStart(2, '0')
    val remainder = (total % 60).toString().padStart(2, '0')
    return if (total >= 3600) "${total / 3600}:$minutes:$remainder" else "${total / 60}:$remainder"
}

/** Embedded video reserves its picture ratio independently of the heading and transport controls. */
sealed interface MediaSurfaceLayout {
    data object Bounded : MediaSurfaceLayout
    data class EmbeddedVideo(val aspectRatio: Float = 16f / 9f) : MediaSurfaceLayout
}
