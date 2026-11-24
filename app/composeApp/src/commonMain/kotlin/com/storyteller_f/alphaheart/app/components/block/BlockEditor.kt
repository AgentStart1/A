/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components.block

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.topic.editor.BlockEditorState
import com.storyteller_f.alphaheart.app.topic.editor.addBlock
import com.storyteller_f.alphaheart.app.topic.editor.changeBlockType
import com.storyteller_f.alphaheart.app.topic.editor.removeBlock
import com.storyteller_f.alphaheart.app.topic.editor.updateBlockContent
import com.storyteller_f.alphaheart.client.compose_core.markdown.ContentBlock
import com.storyteller_f.alphaheart.client.compose_core.markdown.generateMarkdownFromBlocks
import com.storyteller_f.alphaheart.client.compose_core.markdown.parseMarkdownToBlocks

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
    // Parsing is a view operation. Do not publish generated Markdown until these initial blocks actually change.
    val initialBlocks =
        remember(blocks) {
            if (blocks.isEmpty()) parseMarkdownToBlocks(initialMarkdown) else blocks.toList()
        }
    var initialized by remember(blocks) { mutableStateOf(blocks.isNotEmpty()) }
    var lastPublishedBlocks by remember(blocks) { mutableStateOf(initialBlocks) }
    LaunchedEffect(blocks) {
        if (blocks.isEmpty()) blocks.addAll(initialBlocks)
        initialized = true
    }

    var focusedBlockId by remember { mutableStateOf<String?>(null) }
    val currentBlocks = blocks.toList()
    val ready = initialized
    LaunchedEffect(currentBlocks, ready) {
        if (ready && currentBlocks != lastPublishedBlocks) {
            lastPublishedBlocks = currentBlocks
            onMarkdownChange(generateMarkdownFromBlocks(currentBlocks))
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
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
