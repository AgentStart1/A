/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CompletableDeferred
import java.awt.datatransfer.DataFlavor
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class FieldValueDialogTest {
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
    fun `field copy completes after the modal has closed and keeps the complete value`() =
        runComposeUiTest {
            val value = "完整值 😀\nsecond line\n"
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val copied = mutableStateOf<String?>(null)
            setContent {
                Box(Modifier.width(360.dp)) {
                    ExpandableFieldValue(value, "Field name") { text ->
                        started.complete(Unit)
                        release.await()
                        copied.value = text
                    }
                }
            }
            onNodeWithContentDescription("View full value").performClick()
            onNodeWithText("Field name").assertIsDisplayed()
            onNodeWithText("Copy").performClick()
            onNodeWithText("Field name").assertDoesNotExist()
            waitUntil { started.isCompleted }
            assertFalse(release.isCompleted)
            assertEquals(null, copied.value)
            runOnIdle { release.complete(Unit) }
            waitUntil { copied.value == value }
            onNodeWithContentDescription("View full value").performClick()
            onNodeWithContentDescription("Close").performClick()
            onNodeWithText("Field name").assertDoesNotExist()
            assertEquals(value, copied.value)
        }

    @Test
    fun `long field stays scrollable with accessible close and copy at both widths`() =
        runComposeUiTest {
            val width = mutableStateOf(360.dp)
            val value = (1..200).joinToString("\n") { "Field value line $it" }
            var copies = 0
            var closes = 0
            setContent {
                MaterialTheme {
                    Box(Modifier.width(width.value).testTag("field-bounds")) {
                        FieldValueDialogContent("Field name", value, { closes++ }, { copies++ })
                    }
                }
            }
            for (size in listOf(360.dp, 800.dp)) {
                runOnIdle { width.value = size }
                val bounds = onNodeWithTag("field-bounds").fetchSemanticsNode().boundsInRoot
                val close = onNodeWithContentDescription("Close").fetchSemanticsNode().boundsInRoot
                val copy = onNodeWithText("Copy").fetchSemanticsNode().boundsInRoot
                assertTrue(close.left >= bounds.left && close.right <= bounds.right)
                assertTrue(copy.left >= bounds.left && copy.right <= bounds.right)
                assertTrue(copy.bottom <= bounds.bottom)
                onNodeWithText("Copy").assertIsDisplayed().performClick()
                val scroll = onAllNodes(hasScrollAction())[1]
                val range = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
                assertTrue(range.maxValue() > 0f)
                val before = range.value()
                scroll.performTouchInput { swipeUp() }
                assertTrue(range.value() > before)
                onNodeWithContentDescription("Close").assertIsDisplayed().performClick()
            }
            assertEquals(2, copies)
            assertEquals(2, closes)
        }

    @Test
    fun `reusable copy confirmation writes isolated clipboard before removing its modal`() =
        runComposeUiTest {
            val value = "synthetic field value"
            val memory = java.awt.datatransfer.Clipboard("isolated-field-test")
            var nativeReads = 0
            val clipboard =
                object : Clipboard {
                    override val nativeClipboard: Any
                        get() {
                            nativeReads++
                            return memory
                        }
                    override suspend fun getClipEntry(): ClipEntry? = null
                    override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
                }
            val shown = mutableStateOf(true)
            var closedAfterWrite = false
            setContent {
                CompositionLocalProvider(LocalClipboard provides clipboard) {
                    if (shown.value) {
                        CustomAlertDialogInternal(
                            CustomAlertDialogState("Field name", value, enableCopy = true),
                            {
                                closedAfterWrite =
                                    memory.isDataFlavorAvailable(DataFlavor.stringFlavor) &&
                                    memory.getData(DataFlavor.stringFlavor) == value
                                shown.value = false
                            },
                            {},
                        )
                    }
                }
            }
            onNodeWithText("Copy").performClick()
            waitUntil { !shown.value }
            assertTrue(closedAfterWrite)
            assertEquals(1, nativeReads)
            onNodeWithText("Field name").assertDoesNotExist()
        }
}
