/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.storyteller_f.alphaheart.client.compose_core.utils.OverlayPosition
import kotlin.test.Test
import kotlin.test.assertEquals

internal class OverlayPositionTest {
    private val window = IntSize(1280, 800)

    @Test
    fun menuCentersBelowTheTrigger() {
        val anchor = IntRect(400, 60, 440, 100)
        val position = OverlayPosition(anchor, margin = 12, gap = 8, centerOnTrigger = true)
        assertEquals(
            IntOffset(284, 108),
            position.calculatePosition(IntRect.Zero, window, LayoutDirection.Ltr, IntSize(272, 200)),
        )
    }

    @Test
    fun listFlipsAboveAndStaysInsideTheRightEdge() {
        val anchor = IntRect(1244, 750, 1280, 790)
        val position = OverlayPosition(anchor, margin = 12, gap = 8, centerOnTrigger = false)
        assertEquals(
            IntOffset(948, 442),
            position.calculatePosition(IntRect.Zero, window, LayoutDirection.Ltr, IntSize(320, 300)),
        )
    }
}
