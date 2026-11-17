/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class PillAccessibilityTest {
    @Test
    fun `emoji reaction labels remain distinct and update without requiring geometry changes`() =
        runComposeUiTest {
            val count = mutableStateOf(1)
            var smileClicks = 0
            var heartClicks = 0
            setContent {
                MaterialTheme {
                    Row {
                        Pill(text = "${count.value} ", emoji = "😀") { smileClicks++ }
                        Pill(text = "1 ", emoji = "❤️", selected = true) { heartClicks++ }
                    }
                }
            }
            val before = onNodeWithContentDescription("😀 1").fetchSemanticsNode().boundsInRoot
            onNodeWithContentDescription("😀 1").assertIsNotSelected().assertHasClickAction().performClick()
            onNodeWithContentDescription("❤️ 1").assertIsSelected().assertHasClickAction().performClick()
            assertEquals(1, smileClicks)
            assertEquals(1, heartClicks)
            runOnIdle { count.value = 2 }
            onNodeWithContentDescription("😀 1").assertDoesNotExist()
            val after = onNodeWithContentDescription("😀 2").fetchSemanticsNode().boundsInRoot
            assertEquals(before, after)
            onNodeWithContentDescription("😀 2").assertIsNotSelected().performClick()
            onNodeWithContentDescription("❤️ 1").assertIsSelected()
            assertEquals(2, smileClicks)
            assertEquals(1, heartClicks)
        }
}
