/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt

data class NavRoute(val path: String, val icon: ImageVector, val label: String)

/** CSS gap plus space-around for both reading navigation bars. */
fun navigationSpacing(spacing: Dp): Arrangement.Horizontal = NavigationSpacing(spacing)

/** CSS gap plus space-around, including non-shrinking intrinsic items. */
internal data class NavigationSpacing(override val spacing: Dp) : Arrangement.Horizontal {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        layoutDirection: LayoutDirection,
        outPositions: IntArray,
    ) {
        if (sizes.isEmpty()) return
        val gap = spacing.roundToPx()
        val around = (totalSize - sizes.sum() - gap * (sizes.size - 1)).coerceAtLeast(0).toFloat() / sizes.size
        var position = around / 2
        sizes.forEachIndexed { index, size ->
            val start = position.roundToInt()
            outPositions[index] = if (layoutDirection == LayoutDirection.Ltr) start else totalSize - start - size
            position += size + gap + around
        }
    }
}
