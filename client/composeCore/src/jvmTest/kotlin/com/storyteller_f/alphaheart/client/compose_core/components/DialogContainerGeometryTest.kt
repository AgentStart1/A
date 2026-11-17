/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class DialogContainerGeometryTest {
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
    fun closeActionAlignsWithTitleWithoutAddingDialogHeight() =
        runComposeUiTest {
            val width = mutableStateOf(360.dp)
            var dismisses = 0
            setContent {
                MaterialTheme {
                    Box(Modifier.width(width.value).testTag("dialog")) {
                        DialogContainer(onDismissRequest = { dismisses++ }) {
                            Text("Dialog title", Modifier.testTag("title"), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
            for (viewportWidth in listOf(360.dp, 800.dp)) {
                runOnIdle { width.value = viewportWidth }
                val title = onNodeWithTag("title").fetchSemanticsNode().boundsInRoot
                val close = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
                val dialog = onNodeWithTag("dialog").fetchSemanticsNode().boundsInRoot
                val density = title.height / 28f
                assertTrue(abs(title.center.y - close.center.y) <= density)
                assertEquals(24f * density + title.height + 80f * density + 24f * density, dialog.height, density)
                onNodeWithContentDescription("Close").performClick()
            }
            runOnIdle { assertEquals(2, dismisses) }
        }

    @Test
    fun closeActionStaysWithinIconHeaderAndLeavesItsNameVisible() =
        runComposeUiTest {
            val width = mutableStateOf(360.dp)
            var dismisses = 0
            setContent {
                MaterialTheme {
                    Box(Modifier.width(width.value)) {
                        DialogContainer(onDismissRequest = { dismisses++ }) {
                            Row(
                                Modifier.fillMaxWidth().height(50.dp).testTag("header"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(50.dp))
                                Text("Room name", Modifier.padding(start = 12.dp, end = 40.dp).testTag("name"))
                            }
                            Spacer(Modifier.height(80.dp))
                        }
                    }
                }
            }
            for (viewportWidth in listOf(360.dp, 800.dp)) {
                runOnIdle { width.value = viewportWidth }
                val header = onNodeWithTag("header").fetchSemanticsNode().boundsInRoot
                val name = onNodeWithTag("name").fetchSemanticsNode().boundsInRoot
                val close = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
                assertTrue(close.center.y in header.top..header.bottom)
                assertTrue(name.right <= close.left)
                onNodeWithContentDescription("Close").performClick()
            }
            runOnIdle { assertEquals(2, dismisses) }
        }
}
