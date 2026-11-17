/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.storyteller_f.shared.utils.getLang
import com.storyteller_f.shared.utils.readCodeFence
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class MarkdownLineEndingRenderTest {
    @Test
    fun actualMarkdownComponentsReceiveTheSameSourceAndThreeSeparateFencesForEveryLineEnding() {
        val source =
            """
            ```kotlin
            val first = 1
            val second = 2
            ```
            
            ```math
            x^2
            ```
            
            ```csa
            /community/7
            ```
            """.trimIndent() + "\n"
        for (ending in listOf("\n", "\r\n", "\r")) {
            runComposeUiTest {
                val observedSources = mutableSetOf<String>()
                setContent {
                    MaterialTheme {
                        Box(Modifier.width(390.dp)) {
                            CustomMarkdown(
                                plain = source.replace("\n", ending),
                                isEmbed = false,
                                imageTransformer = noImages,
                                dimensionMap = persistentMapOf(),
                            ) { model ->
                                observedSources += model.content
                                Text(
                                    "${getLang(model.node, model.content)}|${readCodeFence(model.node, model.content)}",
                                )
                            }
                        }
                    }
                }
                waitUntil { onAllNodesWithText("csa|/community/7").fetchSemanticsNodes().size == 1 }
                onNodeWithText("kotlin|val first = 1\nval second = 2").assertExists()
                onNodeWithText("math|x^2").assertExists()
                onNodeWithText("csa|/community/7").assertExists()
                runOnIdle { assertEquals(setOf(source), observedSources) }
            }
        }
    }
}

private val noImages =
    object : ImageTransformer {
        @Composable
        override fun transform(link: String): ImageData = error("This fence-only fixture contains no images")
    }
