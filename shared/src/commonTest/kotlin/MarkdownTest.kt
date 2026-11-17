/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared

import com.storyteller_f.shared.utils.astNode
import com.storyteller_f.shared.utils.extractHeadParagraph
import com.storyteller_f.shared.utils.extractMarkdownHeadline
import com.storyteller_f.shared.utils.extractMarkdownMediaLink
import com.storyteller_f.shared.utils.readInlineMath
import com.storyteller_f.shared.utils.trimMarkdownUnusedContent
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.accept
import org.intellij.markdown.ast.acceptChildren
import org.intellij.markdown.ast.visitors.Visitor
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownTest {
    @Test
    fun preservesUnicodeDocumentLength() {
        val text = "正文 🌍 **加粗**"
        assertEquals(text.length, astNode(text).endOffset)
    }

    @Test
    fun preservesUnicodeParagraph() {
        val text = "正文 🌍 **加粗**\n\n第二段"
        assertEquals("正文 🌍 **加粗**", extractHeadParagraph(text))
    }

    @Test
    fun preservesUnicodeMediaOffsets() {
        val text = "正文 🌍 **加粗**\n\n![图片](图片.png)"
        assertEquals(listOf("图片.png"), extractMarkdownMediaLink(text))
    }

    @Test
    fun `test extract headline`() {
        val markdownText =
            """
            # First Level Header

            Some introductory text before the second level header.

            Hello.
            ```c
                print();
            ```
            ## Second Level Header

            Content after the second level header.
            """.trimIndent()
        assertEquals(
            """
            # First Level Header
            Some introductory text before the second level header.
            Hello.
            """.trimIndent(),
            extractMarkdownHeadline(markdownText),
        )
        assertEquals("*hello*", extractMarkdownHeadline("*hello*"))
    }

    @Test
    fun `test extract media`() {
        val text =
            """
            ![test](/test.jpg "test")
            """.trimIndent()
        val list = extractMarkdownMediaLink(text)
        assertEquals("/test.jpg", list.first())
    }

    @Test
    fun `test extract paragraph`() {
        val r = extractHeadParagraph("test")
        assertEquals("test", r)
    }

    @Test
    fun `test extract image`() {
        assertEquals(
            """![I00012733.jpg](I00012733.jpg "I00012733.jpg")""",
            extractMarkdownHeadline("""![I00012733.jpg](I00012733.jpg "I00012733.jpg")"""),
        )
    }

    @Test
    fun `test extract inline math`() {
        val markdownText = """${'$'}\`\sqrt{3x-1}+(1+x)^2\`${'$'}"""
        val parsedTree = astNode(markdownText)
        parsedTree.accept(
            object : Visitor {
                override fun visitNode(node: ASTNode) {
                    if (node.type == GFMElementTypes.INLINE_MATH) readInlineMath(node, markdownText)
                    node.acceptChildren(this)
                }
            },
        )
    }

    @Test
    fun `test trim keeps plain text`() {
        assertEquals(
            "appium-favorite-topic-1781614397799",
            trimMarkdownUnusedContent("appium-favorite-topic-1781614397799"),
        )
    }

    @Test
    fun `test filter code fence unused content`() {
        val markdownText =
            """
            hello
            ```csa

                /user/1
            ```


            ```object
            {
                "contentType": "image/png",
                "name": "imag.png",
                "title": "Image title",
                "hello": "world"
            }
            ```
            """.trimIndent()
        val message = trimMarkdownUnusedContent(markdownText)
        assertEquals(
            """
            hello
            ```csa
            /user/1
            ```
            
            ```object
            {"name":"imag.png","title":"Image title"}
            ```
            """.trimIndent(),
            message,
        )
    }

    @Test
    fun `test preserve asciidoc fence language`() {
        val markdownText =
            """
            ```asciidoc
            = AsciiDoc Preview
            ```
            """.trimIndent()

        assertEquals(markdownText, trimMarkdownUnusedContent(markdownText))
    }

    @Test
    fun trimPreservesGenericFenceLanguagesAndNormalizesMixedLineEndings() {
        val crlf = charArrayOf('\r', '\n').concatToString()
        val inputFences =
            listOf(
                """
                ```kotlin
                val answer = 42
                ```
                """.trimIndent().withCrLfLineEndings(),
                """
                ```mermaid
                flowchart LR
                A --> B
                ```
                """.trimIndent(),
                """
                ```math
                x^2 + y^2 = z^2
                ```
                """.trimIndent().withCrLfLineEndings(),
                """
                ```
                plain code
                ```
                """.trimIndent(),
            )
        val input = inputFences.joinToString(crlf + crlf)
        val expected =
            """
            ```kotlin
            val answer = 42
            ```

            ```mermaid
            flowchart LR
            A --> B
            ```

            ```math
            x^2 + y^2 = z^2
            ```

            ```
            plain code
            ```
            """.trimIndent().trimEnd()

        assertEquals(expected, trimMarkdownUnusedContent(input))
    }
}

private fun String.withCrLfLineEndings(): String =
    buildString {
        for (character in this@withCrLfLineEndings) {
            if (character == '\n') append('\r')
            append(character)
        }
    }
