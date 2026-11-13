/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components.block

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Block 解析器和生成器的单元测试
 * 验证 Markdown <-> Block 双向转换的正确性.
 */
class BlockParserTest {
    // ========== 解析测试 ==========

    @Test
    fun `empty markdown returns an empty paragraph`() {
        val blocks = parseMarkdownToBlocks("")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Paragraph)
        assertEquals("", (blocks[0] as ContentBlock.Paragraph).content)
    }

    @Test
    fun `blank markdown returns an empty paragraph`() {
        val blocks = parseMarkdownToBlocks("   \n\n   ")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ContentBlock.Paragraph)
    }

    @Test
    fun `test parse plain paragraph`() {
        val markdown = "Hello, World!"
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        val paragraph = blocks[0] as ContentBlock.Paragraph
        assertEquals("Hello, World!", paragraph.content)
        assertEquals(0, paragraph.level)
    }

    @Test
    fun `test parse headings`() {
        val markdown = "# Heading 1"
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        val heading = blocks[0] as ContentBlock.Paragraph
        assertEquals("Heading 1", heading.content)
        assertEquals(1, heading.level)
    }

    @Test
    fun `test parse heading level 2`() {
        val markdown = "## Heading 2"
        val blocks = parseMarkdownToBlocks(markdown)
        val heading = blocks[0] as ContentBlock.Paragraph
        assertEquals("Heading 2", heading.content)
        assertEquals(2, heading.level)
    }

    @Test
    fun `test parse heading level 3`() {
        val markdown = "### Heading 3"
        val blocks = parseMarkdownToBlocks(markdown)
        val heading = blocks[0] as ContentBlock.Paragraph
        assertEquals("Heading 3", heading.content)
        assertEquals(3, heading.level)
    }

    @Test
    fun `test parse unordered list`() {
        val markdown =
            """
            - Item 1
            - Item 2
            - Item 3
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(3, blocks.size)
        blocks.forEach { block ->
            assertTrue(block is ContentBlock.ListItem)
            assertEquals(false, block.ordered)
        }
        assertEquals("Item 1", (blocks[0] as ContentBlock.ListItem).content)
        assertEquals("Item 2", (blocks[1] as ContentBlock.ListItem).content)
        assertEquals("Item 3", (blocks[2] as ContentBlock.ListItem).content)
    }

    @Test
    fun `test parse ordered list`() {
        val markdown =
            """
            1. First
            2. Second
            3. Third
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(3, blocks.size)
        blocks.forEach { block ->
            assertTrue(block is ContentBlock.ListItem)
            assertEquals(true, block.ordered)
        }
        assertEquals("First", (blocks[0] as ContentBlock.ListItem).content)
    }

    @Test
    fun `test parse quote block`() {
        val markdown = "> This is a quote"
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        val quote = blocks[0] as ContentBlock.Quote
        assertEquals("This is a quote", quote.content)
    }

    @Test
    fun `test parse code block without language`() {
        val markdown =
            """
            ```
            fun hello() {
                println("Hello")
            }
            ```
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        val codeBlock = blocks[0] as ContentBlock.CodeBlock
        // 验证是代码块类型，语言可能为空
        assertTrue(codeBlock.type == BlockType.CODE)
        assertTrue(codeBlock.content.contains("fun hello()"), "Should contain function code")
    }

    @Test
    fun `test parse code block with language`() {
        val markdown =
            """
            ```kotlin
            fun main() {
                println("Hello Kotlin")
            }
            ```
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        val codeBlock = blocks[0] as ContentBlock.CodeBlock
        assertEquals("kotlin", codeBlock.language)
        assertTrue(codeBlock.content.contains("fun main()"))
    }

    @Test
    fun `test parse legacy object block`() {
        val markdown =
            """
            ```object
            {"name": "test.mp4", "contentType": "video/mp4", "url": "https://example.com/test.mp4"}
            ```
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        val objectBlock = blocks[0] as ContentBlock.ObjectBlock
        assertEquals("test.mp4", objectBlock.name)
        assertEquals("https://example.com/test.mp4", objectBlock.url)
    }

    @Test
    fun `test parse ref block (csa)`() {
        val markdown =
            """
            ```csa
            /user/123
            ```
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        val refBlock = blocks[0] as ContentBlock.RefBlock
        assertEquals("/user/123", refBlock.refPath)
    }

    @Test
    fun `test parse ref block with community path`() {
        val markdown =
            """
            ```com.storyteller_f.alphaheart
            /community/456
            ```
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        val refBlock = blocks[0] as ContentBlock.RefBlock
        assertEquals("/community/456", refBlock.refPath)
    }

    @Test
    fun `test parse math block`() {
        val markdown =
            """
            ```math
            E = mc^2
            ```
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        val mathBlock = blocks[0] as ContentBlock.MathBlock
        assertEquals("E = mc^2", mathBlock.content)
        assertEquals(false, mathBlock.inline)
    }

    @Test
    fun `test parse multiple blocks`() {
        val markdown =
            """
            # Title

            This is a paragraph.

            - List item 1
            - List item 2

            > A quote

            ```kotlin
            fun test() {}
            ```
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)

        // 验证有多种类型的块
        val types = blocks.map { it::class.simpleName }.toSet()
        assertTrue(types.contains("Paragraph"), "Should have paragraph blocks")
        assertTrue(types.contains("ListItem"), "Should have list item blocks")
        assertTrue(types.contains("Quote"), "Should have quote block")
        assertTrue(types.contains("CodeBlock"), "Should have code block")
        assertTrue(blocks.size >= 4, "Should have at least 4 blocks")
    }

    // ========== 生成测试 ==========

    @Test
    fun `test generate empty markdown from empty blocks`() {
        val blocks = emptyList<ContentBlock>()
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("", markdown)
    }

    @Test
    fun `test generate paragraph markdown`() {
        val blocks =
            listOf(
                ContentBlock.Paragraph(
                    id = "test-id",
                    content = "Hello, World!",
                    level = 0,
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("Hello, World!", markdown)
    }

    @Test
    fun `test generate heading markdown`() {
        val blocks =
            listOf(
                ContentBlock.Paragraph(
                    id = "test-id",
                    content = "Heading 1",
                    level = 1,
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("# Heading 1", markdown)
    }

    @Test
    fun `test generate heading level 2 markdown`() {
        val blocks =
            listOf(
                ContentBlock.Paragraph(
                    id = "test-id",
                    content = "Heading 2",
                    level = 2,
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("## Heading 2", markdown)
    }

    @Test
    fun `test generate unordered list markdown`() {
        val blocks =
            listOf(
                ContentBlock.ListItem(
                    id = "test-id",
                    content = "Item 1",
                    ordered = false,
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("- Item 1", markdown)
    }

    @Test
    fun `test generate ordered list markdown`() {
        val blocks =
            listOf(
                ContentBlock.ListItem(
                    id = "test-id",
                    content = "First",
                    ordered = true,
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("1. First", markdown)
    }

    @Test
    fun `test generate quote markdown`() {
        val blocks =
            listOf(
                ContentBlock.Quote(
                    id = "test-id",
                    content = "This is a quote",
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("> This is a quote", markdown)
    }

    @Test
    fun `test generate code block markdown`() {
        val blocks =
            listOf(
                ContentBlock.CodeBlock(
                    id = "test-id",
                    content = "fun hello() {}",
                    language = "kotlin",
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        // 验证包含代码块的基本结构
        assertTrue(markdown.contains("kotlin"), "Should contain language name")
        assertTrue(markdown.contains("fun hello() {}"), "Should contain code content")
        assertTrue(markdown.contains("```"), "Should contain code fences")
    }

    @Test
    fun `test generate object block markdown`() {
        val blocks =
            listOf(
                ContentBlock.ObjectBlock(
                    id = "test-id",
                    name = "video.mp4",
                    url = "https://example.com/video.mp4",
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertTrue(markdown.contains("```object"))
        assertTrue(markdown.contains("video.mp4"))
        assertTrue(!markdown.contains("contentType"))
        assertTrue(markdown.contains("https://example.com/video.mp4"))
    }

    @Test
    fun `test generate ref block markdown`() {
        val blocks =
            listOf(
                ContentBlock.RefBlock(
                    id = "test-id",
                    refPath = "/user/123",
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("```csa\n/user/123\n```", markdown)
    }

    @Test
    fun `test generate math block markdown`() {
        val blocks =
            listOf(
                ContentBlock.MathBlock(
                    id = "test-id",
                    content = "E = mc^2",
                    inline = false,
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("```math\nE = mc^2\n```", markdown)
    }

    @Test
    fun `test generate divider markdown`() {
        val blocks =
            listOf(
                ContentBlock.Divider(id = "test-id"),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("---", markdown)
    }

    // ========== 双向转换测试 ==========

    @Test
    fun `test round-trip conversion paragraph`() {
        val original = "Hello, World!"
        val blocks = parseMarkdownToBlocks(original)
        val result = generateMarkdownFromBlocks(blocks)
        assertEquals(original, result.trim())
    }

    @Test
    fun `test round-trip conversion heading`() {
        val original = "# My Title"
        val blocks = parseMarkdownToBlocks(original)
        val result = generateMarkdownFromBlocks(blocks)
        assertEquals(original, result.trim())
    }

    @Test
    fun `test round-trip conversion code block`() {
        val original = """```kotlin
fun test() = "hello"
```"""
        val blocks = parseMarkdownToBlocks(original)
        val result = generateMarkdownFromBlocks(blocks)
        // 代码块转换可能有一些格式差异，但核心内容应该保留
        assertTrue(result.contains("```kotlin") || result.contains("```"), "Should contain code fence")
        assertTrue(result.contains("fun test()"), "Should contain function definition")
    }

    @Test
    fun `test round-trip conversion multiple blocks`() {
        val original =
            """
            # Title

Some text

- Item 1
- Item 2

> Quote here
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(original)
        val result = generateMarkdownFromBlocks(blocks)

        // 验证关键内容保留
        assertTrue(result.contains("# Title"))
        assertTrue(result.contains("Some text"))
        assertTrue(result.contains("- Item 1"))
        assertTrue(result.contains("- Item 2"))
        assertTrue(result.contains("> Quote here"))
    }

    @Test
    fun `test block id generation is unique`() {
        val id1 = generateBlockId()
        val id2 = generateBlockId()
        assertTrue(id1 != id2)
        assertTrue(id1.startsWith("block_"))
        assertTrue(id2.startsWith("block_"))
    }

    @Test
    fun `test create empty paragraph block`() {
        val block = createEmptyParagraphBlock()
        assertEquals("", block.content)
        assertEquals(0, block.level)
        assertTrue(block.id.startsWith("block_"))
    }

    // ========== 边界情况测试 ==========

    @Test
    fun `invalid object falls back to a code block`() {
        val markdown =
            """
            ```object
            not valid json
            ```
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        // 无效 JSON 应该降级为 CodeBlock
        assertTrue(blocks[0] is ContentBlock.CodeBlock)
        assertEquals("object", (blocks[0] as ContentBlock.CodeBlock).language)
    }

    @Test
    fun `test parse paragraph with special characters`() {
        val markdown = "Text with # hash and > quote and * asterisk"
        val blocks = parseMarkdownToBlocks(markdown)
        assertEquals(1, blocks.size)
        val paragraph = blocks[0] as ContentBlock.Paragraph
        assertEquals("Text with # hash and > quote and * asterisk", paragraph.content)
    }

    @Test
    fun `test parse markdown with extra blank lines`() {
        val markdown =
            """
            # Title


            Some text


            - Item 1

            - Item 2
            """.trimIndent()
        val blocks = parseMarkdownToBlocks(markdown)
        // 应该忽略多余的空行
        assertTrue(blocks.size >= 3)
    }

    @Test
    fun `test generate quote with multiple lines`() {
        val blocks =
            listOf(
                ContentBlock.Quote(
                    id = "test-id",
                    content = "line 1\nline 2\nline 3",
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("> line 1\n> line 2\n> line 3", markdown)
    }

    @Test
    fun `test generate quote with empty content`() {
        val blocks =
            listOf(
                ContentBlock.Quote(
                    id = "test-id",
                    content = "",
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        // 空内容的 quote 会生成一个空行
        assertTrue(markdown.isEmpty() || markdown == "> ")
    }

    @Test
    fun `test generate image markdown with all fields`() {
        val blocks =
            listOf(
                ContentBlock.ImageBlock(
                    id = "test-id",
                    name = "photo.jpg",
                    url = "https://example.com/photo.jpg",
                    alt = "A photo",
                    title = "Photo Title",
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("![A photo](https://example.com/photo.jpg \"Photo Title\")", markdown)
    }

    @Test
    fun `test generate image markdown with minimal fields`() {
        val blocks =
            listOf(
                ContentBlock.ImageBlock(
                    id = "test-id",
                    name = "photo.jpg",
                    url = "",
                    alt = "",
                    title = "",
                ),
            )
        val markdown = generateMarkdownFromBlocks(blocks)
        assertEquals("![photo.jpg](photo.jpg)", markdown)
    }

    @Test
    fun `test parse and regenerate heading levels`() {
        for (level in 1..6) {
            val hashes = "#".repeat(level)
            val markdown = "$hashes Heading $level"
            val blocks = parseMarkdownToBlocks(markdown)
            assertEquals(1, blocks.size)
            val heading = blocks[0] as ContentBlock.Paragraph
            assertEquals(level, heading.level)
            assertEquals("Heading $level", heading.content)

            // 验证生成
            val regenerated = generateMarkdownFromBlocks(blocks)
            assertEquals(markdown, regenerated)
        }
    }
}
