/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.components.CustomBottomNav
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class CompactNavigationUiTest {
    private val originalLocale = Locale.getDefault()

    @BeforeTest
    fun useEnglishLocale() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `compact navigation matches the intrinsic stacked design in every selected state`() =
        runComposeUiTest {
            val routes =
                listOf("World", "Group", "Rooms").map {
                    NavRoute("/$it", navigationIcon(NavigationGlyph.EARTH), it)
                }
            val path = mutableStateOf(routes.first().path)
            setContent {
                MaterialTheme {
                    Box(Modifier.width(390.dp)) { CustomBottomNav(path.value, routes, navigate = { path.value = it }) }
                }
            }
            for (route in routes) {
                onNodeWithContentDescription(route.label).performClick().assertIsSelected()
                for (item in routes) {
                    val button = onNodeWithContentDescription(item.label)
                    val bounds = button.getUnclippedBoundsInRoot()
                    val icon =
                        onNodeWithTag("bottom-nav-icon-${item.path}", useUnmergedTree = true)
                            .getUnclippedBoundsInRoot()
                    val text =
                        onNode(
                            hasText(item.label) and !hasClickAction(),
                            useUnmergedTree = true,
                        ).getUnclippedBoundsInRoot()
                    assertEquals(17.dp, icon.right - icon.left)
                    assertEquals(17.dp, icon.bottom - icon.top)
                    assertEquals(7.dp, icon.top - bounds.top)
                    assertEquals(7.dp, text.top - icon.bottom)
                    assertEquals(7.dp, bounds.bottom - text.bottom)
                    assertEquals(24.dp, bounds.right - bounds.left - (text.right - text.left))
                    assertTrue(bounds.right - bounds.left < 100.dp)
                    val image = button.captureToImage().toPixelMap()
                    val background = image[image.width / 2, image.height - 3].toArgb()
                    assertEquals(if (item == route) 0xFFE4EEE3.toInt() else 0xFFFFFFFF.toInt(), background)
                }
                val bar = onNodeWithTag("reading-bottom-nav").getUnclippedBoundsInRoot()
                val item = onNodeWithContentDescription(route.label).getUnclippedBoundsInRoot()
                assertEquals(17.dp, bar.bottom - bar.top - (item.bottom - item.top))
            }
        }

    @Test
    fun `unread count floats above the room icon without resizing or hiding its label`() =
        runComposeUiTest {
            val routes =
                listOf(
                    NavRoute("/world", navigationIcon(NavigationGlyph.EARTH), "World"),
                    NavRoute("/rooms", navigationIcon(NavigationGlyph.EARTH), "Rooms"),
                )
            val unread = mutableStateOf(0L)
            setContent {
                MaterialTheme {
                    Box(Modifier.width(390.dp)) {
                        CustomBottomNav(
                            "/world",
                            routes,
                            unreadRoomsBadge = unread.value > 0,
                            unreadMessageCount = unread.value,
                        )
                    }
                }
            }
            val bar = onNodeWithTag("reading-bottom-nav").getUnclippedBoundsInRoot()
            val room = onNodeWithTag("Rooms").getUnclippedBoundsInRoot()
            for (count in listOf(3L, 999L, 0L)) {
                runOnIdle { unread.value = count }
                assertEquals(bar, onNodeWithTag("reading-bottom-nav").getUnclippedBoundsInRoot())
                assertEquals(room, onNodeWithTag("Rooms").getUnclippedBoundsInRoot())
                if (count > 0) {
                    onNodeWithContentDescription("Rooms, Unread messages, $count").assertExists()
                    val icon =
                        onNodeWithTag("bottom-nav-icon-/rooms", useUnmergedTree = true).getUnclippedBoundsInRoot()
                    val badge = onNodeWithTag("bottom-nav-unread", useUnmergedTree = true).getUnclippedBoundsInRoot()
                    val label =
                        onNode(
                            hasText("Rooms") and !hasClickAction(),
                            useUnmergedTree = true,
                        ).getUnclippedBoundsInRoot()
                    assertTrue(badge.top < icon.top)
                    assertTrue(badge.right > icon.right)
                    assertTrue(badge.bottom <= label.top)
                    assertTrue(badge.left >= room.left && badge.right <= room.right)
                } else {
                    onNodeWithTag("bottom-nav-unread", useUnmergedTree = true).assertDoesNotExist()
                    onNodeWithContentDescription("Rooms").assertExists()
                }
            }
        }
}
