/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class ReadingTranslationGeometryTest {
    @Test
    fun `wide reading shell accepts translation width while default menu stays compact`() =
        runComposeUiTest {
            val wide = mutableStateOf(false)
            setContent {
                MaterialTheme {
                    Box(Modifier.width(700.dp)) {
                        ReadingActionMenuSurface({}, { Text("Title") }, maxWidth = if (wide.value) 620.dp else 320.dp) {
                            ReadingTranslationResult { Text("Loading") }
                        }
                    }
                }
            }
            for ((mode, width) in listOf(false to 320.dp, true to 620.dp)) {
                runOnIdle { wide.value = mode }
                val bounds = onNodeWithTag("action-menu-surface").getUnclippedBoundsInRoot()
                assertEquals(width, bounds.right - bounds.left)
            }
        }

    @Test
    fun `result slot stays fixed across loading failure and scrolling long content`() =
        runComposeUiTest {
            val mode = mutableStateOf(0)
            setContent {
                MaterialTheme {
                    Box(Modifier.width(284.dp)) {
                        ReadingTranslationResult {
                            if (mode.value < 2) {
                                Text(if (mode.value == 0) "Loading" else "Failed")
                            } else {
                                Column(
                                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("long-result"),
                                ) {
                                    repeat(40) { Text("Result line $it") }
                                }
                            }
                        }
                    }
                }
            }
            val initial = onNodeWithTag("translation-result-slot").getUnclippedBoundsInRoot()
            assertEquals(160.dp, initial.bottom - initial.top)
            for (value in listOf(1, 2)) {
                runOnIdle { mode.value = value }
                assertEquals(initial, onNodeWithTag("translation-result-slot").getUnclippedBoundsInRoot())
            }
            onNodeWithText("Result line 39").performScrollTo()
            assertEquals(initial, onNodeWithTag("translation-result-slot").getUnclippedBoundsInRoot())
        }
}
