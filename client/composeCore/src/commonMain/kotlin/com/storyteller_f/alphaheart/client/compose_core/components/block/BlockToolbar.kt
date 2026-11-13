/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components.block

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
            Icon(Icons.Default.Image, "Insert media", modifier = Modifier.size(20.dp))
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
        Icon(Icons.Default.HorizontalRule, "Divider", modifier = Modifier.size(20.dp))
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
        Icon(Icons.AutoMirrored.Filled.Note, "Math Formula", modifier = Modifier.size(20.dp))
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
        Icon(Icons.Default.Code, "Code Block", modifier = Modifier.size(20.dp))
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
        Icon(Icons.Default.FormatQuote, "Quote", modifier = Modifier.size(20.dp))
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
        Icon(Icons.Default.FormatListNumbered, "Numbered List", modifier = Modifier.size(20.dp))
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
            Icons.AutoMirrored.Filled.FormatListBulleted,
            "Bullet List",
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
        Icon(Icons.Default.Title, "Heading", modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun BlockToolBarMenu(showAddMenu: Boolean, onInsertBlock: (ContentBlock) -> Unit, update: (Boolean) -> Unit) {
    DropdownMenu(
        expanded = showAddMenu,
        onDismissRequest = { update(false) },
    ) {
        DropdownMenuItem(
            text = { Text("Image") },
            onClick = {
                onInsertBlock(
                    ContentBlock.ImageBlock(id = generateBlockId(), name = "", url = "", alt = ""),
                )
                update(false)
            },
        )
        DropdownMenuItem(
            text = { Text("Reference (CSA)") },
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
