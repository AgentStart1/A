/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.VectorPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

class ActionGlyphTest {
    @Test
    fun approvedActionRolesUseTheExactUnfilledRoundStrokeContract() {
        for (glyph in ActionGlyph.entries) {
            val icon = actionIcon(glyph)
            assertEquals(24f, icon.viewportWidth)
            assertEquals(24f, icon.viewportHeight)
            for (index in 0 until icon.root.size) {
                val path = assertIs<VectorPath>(icon.root[index])
                // Lucide KeyRound includes its canonical filled pinhole; the surrounding key stays unfilled.
                val fill = if (glyph == ActionGlyph.KEY_ROUND && index == 1) Color.Black else Color.Transparent
                assertEquals(SolidColor(fill), path.fill, "${glyph.name} path $index")
                assertNotNull(path.stroke)
                assertEquals(2f, path.strokeLineWidth)
                assertEquals(StrokeCap.Round, path.strokeLineCap)
                assertEquals(StrokeJoin.Round, path.strokeLineJoin)
            }
        }
    }

    @Test
    fun favoriteAndSubscriptionSelectedStatesKeepTheCanonicalPathsAndRemainVisuallyDistinct() {
        for (glyph in listOf(ActionGlyph.HEART, ActionGlyph.BELL)) {
            val normal = actionIcon(glyph)
            val selected = actionIcon(glyph, selected = true)
            assertNotSame(normal, selected)
            assertEquals(normal.root.size, selected.root.size)
            for (index in 0 until normal.root.size) {
                val outline = assertIs<VectorPath>(normal.root[index])
                val filled = assertIs<VectorPath>(selected.root[index])
                assertEquals(outline.pathData, filled.pathData)
                assertEquals(SolidColor(Color.Transparent), outline.fill)
                assertNotNull(filled.fill)
                assertEquals(outline.strokeLineWidth, filled.strokeLineWidth)
            }
        }
    }
}
