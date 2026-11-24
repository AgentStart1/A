/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider

enum class ReadingOverlaySize(internal val width: Dp) {
    COMPACT(248.dp),
    MENU(272.dp),
    REACTIONS(324.dp),
    LIST(320.dp),
    FORM(360.dp),
}

internal class OverlayHostState {
    val layers = mutableStateListOf<@Composable () -> Unit>()
}

internal class OverlayTrigger {
    private var bounds: IntRect? = null

    fun record(newBounds: IntRect) {
        bounds = newBounds
    }

    fun takeBounds(): IntRect? = bounds.also { bounds = null }
}

internal class OverlayPointerObserver {
    var coordinates: LayoutCoordinates? = null
}

internal class OverlayPosition(
    private val trigger: IntRect?,
    private val margin: Int,
    private val gap: Int,
    private val centerOnTrigger: Boolean,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        val anchor = trigger ?: anchorBounds.takeIf { it.width in 1..margin * 6 && it.height in 1..margin * 6 }
        val left =
            when {
                anchor == null -> if (layoutDirection == LayoutDirection.Ltr) maxX else margin
                centerOnTrigger -> anchor.center.x - popupContentSize.width / 2
                layoutDirection == LayoutDirection.Ltr -> anchor.right - popupContentSize.width
                else -> anchor.left
            }
        val below = anchor?.bottom?.plus(gap) ?: margin
        val top = if (below <= maxY || anchor == null) below else anchor.top - popupContentSize.height - gap
        return IntOffset(left.coerceIn(margin, maxX), top.coerceIn(margin, maxY))
    }
}
