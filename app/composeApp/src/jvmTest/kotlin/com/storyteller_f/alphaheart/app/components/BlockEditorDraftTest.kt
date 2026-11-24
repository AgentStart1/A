/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.components.block.BlockEditor
import com.storyteller_f.alphaheart.client.compose_core.markdown.ContentBlock
import com.storyteller_f.alphaheart.client.compose_core.markdown.parseMarkdownToBlocks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class BlockEditorDraftTest {
    @Test
    fun mountingAndFocusingTheEditorKeepOriginalCrlfBytesUntilTheFirstRealTextEdit() {
        for (initiallyEmpty in listOf(false, true)) {
            runComposeUiTest {
                val raw = listOf("```kotlin", "val original = 1", "```", "").joinToString("\r\n")
                val blocks =
                    mutableStateListOf<ContentBlock>().apply {
                        if (!initiallyEmpty) addAll(parseMarkdownToBlocks(raw))
                    }
                var draft = raw
                var publications = 0
                setContent {
                    MaterialTheme {
                        ReadingInteractions {
                            Box(Modifier.width(390.dp).height(600.dp)) {
                                BlockEditor(blocks = blocks, initialMarkdown = raw) {
                                    draft = it
                                    publications++
                                }
                            }
                        }
                    }
                }
                onNodeWithText("val original = 1").assertIsDisplayed().performClick()
                runOnIdle {
                    assertEquals(raw, draft)
                    assertEquals(0, publications)
                }
                val field = onNode(hasSetTextAction()).performClick()
                runOnIdle {
                    assertEquals(raw, draft)
                    assertEquals(0, publications)
                }
                field.performTextReplacement("val changed = 2")
                waitUntil { publications > 0 }
                runOnIdle {
                    assertEquals(1, publications)
                    assertEquals("``` kotlin\nval changed = 2\n```", draft)
                }
            }
        }
    }

    @Test
    fun aFirstRealBlockChangeBeforeTheInitialObserverRunsIsNotDiscarded() =
        runComposeUiTest {
            val original = ContentBlock.CodeBlock("stable", "original", "kotlin")
            val blocks = mutableStateListOf<ContentBlock>(original)
            val publications = mutableListOf<String>()
            setContent {
                MaterialTheme {
                    ReadingInteractions {
                        LaunchedEffect(Unit) { blocks[0] = original.copy(content = "early edit") }
                        BlockEditor(blocks = blocks, onMarkdownChange = { publications += it })
                    }
                }
            }
            waitUntil { publications.isNotEmpty() }
            runOnIdle {
                assertEquals(1, publications.size)
                assertTrue(publications.single().contains("early edit"))
            }
        }
}
