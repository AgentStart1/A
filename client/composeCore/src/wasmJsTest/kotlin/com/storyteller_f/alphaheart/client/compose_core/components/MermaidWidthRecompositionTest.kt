/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
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
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class MermaidWidthRecompositionTest {
    @Test
    fun changingWidthKeepsRenderedMermaidCompositionMounted() =
        runComposeUiTest {
            val width = mutableStateOf(390.dp)
            val compositionEvents = mutableListOf<String>()
            setContent {
                MaterialTheme {
                    Box(Modifier.width(width.value)) {
                        CustomMarkdown(
                            plain = "```mermaid\n$MERMAID_SOURCE\n```",
                            isEmbed = false,
                            imageTransformer = noImages,
                            dimensionMap = persistentMapOf(),
                        ) { model ->
                            MermaidLifetimeProbe(model, compositionEvents)
                        }
                    }
                }
            }

            awaitMermaidDiagram()
            val initialEvents = compositionEvents.toList()

            runOnIdle { width.value = 1280.dp }
            awaitMermaidDiagram()
            assertEquals(
                initialEvents,
                compositionEvents,
                "A width-only remeasurement disposed or recreated the parsed Mermaid code fence",
            )
        }

    private suspend fun androidx.compose.ui.test.ComposeUiTest.awaitMermaidDiagram() {
        try {
            // Mermaid rendering and Markdown parsing complete through real dispatcher/browser callbacks.
            withContext(Dispatchers.Default) {
                withTimeout(10_000) {
                    while (true) {
                        waitForIdle()
                        if (onAllNodesWithContentDescription("Mermaid diagram").fetchSemanticsNodes().isNotEmpty()) {
                            break
                        }
                        delay(1)
                    }
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            throw AssertionError("Mermaid diagram semantics: ${onRoot().printToString()}", timeout)
        }
        onNodeWithContentDescription("Mermaid diagram").assertIsDisplayed()
    }
}

@androidx.compose.runtime.Composable
private fun MermaidLifetimeProbe(model: MarkdownComponentModel, events: MutableList<String>) {
    DisposableEffect(model.node) {
        events += "mount"
        onDispose { events += "dispose" }
    }
    CustomCodeFence(
        modal = model,
        mediaList = emptyMap(),
        refBlock = { error("This fixture contains no references") },
        onClick = {},
    )
}

private val noImages =
    object : ImageTransformer {
        @androidx.compose.runtime.Composable
        override fun transform(link: String): ImageData = error("This diagram fixture contains no images")
    }
