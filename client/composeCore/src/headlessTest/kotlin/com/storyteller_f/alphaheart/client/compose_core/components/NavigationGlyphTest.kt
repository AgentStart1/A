/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.compactNavigationIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class NavigationGlyphTest {
    @Test
    fun approvedNavigationRolesUseTheReleasedUnfilledRoundStrokeGeometry() {
        for (glyph in NavigationGlyph.entries) {
            val icon = navigationIcon(glyph)
            assertEquals(24f, icon.viewportWidth)
            assertEquals(24f, icon.viewportHeight)
            assertEquals(24.dp, icon.defaultWidth)
            assertEquals(24.dp, icon.defaultHeight)
            assertTrue(icon.root.size > 0)
            for (index in 0 until icon.root.size) {
                val path = assertIs<VectorPath>(icon.root[index])
                assertEquals(SolidColor(Color.Transparent), path.fill)
                assertNotNull(path.stroke)
                assertEquals(2f, path.strokeLineWidth)
                assertEquals(StrokeCap.Round, path.strokeLineCap)
                assertEquals(StrokeJoin.Round, path.strokeLineJoin)
            }
        }
    }

    @Test
    fun navigationRolesRemainDistinctEvenWhenTheirRoutePathIsTheSame() {
        val global = navigationIcon(NavigationGlyph.TEXT)
        val child = navigationIcon(NavigationGlyph.MESSAGES_SQUARE)
        val info = navigationIcon(NavigationGlyph.INFO)
        assertEquals("text-align-start", global.name)
        assertEquals("messages-square", child.name)
        assertEquals("info", info.name)
        assertSame(global, compactNavigationIcon(NavRoute("/topics", global, "Topics")))
        assertSame(child, compactNavigationIcon(NavRoute("/topics", child, "Child topics")))
        assertSame(info, compactNavigationIcon(NavRoute("/info", info, "Information")))
    }
}
