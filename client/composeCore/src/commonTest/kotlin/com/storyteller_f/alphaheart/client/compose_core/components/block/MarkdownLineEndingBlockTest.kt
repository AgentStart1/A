/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components.block

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownLineEndingBlockTest {
    @Test
    fun codeMathAndCsaRemainSeparateBlocksForLfCrlfAndBareCr() {
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
            val blocks = parseMarkdownToBlocks(source.replace("\n", ending))
            assertEquals(3, blocks.size)
            val code = blocks.filterIsInstance<ContentBlock.CodeBlock>().single()
            assertEquals("kotlin", code.language)
            assertEquals("val first = 1\nval second = 2", code.content)
            assertEquals("x^2", blocks.filterIsInstance<ContentBlock.MathBlock>().single().content)
            assertEquals("/community/7", blocks.filterIsInstance<ContentBlock.RefBlock>().single().refPath)
        }
    }
}
