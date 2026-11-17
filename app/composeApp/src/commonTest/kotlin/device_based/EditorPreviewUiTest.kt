/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.AdaptiveEditorLayout
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class EditorPreviewUiTest {
    @Test
    fun previewOnlyKeepsOnePaneAndRetainsDraftAcrossWidths() =
        runAppComposeUiTest {
            val width = mutableStateOf(390.dp)
            val previewOnly = mutableStateOf(false)
            val draft = mutableStateOf("")
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 500.dp)) {
                        AdaptiveEditorLayout(
                            preview = { Text("Preview: ${draft.value}") },
                            previewOnly = previewOnly.value,
                        ) {
                            BasicTextField(draft.value, { draft.value = it }, Modifier.testTag("editor-draft"))
                        }
                    }
                }
            }
            onNodeWithTag("editor-draft").performTextReplacement("Retained draft")
            runOnIdle { previewOnly.value = true }
            onNodeWithTag("editor-draft").assertDoesNotExist()
            onNodeWithText("Preview: Retained draft").assertIsDisplayed()
            runOnIdle { width.value = 1200.dp }
            onNodeWithTag("editor-draft").assertDoesNotExist()
            onAllNodesWithText("Preview: Retained draft").assertCountEquals(1)
            onNodeWithText("Preview: Retained draft").assertIsDisplayed()
            runOnIdle { previewOnly.value = false }
            onNodeWithTag("editor-draft").assertTextEquals("Retained draft")
            runOnIdle { width.value = 390.dp }
            onNodeWithTag("editor-draft").assertTextEquals("Retained draft")
            onNodeWithText("Preview: Retained draft").assertDoesNotExist()
        }

    @Test
    fun previewInsetsStayConsistentAcrossWidthsAndModes() =
        runAppComposeUiTest {
            val width = mutableStateOf(390.dp)
            val previewOnly = mutableStateOf(true)
            setContent {
                MaterialTheme {
                    Box(
                        Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(width.value, 500.dp).testTag("editor-fixture"),
                    ) {
                        AdaptiveEditorLayout(
                            preview = { Box(Modifier.size(10.dp).testTag("preview-content")) },
                            previewOnly = previewOnly.value,
                        ) { Text("Editor") }
                    }
                }
            }
            val narrowFrame = onNodeWithTag("editor-fixture").getUnclippedBoundsInRoot()
            val narrow = onNodeWithTag("preview-content").getUnclippedBoundsInRoot()
            assertEquals(16.dp, narrow.top - narrowFrame.top)
            assertEquals(16.dp, narrow.left - narrowFrame.left)
            runOnIdle { width.value = 1280.dp }
            val wideFrame = onNodeWithTag("editor-fixture").getUnclippedBoundsInRoot()
            val wide = onNodeWithTag("preview-content").getUnclippedBoundsInRoot()
            assertEquals(22.dp, wide.top - wideFrame.top)
            assertEquals(22.dp, wide.left - wideFrame.left)
            runOnIdle { previewOnly.value = false }
            val splitPreview = onNodeWithTag("preview-content").getUnclippedBoundsInRoot()
            assertEquals(22.dp, splitPreview.top - wideFrame.top)
            runOnIdle { width.value = 390.dp }
            onNodeWithTag("preview-content").assertDoesNotExist()
        }
}
