/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components.block

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Block 编辑器主组件
 * 类似 Notion 的编辑体验，每个块独立编辑，支持拖拽排序.
 *
 * @param modifier 修饰符
 * @param blocks 外部管理的 Block 列表（由父组件通过 [rememberBlockEditorState] 创建）
 * @param initialMarkdown 初始 Markdown 内容（仅在 blocks 为空时使用）
 * @param onMarkdownChange Markdown 内容变化回调
 */
@Composable
fun BlockEditor(
    modifier: Modifier = Modifier,
    blocks: SnapshotStateList<ContentBlock>,
    initialMarkdown: String = "",
    onMarkdownChange: (String) -> Unit,
) {
    // 如果 blocks 为空，解析初始 Markdown
    LaunchedEffect(Unit) {
        if (blocks.isEmpty()) {
            blocks.addAll(parseMarkdownToBlocks(initialMarkdown))
        }
    }

    // 当前聚焦的 Block ID
    var focusedBlockId by remember { mutableStateOf<String?>(null) }

    // 当 Blocks 变化时，生成 Markdown 并回调
    val markdown by remember {
        derivedStateOf {
            generateMarkdownFromBlocks(blocks.toList())
        }
    }

    LaunchedEffect(markdown) {
        onMarkdownChange(markdown)
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
    ) {
        items(blocks, key = { it.id }) { block ->
            EditableBlock(
                block = block,
                isFocused = block.id == focusedBlockId,
                onContentChange = { newContent ->
                    updateBlockContent(blocks, block, newContent)
                },
                onFocusChange = { focused ->
                    focusedBlockId = if (focused) block.id else null
                },
                onDelete = {
                    removeBlock(blocks, block)
                },
                onAddBlockBefore = {
                    focusedBlockId = addBlock(blocks, block)
                },
                onChangeBlockType = { newBlock ->
                    changeBlockType(blocks, block, newBlock)
                },
            )
        }

        // 底部空白区域，点击可添加新块
        item {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(100.dp))
        }
    }
}

private fun addBlock(blocks: SnapshotStateList<ContentBlock>, block: ContentBlock): String {
    val index = blocks.indexOfFirst { it.id == block.id }
    val newBlock = createEmptyParagraphBlock()
    blocks.add(index, newBlock)
    val id = newBlock.id
    return id
}

private fun changeBlockType(blocks: SnapshotStateList<ContentBlock>, block: ContentBlock, newBlock: ContentBlock) {
    val index = blocks.indexOfFirst { it.id == block.id }
    if (index >= 0) {
        blocks[index] = newBlock
    }
}

private fun removeBlock(blocks: SnapshotStateList<ContentBlock>, block: ContentBlock) {
    val index = blocks.indexOfFirst { it.id == block.id }
    if (index >= 0) {
        blocks.removeAt(index)
        if (blocks.isEmpty()) {
            blocks.add(createEmptyParagraphBlock())
        }
    }
}

private fun updateBlockContent(blocks: SnapshotStateList<ContentBlock>, block: ContentBlock, newContent: String) {
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
 * 创建并记住 BlockEditorState.
 */
@Composable
fun rememberBlockEditorState(initialMarkdown: String = ""): BlockEditorState {
    val blocks =
        remember {
            mutableStateListOf<ContentBlock>().apply {
                if (initialMarkdown.isNotBlank()) {
                    addAll(parseMarkdownToBlocks(initialMarkdown))
                }
            }
        }
    return remember { BlockEditorState(blocks) }
}

/**
 * 更新 Block 内容，保持类型不变
 * 注意：ImageBlock、ObjectBlock、RefBlock 的分支目前不会被调用，
 * 因为这些类型的 EditableBlock 没有实现 onContentChange 回调.
 */
private fun updateBlockContent(block: ContentBlock, newContent: String): ContentBlock =
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
