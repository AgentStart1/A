/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.media

import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

internal actual fun Modifier.excludeCropSystemGestures(exclusion: (LayoutCoordinates) -> Rect): Modifier =
    systemGestureExclusion(exclusion)
