/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.fonts.FontLoadStatus
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class FontStatusGeometryTest {
    @Test
    fun loadingReadyFailureAndRetryKeepTheFilenameAndFollowingActionInPlace() {
        for (width in listOf(278, 282)) {
            runComposeUiTest {
                var state by mutableStateOf(FontLoadStatus.LOADING)
                var retries = 0
                setContent {
                    MaterialTheme {
                        ReadingInteractions {
                            Column(Modifier.width(width.dp)) {
                                FontStatusRow("a-long-community-content-font-filename.otf", state) {
                                    retries++
                                    state = FontLoadStatus.LOADING
                                }
                                Text("Following menu action", Modifier.testTag("following-action"))
                            }
                        }
                    }
                }
                val slot = onNodeWithTag("font-status-slot")
                slot.assertWidthIsEqualTo(44.dp).assertHeightIsEqualTo(44.dp)
                onNodeWithTag("font-type-icon").assertWidthIsEqualTo(17.dp).assertHeightIsEqualTo(17.dp)
                onNodeWithTag("font-state-icon").assertWidthIsEqualTo(17.dp).assertHeightIsEqualTo(17.dp)
                val originalSlot = slot.getUnclippedBoundsInRoot()
                val originalName =
                    onNodeWithText(
                        "a-long-community-content-font-filename.otf",
                    ).getUnclippedBoundsInRoot()
                val originalFollowing = onNodeWithTag("following-action").getUnclippedBoundsInRoot()
                for (next in listOf(FontLoadStatus.READY, FontLoadStatus.FAILED)) {
                    runOnIdle { state = next }
                    onNodeWithTag("font-state-icon", useUnmergedTree = true)
                        .assertWidthIsEqualTo(17.dp).assertHeightIsEqualTo(17.dp)
                    assertEquals(originalSlot, slot.getUnclippedBoundsInRoot())
                    assertEquals(
                        originalName,
                        onNodeWithText("a-long-community-content-font-filename.otf").getUnclippedBoundsInRoot(),
                    )
                    assertEquals(originalFollowing, onNodeWithTag("following-action").getUnclippedBoundsInRoot())
                }
                onNodeWithTag(
                    "font-status-retry",
                ).assertWidthIsEqualTo(44.dp).assertHeightIsEqualTo(44.dp).performClick()
                runOnIdle { assertEquals(1, retries) }
                onNodeWithTag("font-status-retry").assertDoesNotExist()
                assertEquals(originalSlot, slot.getUnclippedBoundsInRoot())
                assertEquals(originalFollowing, onNodeWithTag("following-action").getUnclippedBoundsInRoot())
            }
        }
    }
}
