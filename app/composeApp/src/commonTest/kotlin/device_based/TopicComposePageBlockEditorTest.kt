/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.storyteller_f.alphaheart.app.components.block.EditableParagraphBlock
import com.storyteller_f.alphaheart.app.pages.topic.BlockEditTopicPage
import com.storyteller_f.alphaheart.app.pages.topic.EditTopicPage
import com.storyteller_f.alphaheart.client.compose_core.markdown.ContentBlock
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val expectedComplexMarkdown =
    """
    # Title

    > Quote

    ``` kotlin
    println(1)
    ```

    ```math
    x^2
    ```
    """.trimIndent()

@OptIn(ExperimentalTestApi::class)
class TopicComposePageBlockEditorTest {
    @Test
    fun rendersInitialMarkdownInBlockEditor() =
        runAppComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("# Title") {
                latestMarkdown = it
            }

            onNodeWithText("Title").assertExists()
            assertEquals("", latestMarkdown)
        }

    @Test
    fun updatesMarkdownWhenListBlockChanges() =
        runAppComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("- old item") {
                latestMarkdown = it
            }

            onNodeWithText("old item").performClick()
            onNode(hasSetTextAction()).performTextReplacement("new item")

            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "- new item"
            }

            assertEquals("- new item", latestMarkdown)
        }

    @Test
    fun insertsCodeBlockFromToolbar() =
        runAppComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("Intro") {
                latestMarkdown = it
            }

            onNodeWithText("Intro").assertExists()
            assertEquals("", latestMarkdown)
            onNodeWithContentDescription("Code Block").performClick()

            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown ==
                    """
                    Intro

                    ```

                    ```
                    """.trimIndent()
            }

            assertEquals(
                """
                Intro

                ```

                ```
                """.trimIndent(),
                latestMarkdown,
            )
        }

    @Test
    fun rendersMixedMarkdownBlocks() =
        runAppComposeUiTest {
            val input =
                """
                # Title

                > Quote

                ```kotlin
                println(1)
                ```

                ```math
                x^2
                ```
                """.trimIndent()
            var latestMarkdown = ""

            setBlockEditorContent(input) {
                latestMarkdown = it
            }

            onNodeWithText("Title").assertExists()
            onNodeWithText("Quote").assertExists()
            onNodeWithText("kotlin").assertExists()
            onNodeWithText("println(1)").assertExists()
            onNodeWithText("Math Formula").assertExists()
            onNodeWithText("x^2").assertExists()
            assertEquals("", latestMarkdown)
            onNodeWithText("println(1)").performClick()
            onNode(hasSetTextAction()).performTextReplacement("println(2)")
            val expectedAfterEdit = expectedComplexMarkdown.replace("println(1)", "println(2)")
            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == expectedAfterEdit
            }
            assertEquals(expectedAfterEdit, latestMarkdown)
        }

    @Test
    fun updatesMarkdownWhenQuoteBlockChanges() =
        runAppComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("> old quote") {
                latestMarkdown = it
            }

            onNodeWithText("old quote").performClick()
            onNode(hasSetTextAction()).performTextReplacement("new quote")

            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "> new quote"
            }

            assertEquals("> new quote", latestMarkdown)
        }

    @Test
    fun updatesMarkdownWhenCodeBlockChanges() =
        runAppComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("```\nold()\n```") {
                latestMarkdown = it
            }

            onNodeWithText("old()").performClick()
            onNode(hasSetTextAction()).performTextReplacement("new()")

            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "```\nnew()\n```"
            }

            assertEquals("```\nnew()\n```", latestMarkdown)
        }

    @Test
    fun deletesLastBlockAndKeepsEditorUsable() =
        runAppComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("Only block") {
                latestMarkdown = it
            }

            onNodeWithText("Only block").assertExists()
            assertEquals("", latestMarkdown)
            onNodeWithContentDescription("Delete or change type").performClick()
            onNodeWithText("Delete").performClick()

            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == ""
            }
            onNodeWithContentDescription("Heading").performClick()

            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "# "
            }

            assertEquals("# ", latestMarkdown)
        }

    @Test
    fun changesListBlockToCodeBlockFromBlockMenu() =
        runAppComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("- item") {
                latestMarkdown = it
            }

            onNodeWithText("item").assertExists()
            assertEquals("", latestMarkdown)
            onNodeWithContentDescription("Delete or change type").performClick()
            onNodeWithText("To Code Block").performClick()

            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "```\nitem\n```"
            }

            assertEquals("```\nitem\n```", latestMarkdown)
        }

    @Test
    fun insertsReferenceBlockFromMediaMenu() =
        runAppComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("Intro") {
                latestMarkdown = it
            }

            onNodeWithText("Intro").assertExists()
            assertEquals("", latestMarkdown)
            onNodeWithContentDescription("Insert media").performClick()
            onNodeWithText("Reference (CSA)").performClick()

            awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown ==
                    """
                    Intro

                    ```csa
                    /topic/
                    ```
                    """.trimIndent()
            }

            assertEquals(
                """
                Intro

                ```csa
                /topic/
                ```
                """.trimIndent(),
                latestMarkdown,
            )
        }

    @Test
    fun unfocusedParagraphRendersInlineFormattingWithoutMarkdownMarkers() =
        runAppComposeUiTest {
            setContent {
                MaterialTheme {
                    EditableParagraphBlock(
                        ContentBlock.Paragraph("formatted", "**Bold** and *italic*"),
                        isFocused = false,
                        onContentChange = {},
                        onFocusChange = {},
                    )
                }
            }
            val text = onNodeWithText("Bold and italic").fetchSemanticsNode().config[SemanticsProperties.Text].single()
            assertTrue(text.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
            assertTrue(text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
            onNodeWithText("**Bold** and *italic*").assertDoesNotExist()
        }

    @Test
    fun headingRetainsBoldAndItalicAfterFocusAndBlur() =
        runAppComposeUiTest {
            val content = mutableStateOf("Heading text")
            val focused = mutableStateOf(false)
            setContent {
                MaterialTheme {
                    Column {
                        EditableParagraphBlock(
                            ContentBlock.Paragraph("heading", content.value, level = 2),
                            focused.value,
                            onContentChange = { content.value = it },
                            onFocusChange = { focused.value = it },
                        )
                        EditableParagraphBlock(
                            ContentBlock.Paragraph("other", "Blur target"),
                            isFocused = false,
                            onContentChange = {},
                            onFocusChange = { focused.value = false },
                        )
                    }
                }
            }
            onNodeWithText("Heading text").performClick()
            onNode(hasSetTextAction()).performTextInputSelection(TextRange(0, 7))
            onNodeWithContentDescription("Toggle bold").performClick()
            onNodeWithContentDescription("Toggle italic").performClick()
            onNodeWithText("Blur target").performClick()
            val text = onNodeWithText("Heading text").fetchSemanticsNode().config[SemanticsProperties.Text].single()
            assertTrue(
                text.spanStyles.any { it.item.fontWeight == FontWeight.Bold },
                "Bold should survive blur: ${content.value}",
            )
            assertTrue(text.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
            onNodeWithContentDescription("Toggle bold").assertDoesNotExist()
        }

    @Test
    fun focusingAndSelectingParagraphDoesNotPublishNormalizedSource() =
        runAppComposeUiTest {
            val source = "**Bold** and *italic*: ${'$'}x^2+y^2=z^2${'$'}; a=b; `x=y`  "
            val content = mutableStateOf(source)
            val focused = mutableStateOf(false)
            val publications = mutableListOf<String>()
            setContent {
                MaterialTheme {
                    EditableParagraphBlock(
                        ContentBlock.Paragraph("source", content.value),
                        focused.value,
                        onContentChange = {
                            publications += it
                            content.value = it
                        },
                        onFocusChange = { focused.value = it },
                    )
                }
            }
            runOnIdle { focused.value = true }
            onNode(hasSetTextAction()).performTextInputSelection(TextRange(0, 4))
            runOnIdle {
                assertEquals(emptyList(), publications, "Focus and selection must not rewrite original Markdown")
                assertEquals(source, content.value)
            }
        }

    @Test
    fun editingAndFormattingParagraphKeepsInlineMathAndCode() =
        runAppComposeUiTest {
            val source = "Plain ${'$'}x^2+y^2=z^2${'$'} and `a=b`"
            val content = mutableStateOf(source)
            val focused = mutableStateOf(true)
            val publications = mutableListOf<String>()
            setContent {
                MaterialTheme {
                    EditableParagraphBlock(
                        ContentBlock.Paragraph("source", content.value),
                        focused.value,
                        onContentChange = {
                            publications += it
                            content.value = it
                        },
                        onFocusChange = { focused.value = it },
                    )
                }
            }
            onNode(hasSetTextAction()).performTextInputSelection(TextRange(0, 5))
            runOnIdle { assertEquals(emptyList(), publications) }
            onNodeWithContentDescription("Toggle bold").performClick()
            onNodeWithContentDescription("Toggle italic").performClick()
            runOnIdle {
                assertTrue(publications.isNotEmpty(), "Formatting must publish an actual document edit")
                assertTrue(content.value.contains("***Plain***"), content.value)
                assertTrue(content.value.contains("${'$'}x^2+y^2=z^2${'$'}"), content.value)
                assertTrue(content.value.contains("`a=b`"), content.value)
            }
            onNode(hasSetTextAction()).performTextReplacement("Updated ${'$'}x=y${'$'} and `a=b`")
            runOnIdle {
                assertTrue(content.value.contains("Updated"), content.value)
                assertTrue(content.value.contains("${'$'}x=y${'$'}"), content.value)
                assertTrue(content.value.contains("`a=b`"), content.value)
            }
        }

    @Test
    fun firstTypingBatchAfterReenteringBlockEditorKeepsItsFinalCharacter() =
        runAppComposeUiTest {
            val source =
                "第一行 **粗体**\n" +
                    "第二行 ${'$'}x^2+y^2=z^2${'$'}\n" +
                    "第三行 `a=b`"
            val content = mutableStateOf(source)
            val blockMode = mutableStateOf(false)
            setContent {
                MaterialTheme {
                    if (blockMode.value) {
                        BlockEditTopicPage(content.value) { content.value = it }
                    } else {
                        EditTopicPage(content.value) { content.value = it }
                    }
                }
            }

            runOnIdle { blockMode.value = true }
            onNodeWithText("第三行 a=b", substring = true).assertExists()
            runOnIdle { blockMode.value = false }
            runOnIdle { blockMode.value = true }

            onNodeWithText("第三行 a=b", substring = true).performClick()
            val editor = onNode(hasSetTextAction())
            val editorText = editor.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
            editor.performTextInputSelection(TextRange(editorText.length))

            // Let the first edit publish its new block content, then enter the next
            // character before the recomposition can reconcile that echo.
            mainClock.autoAdvance = false
            try {
                editor.performTextInput(" c=")
                mainClock.advanceTimeByFrame()
                editor.performTextInput("d")
                mainClock.advanceTimeByFrame()
            } finally {
                mainClock.autoAdvance = true
            }

            try {
                awaitExternalCondition(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                    content.value.contains("c=d")
                }
            } catch (timeout: TimeoutCancellationException) {
                throw AssertionError("Expected c=d in published Markdown: ${content.value}", timeout)
            }
        }

    @Test
    fun focusedParagraphImportsExternalContentReplacement() =
        runAppComposeUiTest {
            val content = mutableStateOf("Original content")
            setContent {
                MaterialTheme {
                    EditableParagraphBlock(
                        block = ContentBlock.Paragraph("external-replacement", content.value),
                        isFocused = true,
                        onContentChange = { content.value = it },
                        onFocusChange = {},
                    )
                }
            }

            onNodeWithText("Original content").assertExists()
            runOnIdle { content.value = "External replacement" }
            onNodeWithText("External replacement").assertExists()
        }

    private fun ComposeUiTest.setBlockEditorContent(input: String, updateInput: (String) -> Unit) {
        var initialPublications = 0
        setContent {
            MaterialTheme {
                BlockEditTopicPage(
                    input = input,
                    updateInput = {
                        initialPublications++
                        updateInput(it)
                    },
                )
            }
        }
        runOnIdle { assertEquals(0, initialPublications, "Rendering must not rewrite the untouched draft") }
    }

    private companion object {
        const val TEST_TIMEOUT_MILLIS = 3_000L
    }
}
