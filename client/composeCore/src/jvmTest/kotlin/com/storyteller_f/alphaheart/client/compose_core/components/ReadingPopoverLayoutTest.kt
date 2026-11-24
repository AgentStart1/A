/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import kotlin.test.Test
import kotlin.test.assertEquals

private val LocalTestLabel = staticCompositionLocalOf { "Host" }

@OptIn(ExperimentalTestApi::class)
internal class ReadingPopoverLayoutTest {
    @Test
    fun openingPopoverLeavesToolbarBoundsUnchanged() =
        runComposeUiTest {
            var open by mutableStateOf(false)
            val wideWindow =
                object : WindowInfo {
                    override val isWindowFocused = true
                    override val containerSize = IntSize(1200, 800)
                }
            setContent {
                CompositionLocalProvider(LocalWindowInfo provides wideWindow) {
                    MaterialTheme {
                        ReadingOverlayHost {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(100.dp, 40.dp).testTag("search"))
                                ReadingButton({ open = true }, Modifier.testTag("trigger")) { Text("Open") }
                                if (open) {
                                    CompositionLocalProvider(LocalTestLabel provides "Popover") {
                                        ReadingAdaptiveOverlay({ open = false }, ReadingOverlaySize.MENU) {
                                            Text(LocalTestLabel.current)
                                        }
                                    }
                                }
                                Box(Modifier.size(80.dp, 40.dp).testTag("trailing"))
                            }
                        }
                    }
                }
            }

            val search = onNodeWithTag("search")
            val trigger = onNodeWithTag("trigger")
            val trailing = onNodeWithTag("trailing")
            val before = listOf(search, trigger, trailing).map { it.getUnclippedBoundsInRoot() }
            trigger.performClick()
            onNodeWithText("Popover").assertIsDisplayed()
            assertEquals(before, listOf(search, trigger, trailing).map { it.getUnclippedBoundsInRoot() })
        }
}
