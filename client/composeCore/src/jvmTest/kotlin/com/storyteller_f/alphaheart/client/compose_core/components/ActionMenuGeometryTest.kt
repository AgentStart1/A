/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class ActionMenuGeometryTest {
    @Test
    fun canonicalMenusKeepEqualTilesGlyphsGapsBackgroundsCountsAndAllActionsAtBothWidths() {
        for (compact in listOf(false, true)) {
            for (count in listOf(1, 2, 6, 8, 13)) {
                runComposeUiTest {
                    val clicked = mutableListOf<Int>()
                    val background = lightColorScheme().surfaceContainer
                    setContent {
                        CompositionLocalProvider(LocalDensity provides Density(1f)) {
                            MaterialTheme(colorScheme = lightColorScheme(background = Color.White)) {
                                ReadingInteractions {
                                    Box(Modifier.width(if (compact) 390.dp else 800.dp)) {
                                        ReadingActionMenuSurface({}, { Text("Identity") }, compact) {
                                            IconActionMenu(twoActions = count == 2) {
                                                repeat(count) { index ->
                                                    ButtonNav(
                                                        IconRes.Vector(Icons.Default.Add, "glyph$index"),
                                                        "Action$index",
                                                        suffix = { Text("$index") },
                                                    ) { clicked += index }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    onNodeWithTag("action-menu-surface").assertWidthIsEqualTo(320.dp)
                    val bounds =
                        (0 until count).map { index ->
                            val action = onNodeWithContentDescription("Action$index")
                            action.assertHeightIsEqualTo(52.dp)
                            onNodeWithContentDescription("glyph$index", useUnmergedTree = true)
                                .assertWidthIsEqualTo(21.dp).assertHeightIsEqualTo(21.dp)
                            val pixels = action.captureToImage().toPixelMap()
                            assertEquals(background, pixels[pixels.width / 2, pixels.height - 5])
                            onNodeWithText("$index", useUnmergedTree = true).assertExists()
                            action.performClick()
                            action.getUnclippedBoundsInRoot()
                        }
                    runOnIdle { assertEquals((0 until count).toList(), clicked) }
                    if (count == 2) {
                        val surface = onNodeWithTag("action-menu-surface").getUnclippedBoundsInRoot()
                        assertTwoActionBounds(bounds, surface)
                    } else {
                        bounds.forEachIndexed { index, rect ->
                            assertTrue(
                                abs((rect.right - rect.left).value - (bounds[0].right - bounds[0].left).value) <= 1,
                            )
                            if (index % 4 != 0) assertEquals(12.dp, rect.left - bounds[index - 1].right)
                            if (index >= 4) {
                                assertEquals(bounds[index - 4].left, rect.left)
                                assertEquals(12.dp, rect.top - bounds[index - 4].bottom)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun actionTilesShowTheHostAtRestAndSoftBackgroundOnlyWhileHovered() {
        for (dark in listOf(false, true)) {
            runComposeUiTest {
                setContent {
                    MaterialTheme(
                        colorScheme =
                        if (dark) {
                            darkColorScheme(background = Color.Black)
                        } else {
                            lightColorScheme(background = Color.White)
                        },
                    ) {
                        ReadingInteractions {
                            Box(Modifier.width(280.dp).background(if (dark) Color.Black else Color.White)) {
                                IconActionMenu {
                                    ButtonNav(actionIcon(ActionGlyph.PLUS), "Create") {}
                                }
                            }
                        }
                    }
                }
                val action = onNodeWithContentDescription("Create")
                val expected = if (dark) Color.Black else Color.White
                action.assertTileBackground(expected)
                action.performMouseInput { enter(center) }
                val hovered = if (dark) darkColorScheme().primaryContainer else lightColorScheme().primaryContainer
                action.assertTileBackground(hovered)
                action.performMouseInput { exit() }
                action.assertTileBackground(expected)
            }
        }
    }

    @Test
    fun longIdentityAndManyActionsScrollWithoutMovingOrHidingTheCloseControl() {
        runComposeUiTest {
            val clicked = mutableListOf<Int>()
            var closed = false
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MaterialTheme {
                        ReadingInteractions {
                            Box(Modifier.width(390.dp).height(280.dp)) {
                                ReadingActionMenuSurface(
                                    { closed = true },
                                    {
                                        Text(
                                            "A long identity name that wraps while keeping the close control reachable",
                                        )
                                    },
                                    compact = true,
                                ) {
                                    IconActionMenu {
                                        repeat(17) { index ->
                                            ButtonNav(Icons.Default.Add, "Action$index") { clicked += index }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            val close = onNodeWithTag("action-menu-close")
            val initial = close.getUnclippedBoundsInRoot()
            onNodeWithContentDescription("Action16").performScrollTo().assertIsDisplayed().performClick()
            assertEquals(initial, close.getUnclippedBoundsInRoot())
            close.assertIsDisplayed().performClick()
            runOnIdle {
                assertEquals(listOf(16), clicked)
                assertTrue(closed)
            }
        }
    }
}

private fun assertTwoActionBounds(bounds: List<DpRect>, surface: DpRect) {
    assertEquals(60.dp, bounds[0].right - bounds[0].left)
    assertEquals(60.dp, bounds[1].right - bounds[1].left)
    assertEquals(12.dp, bounds[1].left - bounds[0].right)
    assertEquals((surface.left + surface.right) / 2, (bounds[0].left + bounds[1].right) / 2)
}

private fun SemanticsNodeInteraction.assertTileBackground(expected: Color) {
    val pixels = captureToImage().toPixelMap()
    assertEquals(expected, pixels[pixels.width / 2, pixels.height - 5])
}
