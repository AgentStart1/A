/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class ReadingFormInputTest {
    private val originalLocale = java.util.Locale.getDefault()

    @kotlin.test.BeforeTest
    fun setEnglishLocale() {
        java.util.Locale.setDefault(java.util.Locale.ENGLISH)
    }

    @kotlin.test.AfterTest
    fun restoreLocale() {
        java.util.Locale.setDefault(originalLocale)
    }

    @Test
    fun externalLabelRetainsUnicodeEditingAndSelectionAcrossWidths() =
        runComposeUiTest {
            val width = mutableStateOf(390.dp)
            var captured = ""
            setContent {
                MaterialTheme {
                    var text by remember { mutableStateOf("") }
                    Box(Modifier.width(width.value)) {
                        ReadingOutlinedTextField(text, {
                            text = it
                            captured = it
                        }, label = { Text("Name") })
                    }
                }
            }
            val editor = onNode(hasSetTextAction())
            editor.performTextInput("测试 Alpha 😀")
            editor.performTextInputSelection(TextRange(3, 8))
            editor.performTextInput("Beta")
            runOnIdle { assertEquals("测试 Beta 😀", captured) }
            runOnIdle { width.value = 800.dp }
            editor.assertTextContains("测试 Beta 😀")
            editor.performTextInputSelection(TextRange(0, 2))
            editor.performTextInput("中文")
            runOnIdle { assertEquals("中文 Beta 😀", captured) }
            onNodeWithText("Name", useUnmergedTree = true).assertIsDisplayed()
        }

    @Test
    fun directFieldCopyDoesNotOpenAModalAndRetainsTheCompleteValue() =
        runComposeUiTest {
            val value = "first\n完整值 😀\nlast"
            var copied: String? = null
            setContent {
                MaterialTheme {
                    Box(Modifier.width(390.dp)) {
                        ExpandableFieldValue(value, "Synthetic field") { copied = it }
                    }
                }
            }
            onNodeWithContentDescription("Copy").performClick()
            waitUntil { copied != null }
            runOnIdle { assertEquals(value, copied) }
            onNodeWithText("Synthetic field").assertDoesNotExist()
        }
}
