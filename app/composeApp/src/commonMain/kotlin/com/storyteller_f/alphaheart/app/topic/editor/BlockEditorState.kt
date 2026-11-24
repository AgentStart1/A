/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic.editor

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.storyteller_f.alphaheart.client.compose_core.markdown.ContentBlock
import com.storyteller_f.alphaheart.client.compose_core.markdown.createEmptyParagraphBlock

internal fun addBlock(blocks: SnapshotStateList<ContentBlock>, block: ContentBlock): String {
    val index = blocks.indexOfFirst { it.id == block.id }
    val newBlock = createEmptyParagraphBlock()
    blocks.add(index, newBlock)
    val id = newBlock.id
    return id
}

internal fun changeBlockType(blocks: SnapshotStateList<ContentBlock>, block: ContentBlock, newBlock: ContentBlock) {
    val index = blocks.indexOfFirst { it.id == block.id }
    if (index >= 0) {
        blocks[index] = newBlock
    }
}

internal fun removeBlock(blocks: SnapshotStateList<ContentBlock>, block: ContentBlock) {
    val index = blocks.indexOfFirst { it.id == block.id }
    if (index >= 0) {
        blocks.removeAt(index)
        if (blocks.isEmpty()) {
            blocks.add(createEmptyParagraphBlock())
        }
    }
}

internal fun updateBlockContent(blocks: SnapshotStateList<ContentBlock>, block: ContentBlock, newContent: String) {
    val index = blocks.indexOfFirst { it.id == block.id }
    if (index >= 0) {
        blocks[index] = updateBlockContent(blocks[index], newContent)
    }
}

/**
 * Block 编辑器状态类
 * 用于在父组件中管理 Block 列表，以便与 BlockToolbar 共享.
 */
class BlockEditorState(val blocks: SnapshotStateList<ContentBlock>) {
    /**
     * 在指定位置插入一个 Block.
     */
    fun insertBlock(index: Int, block: ContentBlock) {
        blocks.add(index.coerceIn(0, blocks.size), block)
    }

    /**
     * 在末尾添加一个 Block.
     */
    fun appendBlock(block: ContentBlock) {
        if (blocks.singleOrNull()?.isEmptyParagraph() == true) {
            blocks[0] = block
        } else {
            blocks.add(block)
        }
    }

    /**
     * 替换指定 ID 的 Block.
     */
    fun replaceBlock(blockId: String, newBlock: ContentBlock) {
        val index = blocks.indexOfFirst { it.id == blockId }
        if (index >= 0) {
            blocks[index] = newBlock
        }
    }
}

private fun ContentBlock.isEmptyParagraph(): Boolean = this is ContentBlock.Paragraph && content.isEmpty() && level == 0

/**
 * 更新 Block 内容，保持类型不变
 * 注意：ImageBlock、ObjectBlock、RefBlock 的分支目前不会被调用，
 * 因为这些类型的 EditableBlock 没有实现 onContentChange 回调.
 */
internal fun updateBlockContent(block: ContentBlock, newContent: String): ContentBlock =
    when (block) {
        is ContentBlock.Paragraph -> block.copy(content = newContent)

        is ContentBlock.ListItem -> block.copy(content = newContent)

        is ContentBlock.Quote -> block.copy(content = newContent)

        is ContentBlock.CodeBlock -> block.copy(content = newContent)

        // 以下三种类型需要专门的编辑界面，目前不会执行这些分支。
        is ContentBlock.ImageBlock -> block.copy(alt = newContent)

        is ContentBlock.ObjectBlock -> block.copy(title = newContent)

        is ContentBlock.RefBlock -> block.copy(refPath = newContent)

        is ContentBlock.MathBlock -> block.copy(content = newContent)

        is ContentBlock.Divider -> block
    }
