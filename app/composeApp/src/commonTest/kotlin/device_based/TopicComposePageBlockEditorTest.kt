/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.storyteller_f.alphaheart.app.pages.topic.BlockEditTopicPage
import kotlin.test.Test
import kotlin.test.assertEquals

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
        runComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("# Title") {
                latestMarkdown = it
            }

            onNodeWithText("Title").assertExists()
            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "# Title"
            }

            assertEquals("# Title", latestMarkdown)
        }

    @Test
    fun updatesMarkdownWhenListBlockChanges() =
        runComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("- old item") {
                latestMarkdown = it
            }

            onNodeWithText("old item").performClick()
            onNode(hasSetTextAction()).performTextReplacement("new item")

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "- new item"
            }

            assertEquals("- new item", latestMarkdown)
        }

    @Test
    fun insertsCodeBlockFromToolbar() =
        runComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("Intro") {
                latestMarkdown = it
            }

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "Intro"
            }
            onNodeWithContentDescription("Code Block").performClick()

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
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
        runComposeUiTest {
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
            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == expectedComplexMarkdown
            }

            assertEquals(
                expectedComplexMarkdown,
                latestMarkdown,
            )
        }

    @Test
    fun updatesMarkdownWhenQuoteBlockChanges() =
        runComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("> old quote") {
                latestMarkdown = it
            }

            onNodeWithText("old quote").performClick()
            onNode(hasSetTextAction()).performTextReplacement("new quote")

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "> new quote"
            }

            assertEquals("> new quote", latestMarkdown)
        }

    @Test
    fun updatesMarkdownWhenCodeBlockChanges() =
        runComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("```\nold()\n```") {
                latestMarkdown = it
            }

            onNodeWithText("old()").performClick()
            onNode(hasSetTextAction()).performTextReplacement("new()")

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "```\nnew()\n```"
            }

            assertEquals("```\nnew()\n```", latestMarkdown)
        }

    @Test
    fun deletesLastBlockAndKeepsEditorUsable() =
        runComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("Only block") {
                latestMarkdown = it
            }

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "Only block"
            }
            onNodeWithContentDescription("Delete or change type").performClick()
            onNodeWithText("Delete").performClick()

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == ""
            }
            onNodeWithContentDescription("Heading").performClick()

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "# "
            }

            assertEquals("# ", latestMarkdown)
        }

    @Test
    fun changesListBlockToCodeBlockFromBlockMenu() =
        runComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("- item") {
                latestMarkdown = it
            }

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "- item"
            }
            onNodeWithContentDescription("Delete or change type").performClick()
            onNodeWithText("To Code Block").performClick()

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "```\nitem\n```"
            }

            assertEquals("```\nitem\n```", latestMarkdown)
        }

    @Test
    fun insertsReferenceBlockFromMediaMenu() =
        runComposeUiTest {
            var latestMarkdown = ""

            setBlockEditorContent("Intro") {
                latestMarkdown = it
            }

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
                latestMarkdown == "Intro"
            }
            onNodeWithContentDescription("Insert media").performClick()
            onNodeWithText("Reference (CSA)").performClick()

            waitUntil(timeoutMillis = TEST_TIMEOUT_MILLIS) {
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

    private fun ComposeUiTest.setBlockEditorContent(input: String, updateInput: (String) -> Unit) {
        setContent {
            MaterialTheme {
                BlockEditTopicPage(
                    input = input,
                    updateInput = updateInput,
                )
            }
        }
    }

    private companion object {
        const val TEST_TIMEOUT_MILLIS = 3_000L
    }
}
