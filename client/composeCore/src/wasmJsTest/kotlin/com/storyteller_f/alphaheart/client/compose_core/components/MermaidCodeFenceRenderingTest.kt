/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.storyteller.alphaheart.client.composecore.markdown.MERMAID_SOURCE
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
internal class MermaidCodeFenceRenderingTest {
    @Test
    fun customCodeFenceRendersDiagramAtNarrowWidth() = assertMermaidRenderedAt(390)

    @Test
    fun customCodeFenceRendersDiagramAtWideWidth() = assertMermaidRenderedAt(900)

    private fun assertMermaidRenderedAt(width: Int) =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    Box(Modifier.width(width.dp)) {
                        CustomMarkdown(
                            plain = "```mermaid\n$MERMAID_SOURCE\n```",
                            isEmbed = false,
                            imageTransformer = noImages,
                            dimensionMap = persistentMapOf(),
                        ) { model ->
                            CustomCodeFence(
                                modal = model,
                                mediaList = emptyMap(),
                                refBlock = { error("This fixture contains no references") },
                                onClick = {},
                            )
                        }
                    }
                }
            }

            awaitIdle()
            try {
                // Rendering uses a real dispatcher; let browser Promise callbacks run between frames.
                withContext(Dispatchers.Default) {
                    withTimeout(10_000) {
                        while (true) {
                            waitForIdle()
                            val diagram =
                                onAllNodesWithContentDescription("Mermaid diagram").fetchSemanticsNodes().isNotEmpty()
                            val fallback =
                                onAllNodesWithText("A[Community] --> B[Room]", substring = true)
                                    .fetchSemanticsNodes().isNotEmpty()
                            if (diagram || fallback) break
                            delay(1)
                        }
                    }
                }
            } catch (timeout: TimeoutCancellationException) {
                throw AssertionError("Diagram/fallback semantics: ${onRoot().printToString()}", timeout)
            }
            onNodeWithContentDescription("Mermaid diagram").assertIsDisplayed()
            onNodeWithText("A[Community] --> B[Room]", substring = true).assertDoesNotExist()
        }
}

private val noImages =
    object : ImageTransformer {
        @androidx.compose.runtime.Composable
        override fun transform(link: String): ImageData = error("This diagram fixture contains no images")
    }
