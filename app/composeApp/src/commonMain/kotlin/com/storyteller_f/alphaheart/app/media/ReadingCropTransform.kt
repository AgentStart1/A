/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.media

import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.attafitamim.krop.core.crop.CropState
import com.attafitamim.krop.core.crop.getTransformedImageRect
import com.attafitamim.krop.core.crop.rotLeft
import com.attafitamim.krop.core.crop.rotRight
import com.attafitamim.krop.core.utils.coerceAtMost
import com.attafitamim.krop.core.utils.constrainOffset

/** Rotation changes image orientation, while a locked crop keeps its required output orientation. */
internal fun CropState.rotateReadingCrop(clockwise: Boolean) {
    val previousSize = region.size
    if (clockwise) rotRight() else rotLeft()
    if (aspectLock) {
        val bounds = getTransformedImageRect(transform, src.size)
        val size = previousSize.coerceAtMost(bounds.size)
        val aligned = Rect(region.center - Offset(size.width / 2, size.height / 2), size).constrainOffset(bounds)
        aspectLock = false
        try {
            region = aligned
        } finally {
            aspectLock = true
        }
    }
}
