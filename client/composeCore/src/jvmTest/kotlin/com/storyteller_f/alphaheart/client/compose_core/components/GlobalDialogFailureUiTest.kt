/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogState
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class GlobalDialogFailureUiTest {
    @Test
    fun `failure explains the operation and both localized dismissal actions preserve caller state`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
            runComposeUiTest {
                val visible = mutableStateOf(false)
                val draft = mutableStateOf("原名称")
                var dismissals = 0
                setContent {
                    Column {
                        TextField(draft.value, { draft.value = it }, Modifier.testTag("draft"))
                        ReadingButton({ visible.value = true }) { Text("模拟保存失败") }
                    }
                    if (visible.value) {
                        GlobalDialogInternal(
                            GlobalDialogState.Error(IllegalStateException("503 synthetic diagnostic")),
                        ) {
                            visible.value = false
                            dismissals++
                        }
                    }
                }
                onNodeWithTag("draft").performTextReplacement("编辑中的名称 😀")
                onNodeWithText("模拟保存失败").performClick()
                onNodeWithText("操作失败").assertIsDisplayed()
                onNodeWithText("操作未完成，请关闭提示后重试。").assertIsDisplayed()
                onNodeWithText("错误详情").assertIsDisplayed()
                onNodeWithText("503 synthetic diagnostic").assertIsDisplayed()
                onNodeWithText("确定").performClick()
                onNodeWithText("操作失败").assertDoesNotExist()
                onNodeWithTag("draft").assertTextEquals("编辑中的名称 😀")
                onNodeWithText("模拟保存失败").performClick()
                onNodeWithContentDescription("关闭").performClick()
                onNodeWithTag("draft").assertTextEquals("编辑中的名称 😀")
                assertEquals(2, dismissals)
            }
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `short failures wrap naturally and long diagnostics scroll within a bounded body at both widths`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            runComposeUiTest {
                val width = mutableStateOf(360.dp)
                val diagnostic = mutableStateOf("503 diagnostic")
                setContent {
                    Box(Modifier.width(width.value).testTag("failure-bounds")) {
                        ReadingFailureSurface(IllegalStateException(diagnostic.value), rememberScrollState()) {}
                    }
                }
                for (size in listOf(360.dp, 800.dp)) {
                    runOnIdle {
                        width.value = size
                        diagnostic.value = "503 diagnostic"
                    }
                    val compact = onNodeWithTag("action-menu-surface").getUnclippedBoundsInRoot()
                    assertEquals(320.dp, compact.right - compact.left)
                    val compactTitle = onNodeWithText("Operation failed").getUnclippedBoundsInRoot()
                    assertEquals(compact.left + 21.dp, compactTitle.left)
                    assertEquals(compact.top + 21.dp, compactTitle.top)
                    assertTrue(compact.bottom - compact.top < 320.dp)
                    runOnIdle {
                        diagnostic.value = (1..200).joinToString("\n") { "503 diagnostic line $it" }
                    }
                    val title = onNodeWithText("Operation failed").fetchSemanticsNode().boundsInRoot
                    val button = onNodeWithText("OK").fetchSemanticsNode().boundsInRoot
                    val bounds = onNodeWithTag("action-menu-surface").getUnclippedBoundsInRoot()
                    val details = onNodeWithTag("failure-details").getUnclippedBoundsInRoot()
                    assertEquals(320.dp, bounds.right - bounds.left)
                    assertTrue(bounds.bottom - bounds.top > compact.bottom - compact.top)
                    assertTrue(details.bottom - details.top <= 160.dp)
                    assertTrue(
                        onAllNodes(hasScrollAction()).fetchSemanticsNodes().any {
                            it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.maxValue()?.let { max ->
                                max > 0f
                            } == true
                        },
                    )
                    onNodeWithTag("failure-details").performTouchInput { swipeUp() }
                    assertEquals(title, onNodeWithText("Operation failed").fetchSemanticsNode().boundsInRoot)
                    assertEquals(button, onNodeWithText("OK").fetchSemanticsNode().boundsInRoot)
                    assertEquals(bounds, onNodeWithTag("action-menu-surface").getUnclippedBoundsInRoot())
                    assertTrue(
                        onNodeWithTag("failure-details").fetchSemanticsNode().config
                            .getOrNull(SemanticsProperties.VerticalScrollAxisRange)?.value()?.let { it > 0f } == true,
                    )
                    onNodeWithText("OK").assertIsDisplayed()
                    onNodeWithContentDescription("Close").assertIsDisplayed()
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
