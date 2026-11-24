/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.alphaheart.panel.common.PanelAllUsersScreen
import com.storyteller_f.alphaheart.panel.common.PanelUserDetailScreen
import com.storyteller_f.alphaheart.panel.navigation.selectedPanelEntityId
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.ObjectType
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class PanelDirectorySelectionRenderTest {
    @Test
    fun changingSelectionKeepsRowsMountedAndGeometryStableAtBothWidths() =
        runComposeUiTest {
            val width = mutableStateOf(360.dp)
            val route = mutableStateOf<NavKey>(PanelUserDetailScreen(1))
            var selectedColor = Color.Unspecified
            var mounts = 0
            var disposals = 0
            var clicks = 0
            setContent {
                MaterialTheme {
                    ReadingInteractions {
                        val color = MaterialTheme.colorScheme.primaryContainer
                        SideEffect { selectedColor = color }
                        DisposableEffect(Unit) {
                            mounts++
                            onDispose { disposals++ }
                        }
                        val selectedId = selectedPanelEntityId(route.value, ObjectType.USER)
                        Column(Modifier.width(width.value)) {
                            DirectorySelectionRows(selectedId) { id ->
                                clicks++
                                route.value = PanelUserDetailScreen(id)
                            }
                        }
                    }
                }
            }
            for (viewportWidth in listOf(360.dp, 800.dp)) {
                runOnIdle {
                    width.value = viewportWidth
                    route.value = PanelUserDetailScreen(1)
                }
                val firstBounds = onNodeWithText("First user").fetchSemanticsNode().boundsInRoot
                val secondBounds = onNodeWithText("Second user").fetchSemanticsNode().boundsInRoot
                onNodeWithText("First user").assertIsSelected()
                onNodeWithText("Second user").assertIsNotSelected()
                assertInteriorColor(onNodeWithText("First user"), selectedColor)
                assertInteriorColor(onNodeWithTag("material"), selectedColor)
                onNodeWithText("Second user").performClick()
                onNodeWithText("First user").assertIsNotSelected()
                onNodeWithText("Second user").assertIsSelected()
                assertInteriorColor(onNodeWithText("Second user"), selectedColor)
                assertEquals(firstBounds, onNodeWithText("First user").fetchSemanticsNode().boundsInRoot)
                assertEquals(secondBounds, onNodeWithText("Second user").fetchSemanticsNode().boundsInRoot)
                runOnIdle { route.value = PanelAllUsersScreen }
                onNodeWithText("First user").assertIsNotSelected()
                onNodeWithText("Second user").assertIsNotSelected()
            }
            runOnIdle {
                assertEquals(2, clicks)
                assertEquals(1, mounts)
                assertEquals(0, disposals)
            }
        }
}

@OptIn(ExperimentalTestApi::class)
private fun assertInteriorColor(node: SemanticsNodeInteraction, expected: Color) {
    val pixels = node.captureToImage().toPixelMap()
    assertEquals(expected.toArgb(), pixels[pixels.width - 10, pixels.height / 2].toArgb())
}

@Composable
private fun DirectorySelectionRows(selectedId: Long?, onSelect: (Long) -> Unit) {
    for ((id, name) in listOf(1L to "First user", 2L to "Second user")) {
        UserCell(UserInfo.EMPTY.copy(id = id, nickname = name), selected = id == selectedId) {
            onSelect(id)
        }
    }
    ListItem(
        headlineContent = { Text("Material row") },
        colors = panelDirectoryRowColors(selectedId == 1L),
        modifier = Modifier.fillMaxWidth().panelDirectorySelection(selectedId == 1L).testTag("material"),
    )
}
