/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class CachedRefreshGeometryTest {
    private val originalLocale = Locale.getDefault()

    @BeforeTest
    fun setEnglishLocale() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun cachedWrapContentRefreshDoesNotMoveFollowingContent() = verifyGeometry(referenceCell = false)

    @Test
    fun cachedReferenceRefreshDoesNotMoveFollowingContent() = verifyGeometry(referenceCell = true)

    private fun verifyGeometry(referenceCell: Boolean) =
        runComposeUiTest {
            val handler = RefreshGeometryHandler()
            val width = mutableStateOf(360.dp)
            setContent {
                MaterialTheme {
                    Column(Modifier.width(width.value)) {
                        if (referenceCell) {
                            RefCellStateView(handler, Modifier.fillMaxWidth()) {
                                Box(Modifier.fillMaxWidth().height(38.dp).testTag("cached-content"))
                            }
                        } else {
                            StateView(handler, Modifier.fillMaxWidth()) {
                                Box(Modifier.fillMaxWidth().height(38.dp).testTag("cached-content"))
                            }
                        }
                        Text("Following content", Modifier.testTag("following-content"))
                    }
                }
            }
            for (viewportWidth in listOf(360.dp, 800.dp)) {
                runOnIdle {
                    width.value = viewportWidth
                    handler.state.value = LoadingState.Done
                }
                val contentBounds = onNodeWithTag("cached-content").fetchSemanticsNode().boundsInRoot
                val followingBounds = onNodeWithTag("following-content").fetchSemanticsNode().boundsInRoot
                for (state in listOf(LoadingState.Loading, LoadingState.Error(Exception("Refresh failed")))) {
                    runOnIdle { handler.state.value = state }
                    assertEquals(contentBounds, onNodeWithTag("cached-content").fetchSemanticsNode().boundsInRoot)
                    assertEquals(followingBounds, onNodeWithTag("following-content").fetchSemanticsNode().boundsInRoot)
                }
                onNode(hasText("Refresh failed", substring = true) and hasClickAction()).performClick()
                runOnIdle {
                    assertEquals(LoadingState.Loading, handler.state.value)
                }
                assertEquals(followingBounds, onNodeWithTag("following-content").fetchSemanticsNode().boundsInRoot)
                runOnIdle { handler.state.value = LoadingState.Done }
                assertEquals(followingBounds, onNodeWithTag("following-content").fetchSemanticsNode().boundsInRoot)
            }
            runOnIdle { assertEquals(2, handler.refreshCount) }
        }
}

private class RefreshGeometryHandler : LoadingHandler<String> {
    override val data = MutableStateFlow<String?>("Cached content")
    override val state = MutableStateFlow<LoadingState?>(LoadingState.Done)
    var refreshCount = 0
        private set

    override fun refresh() {
        refreshCount += 1
        state.value = LoadingState.Loading
    }
}
