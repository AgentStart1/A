/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class ReadingPagerAccessibilityTest {
    @Test
    fun accessibilityScrollsSnapAccumulateAndLeavePointerNavigationAvailable() =
        runComposeUiTest {
            var pager: PagerState? = null
            setContent {
                MaterialTheme {
                    Box(Modifier.size(390.dp, 400.dp)) {
                        val state = rememberPagerState { 3 }
                        pager = state
                        ReadingHorizontalPager(state, Modifier.testTag("pager")) { Text("Page $it") }
                    }
                }
            }
            onNodeWithTag("pager").performSemanticsAction(SemanticsActions.ScrollBy) { it(162f, 0f) }
            runOnIdle {
                assertEquals(0, checkNotNull(pager).currentPage)
                assertEquals(0f, pager.currentPageOffsetFraction)
            }
            onNodeWithTag("pager").performSemanticsAction(SemanticsActions.ScrollBy) {
                it(390f, 0f)
                it(390f, 0f)
            }
            runOnIdle { assertEquals(2, checkNotNull(pager).currentPage) }
            onNodeWithText("Page 2").assertIsDisplayed()
            onNodeWithTag("pager").performSemanticsAction(SemanticsActions.ScrollBy) { it(-1000f, 0f) }
            runOnIdle { assertEquals(0, checkNotNull(pager).currentPage) }
            onNodeWithTag("pager").performTouchInput { swipeLeft() }
            runOnIdle {
                assertEquals(1, checkNotNull(pager).currentPage)
                assertEquals(0f, pager.currentPageOffsetFraction)
            }
            onNodeWithText("Page 1").assertIsDisplayed()
        }
}
