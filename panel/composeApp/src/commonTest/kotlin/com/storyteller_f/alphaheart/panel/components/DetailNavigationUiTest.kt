/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DetailNavigationUiTest {
    @Test
    fun compactDetailNavigationReachesNonAdjacentPageWithOneClick() = verifyNavigation(390.dp)

    @Test
    fun wideDetailNavigationReachesNonAdjacentPageWithOneClick() = verifyNavigation(1280.dp)

    private fun verifyNavigation(width: Dp) =
        runComposeUiTest {
            val routes = List(3) { NavRoute("/section-$it", Icons.Default.Public, "Tab $it") }
            setContent {
                MaterialTheme {
                    ReadingInteractions {
                        Box(
                            Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                                .requiredSize(width, 600.dp).testTag("navigation-fixture"),
                        ) {
                            val pager = rememberPagerState { routes.size }
                            val section: @androidx.compose.runtime.Composable (Int) -> Unit = { page ->
                                Column {
                                    Text("Page $page")
                                    if (page == 0) {
                                        var draft by rememberSaveable { mutableStateOf("") }
                                        BasicTextField(draft, { draft = it }, Modifier.testTag("section-draft"))
                                    }
                                }
                            }
                            DetailNavigationScaffold(pager, routes, topBar = {}, content = section)
                        }
                    }
                }
            }
            val bounds = onNodeWithTag("navigation-fixture").getUnclippedBoundsInRoot()
            assertEquals(width, bounds.right - bounds.left)
            onNodeWithTag("section-draft").performTextReplacement("Retained draft")
            onNodeWithContentDescription("Tab 2").performClick()
            waitForIdle()
            onNodeWithText("Page 2").assertIsDisplayed()
            onNodeWithContentDescription("Tab 2").assertIsSelected()
            onNodeWithContentDescription("Tab 0").assertIsNotSelected()
            onNodeWithContentDescription("Tab 0").performClick()
            waitForIdle()
            onNodeWithText("Page 0").assertIsDisplayed()
            onNodeWithContentDescription("Tab 0").assertIsSelected()
            onNodeWithContentDescription("Tab 2").assertIsNotSelected()
            onNodeWithTag("section-draft").assertTextEquals("Retained draft")
        }
}
