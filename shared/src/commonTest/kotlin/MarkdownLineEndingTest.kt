/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared

import com.storyteller_f.shared.utils.astNode
import com.storyteller_f.shared.utils.extractHeadParagraph
import com.storyteller_f.shared.utils.extractMarkdownHeadline
import com.storyteller_f.shared.utils.extractMarkdownMediaLink
import com.storyteller_f.shared.utils.extractMath
import com.storyteller_f.shared.utils.getLang
import com.storyteller_f.shared.utils.normalizeMarkdownLineEndings
import com.storyteller_f.shared.utils.readCodeFence
import com.storyteller_f.shared.utils.trimMarkdownUnusedContent
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownLineEndingTest {
    private val source =
        """
        正文 🌍
        
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

    @Test
    fun everyLineEndingKeepsFenceKindsBodiesAndOffsetsPairedWithTheRenderedSource() {
        for (ending in listOf("\n", "\r\n", "\r")) {
            val raw = source.replace("\n", ending)
            val view = normalizeMarkdownLineEndings(raw)
            assertEquals(source, view)
            val tree = astNode(raw)
            assertEquals(view.length, tree.endOffset)
            val fences = descendants(tree).filter { it.type == MarkdownElementTypes.CODE_FENCE }
            assertEquals(listOf("kotlin", "math", "csa"), fences.map { getLang(it, raw) })
            assertEquals(
                listOf("val first = 1\nval second = 2", "x^2", "/community/7"),
                fences.map { readCodeFence(it, raw) },
            )
            fences.forEach { node ->
                assertTrue(node.getTextInNode(view).startsWith("```"))
                assertTrue(node.getTextInNode(view).endsWith("```"))
            }
            val math = extractMath(raw).single()
            assertEquals("x^2", math.content)
            assertEquals("```math${ending}x^2$ending```", raw.substring(math.start, math.end))
        }
    }

    @Test
    fun headlineAndAttachmentExtractionUseTheSameNormalizedOffsetsAfterUnicode() {
        val text =
            """
            # 标题 🌍
            
            Introduction
            
            ![图](image.avif)
            
            ```object
            {"name":"audio.flac"}
            ```
            """.trimIndent() + "\n"
        for (ending in listOf("\n", "\r\n", "\r")) {
            val raw = text.replace("\n", ending)
            assertEquals(extractMarkdownHeadline(text), extractMarkdownHeadline(raw))
            assertEquals(extractHeadParagraph(text), extractHeadParagraph(raw))
            assertEquals(listOf("image.avif", "audio.flac"), extractMarkdownMediaLink(raw))
            assertEquals(trimMarkdownUnusedContent(text), trimMarkdownUnusedContent(raw))
        }
    }
}

private fun descendants(node: ASTNode): List<ASTNode> = listOf(node) + node.children.flatMap(::descendants)
