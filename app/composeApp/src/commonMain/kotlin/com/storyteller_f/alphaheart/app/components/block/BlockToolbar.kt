/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components.block

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.block_editor_bullet_list
import com.storyteller_f.alphaheart.app.block_editor_code
import com.storyteller_f.alphaheart.app.block_editor_divider
import com.storyteller_f.alphaheart.app.block_editor_heading
import com.storyteller_f.alphaheart.app.block_editor_image
import com.storyteller_f.alphaheart.app.block_editor_insert_media
import com.storyteller_f.alphaheart.app.block_editor_math
import com.storyteller_f.alphaheart.app.block_editor_numbered_list
import com.storyteller_f.alphaheart.app.block_editor_quote
import com.storyteller_f.alphaheart.app.block_editor_reference_csa
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.markdown.ContentBlock
import com.storyteller_f.alphaheart.client.compose_core.markdown.generateBlockId
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

/**
 * Block 工具栏
 * 提供快速插入不同类型 Block 的按钮.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockToolbar(onInsertBlock: (ContentBlock) -> Unit, modifier: Modifier = Modifier) {
    var showAddMenu by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // 文本格式按钮
        TextAddButton(onInsertBlock)

        ListAddButton(onInsertBlock)

        NumberListAddButton(onInsertBlock)

        QuotaAddButton(onInsertBlock)

        CodeBlockAddButton(onInsertBlock)

        MathAddButton(onInsertBlock)

        DividerAddButton(onInsertBlock)

        // 媒体插入菜单
        IconButton(onClick = { showAddMenu = true }) {
            Icon(
                actionIcon(ActionGlyph.IMAGE),
                stringResource(Res.string.block_editor_insert_media),
                modifier = Modifier.size(20.dp),
            )
        }

        BlockToolBarMenu(showAddMenu, onInsertBlock) {
            showAddMenu = it
        }
    }
}

@Composable
private fun DividerAddButton(onInsertBlock: (ContentBlock) -> Unit) {
    IconButton(onClick = {
        onInsertBlock(
            ContentBlock.Divider(id = generateBlockId()),
        )
    }) {
        Icon(
            actionIcon(ActionGlyph.MINUS),
            stringResource(Res.string.block_editor_divider),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun MathAddButton(onInsertBlock: (ContentBlock) -> Unit) {
    IconButton(onClick = {
        onInsertBlock(
            ContentBlock.MathBlock(
                id = generateBlockId(),
                content = "",
                inline = false,
            ),
        )
    }) {
        Icon(
            actionIcon(ActionGlyph.STICKY_NOTE_RTL),
            stringResource(Res.string.block_editor_math),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun CodeBlockAddButton(onInsertBlock: (ContentBlock) -> Unit) {
    IconButton(onClick = {
        onInsertBlock(
            ContentBlock.CodeBlock(
                id = generateBlockId(),
                content = "",
                language = "",
            ),
        )
    }) {
        Icon(
            actionIcon(ActionGlyph.CODE),
            stringResource(Res.string.block_editor_code),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun QuotaAddButton(onInsertBlock: (ContentBlock) -> Unit) {
    IconButton(onClick = {
        onInsertBlock(
            ContentBlock.Quote(
                id = generateBlockId(),
                content = "",
            ),
        )
    }) {
        Icon(
            actionIcon(ActionGlyph.QUOTE),
            stringResource(Res.string.block_editor_quote),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun NumberListAddButton(onInsertBlock: (ContentBlock) -> Unit) {
    IconButton(onClick = {
        onInsertBlock(
            ContentBlock.ListItem(
                id = generateBlockId(),
                content = "",
                ordered = true,
            ),
        )
    }) {
        Icon(
            actionIcon(ActionGlyph.LIST_ORDERED),
            stringResource(Res.string.block_editor_numbered_list),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ListAddButton(onInsertBlock: (ContentBlock) -> Unit) {
    IconButton(onClick = {
        onInsertBlock(
            ContentBlock.ListItem(
                id = generateBlockId(),
                content = "",
                ordered = false,
            ),
        )
    }) {
        Icon(
            actionIcon(ActionGlyph.LIST_RTL),
            stringResource(Res.string.block_editor_bullet_list),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun TextAddButton(onInsertBlock: (ContentBlock) -> Unit) {
    IconButton(onClick = {
        onInsertBlock(
            ContentBlock.Paragraph(
                id = generateBlockId(),
                content = "",
                level = 1,
            ),
        )
    }) {
        Icon(
            actionIcon(ActionGlyph.HEADING),
            stringResource(Res.string.block_editor_heading),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun BlockToolBarMenu(showAddMenu: Boolean, onInsertBlock: (ContentBlock) -> Unit, update: (Boolean) -> Unit) {
    DropdownMenu(
        expanded = showAddMenu,
        onDismissRequest = { update(false) },
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.block_editor_image)) },
            onClick = {
                onInsertBlock(
                    ContentBlock.ImageBlock(id = generateBlockId(), name = "", url = "", alt = ""),
                )
                update(false)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.block_editor_reference_csa)) },
            onClick = {
                onInsertBlock(
                    ContentBlock.RefBlock(
                        id = generateBlockId(),
                        refPath = "/topic/",
                    ),
                )
                update(false)
            },
        )
    }
}
