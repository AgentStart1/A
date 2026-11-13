/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components.block

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Block 模型的单元测试.
 */
class BlockModelTest {
    @Test
    fun `test paragraph block creation`() {
        val block =
            ContentBlock.Paragraph(
                id = "test-1",
                content = "Hello",
                level = 0,
            )
        assertEquals("test-1", block.id)
        assertEquals("Hello", block.content)
        assertEquals(0, block.level)
        assertEquals(BlockType.PARAGRAPH, block.type)
    }

    @Test
    fun `test heading block creation`() {
        val block =
            ContentBlock.Paragraph(
                id = "test-2",
                content = "Title",
                level = 1,
            )
        assertEquals(1, block.level)
        assertEquals(BlockType.PARAGRAPH, block.type)
    }

    @Test
    fun `test list item block creation`() {
        val block =
            ContentBlock.ListItem(
                id = "test-3",
                content = "List item",
                ordered = true,
                indent = 2,
            )
        assertEquals("test-3", block.id)
        assertEquals("List item", block.content)
        assertEquals(true, block.ordered)
        assertEquals(2, block.indent)
        assertEquals(BlockType.LIST_ITEM, block.type)
    }

    @Test
    fun `test quote block creation`() {
        val block =
            ContentBlock.Quote(
                id = "test-4",
                content = "To be or not to be",
            )
        assertEquals("test-4", block.id)
        assertEquals("To be or not to be", block.content)
        assertEquals(BlockType.QUOTE, block.type)
    }

    @Test
    fun `test code block creation`() {
        val block =
            ContentBlock.CodeBlock(
                id = "test-5",
                content = "print('hello')",
                language = "python",
            )
        assertEquals("test-5", block.id)
        assertEquals("print('hello')", block.content)
        assertEquals("python", block.language)
        assertEquals(BlockType.CODE, block.type)
    }

    @Test
    fun `test image block creation`() {
        val block =
            ContentBlock.ImageBlock(
                id = "test-6",
                name = "photo.jpg",
                url = "https://example.com/photo.jpg",
                alt = "A beautiful photo",
                title = "Photo Title",
            )
        assertEquals("test-6", block.id)
        assertEquals("photo.jpg", block.name)
        assertEquals("https://example.com/photo.jpg", block.url)
        assertEquals("A beautiful photo", block.alt)
        assertEquals(BlockType.IMAGE, block.type)
    }

    @Test
    fun `test object block creation`() {
        val block =
            ContentBlock.ObjectBlock(
                id = "test-7",
                name = "video.mp4",
                url = "https://example.com/video.mp4",
                cover = "cover.jpg",
                title = "My Video",
                isPlaylist = false,
            )
        assertEquals("test-7", block.id)
        assertEquals("video.mp4", block.name)
        assertEquals("https://example.com/video.mp4", block.url)
        assertEquals("cover.jpg", block.cover)
        assertEquals("My Video", block.title)
        assertEquals(false, block.isPlaylist)
        assertEquals(BlockType.OBJECT, block.type)
    }

    @Test
    fun `test ref block creation`() {
        val block =
            ContentBlock.RefBlock(
                id = "test-8",
                refPath = "/topic/123",
            )
        assertEquals("test-8", block.id)
        assertEquals("/topic/123", block.refPath)
        assertEquals(BlockType.REF, block.type)
    }

    @Test
    fun `test math block creation`() {
        val block =
            ContentBlock.MathBlock(
                id = "test-9",
                content = "\\sum_{i=1}^{n} i",
                inline = false,
            )
        assertEquals("test-9", block.id)
        assertEquals("\\sum_{i=1}^{n} i", block.content)
        assertEquals(false, block.inline)
        assertEquals(BlockType.MATH, block.type)
    }

    @Test
    fun `test inline math block creation`() {
        val block =
            ContentBlock.MathBlock(
                id = "test-10",
                content = "E = mc^2",
                inline = true,
            )
        assertEquals(true, block.inline)
        assertEquals(BlockType.MATH_INLINE, block.type)
    }

    @Test
    fun `test divider block creation`() {
        val block =
            ContentBlock.Divider(
                id = "test-11",
            )
        assertEquals("test-11", block.id)
        assertEquals(BlockType.DIVIDER, block.type)
    }

    @Test
    fun `test block type enum values`() {
        val expectedTypes =
            listOf(
                BlockType.PARAGRAPH,
                BlockType.LIST_ITEM,
                BlockType.QUOTE,
                BlockType.CODE,
                BlockType.IMAGE,
                BlockType.OBJECT,
                BlockType.REF,
                BlockType.MATH,
                BlockType.MATH_INLINE,
                BlockType.DIVIDER,
            )
        assertEquals(10, BlockType.values().size)
        expectedTypes.forEach { type ->
            assertTrue(BlockType.values().contains(type))
        }
    }

    @Test
    fun `test paragraph block copy`() {
        val original =
            ContentBlock.Paragraph(
                id = "test-12",
                content = "Original",
                level = 0,
            )
        val copied = original.copy(content = "Modified")
        assertEquals("test-12", copied.id)
        assertEquals("Modified", copied.content)
        assertEquals(0, copied.level)
    }

    @Test
    fun `test code block copy`() {
        val original =
            ContentBlock.CodeBlock(
                id = "test-13",
                content = "old code",
                language = "kotlin",
            )
        val copied = original.copy(content = "new code", language = "java")
        assertEquals("test-13", copied.id)
        assertEquals("new code", copied.content)
        assertEquals("java", copied.language)
    }
}
