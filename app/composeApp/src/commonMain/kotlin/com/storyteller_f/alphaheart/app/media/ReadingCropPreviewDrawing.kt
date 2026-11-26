/*
 * This is a private project. All rights reserved.
 */

// Adapted from Krop 0.2.0 CropperPreview by Tamim Attafi (Apache-2.0).
// Modifications: app-local names and platform gesture exclusion. See KROP-LICENSE.txt.
package com.storyteller_f.alphaheart.app.media

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.attafitamim.krop.core.crop.CropperStyle
import com.attafitamim.krop.core.images.DecodeResult

internal fun DrawScope.drawReadingCropPreview(
    image: DecodeResult?,
    matrix: Matrix,
    style: CropperStyle,
    cropRect: Rect,
    cropPath: Path,
) {
    if (image == null) return
    val (params, bitmap) = image
    withTransform({ transform(matrix) }) {
        drawImage(bitmap, dstOffset = params.subset.topLeft, dstSize = params.subset.size)
    }
    with(style) {
        clipPath(cropPath, ClipOp.Difference) { drawRect(color = overlayColor) }
        drawCropRect(cropRect)
    }
}
