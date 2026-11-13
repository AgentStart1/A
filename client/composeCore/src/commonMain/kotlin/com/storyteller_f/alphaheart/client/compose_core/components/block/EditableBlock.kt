/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.storyteller_f.alphaheart.client.compose_core.components.LocalTopicContentFonts

/**
 * 可编辑的 Block 组件
 * 每个 Block 在聚焦时显示为编辑模式，失焦时显示为渲染后的预览.
 */
@Composable
fun EditableBlock(
    block: ContentBlock,
    isFocused: Boolean,
    onContentChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onAddBlockBefore: () -> Unit,
    onChangeBlockType: (ContentBlock) -> Unit,
) {
    var showTypeMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            // Block 操作行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 添加按钮
                IconButton(
                    onClick = { onAddBlockBefore() },
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(Icons.Default.Add, "Add block before", modifier = Modifier.size(16.dp))
                }

                // 拖拽图标
                Icon(
                    Icons.Default.DragIndicator,
                    "Drag to reorder",
                    modifier = Modifier.size(20.dp),
                )

                // 编辑/预览区域
                BlockPreview(block, isFocused, onContentChange, onFocusChange)

                // 删除按钮
                IconButton(onClick = { showTypeMenu = true }) {
                    Icon(Icons.Default.Delete, "Delete or change type", modifier = Modifier.size(16.dp))
                }

                BlockMenu(showTypeMenu, onDelete, onChangeBlockType, block) {
                    showTypeMenu = it
                }
            }
        }
    }
}

@Composable
private fun BlockMenu(
    showTypeMenu: Boolean,
    onDelete: () -> Unit,
    onChangeBlockType: (ContentBlock) -> Unit,
    block: ContentBlock,
    update: (Boolean) -> Unit,
) {
    DropdownMenu(
        expanded = showTypeMenu,
        onDismissRequest = { update(false) },
    ) {
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDelete()
                update(false)
            },
        )
        DropdownMenuItem(
            text = { Text("To Paragraph") },
            onClick = {
                onChangeBlockType(
                    ContentBlock.Paragraph(id = block.id, content = getBlockText(block)),
                )
                update(false)
            },
        )
        DropdownMenuItem(
            text = { Text("To Heading 1") },
            onClick = {
                onChangeBlockType(
                    ContentBlock.Paragraph(id = block.id, content = getBlockText(block), level = 1),
                )
                update(false)
            },
        )
        DropdownMenuItem(
            text = { Text("To Heading 2") },
            onClick = {
                onChangeBlockType(
                    ContentBlock.Paragraph(id = block.id, content = getBlockText(block), level = 2),
                )
                update(false)
            },
        )
        DropdownMenuItem(
            text = { Text("To Code Block") },
            onClick = {
                onChangeBlockType(
                    ContentBlock.CodeBlock(id = block.id, content = getBlockText(block)),
                )
                update(false)
            },
        )
    }
}

@Composable
private fun RowScope.BlockPreview(
    block: ContentBlock,
    isFocused: Boolean,
    onContentChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val style = LocalTextStyle.current.merge(fontFamily = LocalTopicContentFonts.current.content)
    ProvideTextStyle(style) {
        Box(modifier = Modifier.weight(1f)) {
            when (block) {
                is ContentBlock.Paragraph ->
                    EditableParagraphBlock(
                        block = block,
                        isFocused = isFocused,
                        onContentChange = onContentChange,
                        onFocusChange = onFocusChange,
                    )

                is ContentBlock.ListItem ->
                    EditableListItemBlock(
                        block = block,
                        isFocused = isFocused,
                        onContentChange = onContentChange,
                        onFocusChange = onFocusChange,
                    )

                is ContentBlock.Quote ->
                    EditableQuoteBlock(
                        block = block,
                        isFocused = isFocused,
                        onContentChange = onContentChange,
                        onFocusChange = onFocusChange,
                    )

                is ContentBlock.CodeBlock ->
                    EditableCodeBlock(
                        block = block,
                        isFocused = isFocused,
                        onContentChange = onContentChange,
                        onFocusChange = onFocusChange,
                    )

                is ContentBlock.ImageBlock ->
                    EditableImageBlock(
                        block = block,
                        isFocused = isFocused,
                    )

                is ContentBlock.ObjectBlock ->
                    EditableObjectBlock(
                        block = block,
                        isFocused = isFocused,
                    )

                is ContentBlock.RefBlock ->
                    EditableRefBlock(
                        block = block,
                    )

                is ContentBlock.MathBlock ->
                    EditableMathBlock(
                        block = block,
                        isFocused = isFocused,
                        onContentChange = onContentChange,
                        onFocusChange = onFocusChange,
                    )

                is ContentBlock.Divider -> DividerBlock()
            }
        }
    }
}

private fun getBlockText(block: ContentBlock): String =
    when (block) {
        is ContentBlock.Paragraph -> block.content
        is ContentBlock.ListItem -> block.content
        is ContentBlock.Quote -> block.content
        is ContentBlock.CodeBlock -> block.content
        is ContentBlock.ImageBlock -> block.alt
        is ContentBlock.ObjectBlock -> block.title ?: block.name
        is ContentBlock.RefBlock -> block.refPath
        is ContentBlock.MathBlock -> block.content
        is ContentBlock.Divider -> ""
    }

/**
 * 可编辑的段落块.
 */
@Composable
fun EditableParagraphBlock(
    block: ContentBlock.Paragraph,
    isFocused: Boolean,
    onContentChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val textStyle =
        when (block.level) {
            1 -> MaterialTheme.typography.headlineLarge
            2 -> MaterialTheme.typography.headlineMedium
            3 -> MaterialTheme.typography.headlineSmall
            4 -> MaterialTheme.typography.titleLarge
            5 -> MaterialTheme.typography.titleMedium
            6 -> MaterialTheme.typography.titleSmall
            else -> MaterialTheme.typography.bodyLarge
        }.merge(fontFamily = LocalTopicContentFonts.current.content)

    if (isFocused) {
        RichEditableParagraphBlock(block, textStyle, onContentChange)
    } else {
        Text(
            text = block.content,
            style = textStyle,
            modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onFocusChange(true) }
                .padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun RichEditableParagraphBlock(
    block: ContentBlock.Paragraph,
    textStyle: androidx.compose.ui.text.TextStyle,
    onContentChange: (String) -> Unit,
) {
    val state = rememberRichTextState()

    LaunchedEffect(block.id) {
        state.setMarkdown(block.content)
    }
    LaunchedEffect(block.content) {
        val markdown = state.toMarkdown().normalizeParagraphMarkdown()
        if (markdown != block.content) {
            state.setMarkdown(block.content)
        }
    }
    LaunchedEffect(state.annotatedString) {
        val markdown = state.toMarkdown().normalizeParagraphMarkdown()
        if (markdown != block.content) {
            onContentChange(markdown)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        ParagraphRichTextToolbar(state)
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            if (block.content.isEmpty()) {
                Text(
                    "Type '/' for commands...",
                    style = textStyle.copy(color = LocalContentColor.current.copy(alpha = 0.5f)),
                )
            }
            BasicRichTextEditor(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                textStyle = textStyle.copy(color = LocalContentColor.current),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ParagraphRichTextToolbar(state: RichTextState) {
    val currentSpanStyle = state.currentSpanStyle
    FlowRow {
        IconToggleButton(currentSpanStyle.fontWeight == FontWeight.Bold, {
            state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        }) {
            Icon(Icons.Default.FormatBold, "Toggle bold")
        }
        IconToggleButton(currentSpanStyle.fontStyle == FontStyle.Italic, {
            state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        }) {
            Icon(Icons.Default.FormatItalic, "Toggle italic")
        }
        IconToggleButton(currentSpanStyle.textDecoration == TextDecoration.Underline, {
            state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
        }) {
            Icon(Icons.Default.FormatUnderlined, "Toggle underline")
        }
        IconToggleButton(currentSpanStyle.textDecoration == TextDecoration.LineThrough, {
            state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        }) {
            Icon(Icons.Default.FormatStrikethrough, "Toggle strikethrough")
        }
        VerticalDivider(modifier = Modifier.height(20.dp).align(Alignment.CenterVertically))
        IconToggleButton(state.isOrderedList, {
            state.toggleOrderedList()
        }) {
            Icon(Icons.Default.FormatListNumbered, "Toggle ordered list")
        }
        IconToggleButton(state.isUnorderedList, {
            state.toggleUnorderedList()
        }) {
            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "Toggle unordered list")
        }
        VerticalDivider(modifier = Modifier.height(20.dp).align(Alignment.CenterVertically))
        IconToggleButton(state.isCodeSpan, {
            state.toggleCodeSpan()
        }) {
            Icon(Icons.Default.Code, "Toggle code span")
        }
    }
}

private fun String.normalizeParagraphMarkdown(): String = trimEnd('\r', '\n')

/**
 * 可编辑的列表项.
 */
@Composable
fun EditableListItemBlock(
    block: ContentBlock.ListItem,
    isFocused: Boolean,
    onContentChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = if (block.ordered) "1." else "•",
            modifier = Modifier.padding(end = 8.dp),
        )

        if (isFocused) {
            BasicTextField(
                value = block.content,
                onValueChange = onContentChange,
                modifier = Modifier.weight(1f),
                textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
                decorationBox = { innerTextField ->
                    if (block.content.isEmpty()) {
                        Text(
                            "List item...",
                            style = LocalTextStyle.current.copy(color = LocalContentColor.current.copy(alpha = 0.5f)),
                        )
                    }
                    innerTextField()
                },
            )
        } else {
            Text(
                text = block.content,
                modifier = Modifier.weight(1f).clickable { onFocusChange(true) },
            )
        }
    }
}

/**
 * 可编辑的引用块.
 */
@Composable
fun EditableQuoteBlock(
    block: ContentBlock.Quote,
    isFocused: Boolean,
    onContentChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .border(
                width = 3.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (isFocused) {
            BasicTextField(
                value = block.content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle =
                LocalTextStyle.current.copy(
                    color = LocalContentColor.current,
                    fontStyle = FontStyle.Italic,
                ),
                decorationBox = { innerTextField ->
                    if (block.content.isEmpty()) {
                        Text(
                            "Quote...",
                            style = LocalTextStyle.current.copy(color = LocalContentColor.current.copy(alpha = 0.5f)),
                        )
                    }
                    innerTextField()
                },
            )
        } else {
            Text(
                text = block.content,
                style =
                LocalTextStyle.current.copy(
                    fontStyle = FontStyle.Italic,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                ),
                modifier = Modifier.clickable { onFocusChange(true) },
            )
        }
    }
}

/**
 * 可编辑的代码块.
 */
@Composable
fun EditableCodeBlock(
    block: ContentBlock.CodeBlock,
    isFocused: Boolean,
    onContentChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small,
            )
            .padding(12.dp),
    ) {
        // 语言显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = block.language.ifBlank { "plain text" },
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.6f),
            )
        }

        if (isFocused) {
            BasicTextField(
                value = block.content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textStyle =
                LocalTextStyle.current.copy(
                    fontFamily = LocalTopicContentFonts.current.code ?: FontFamily.Monospace,
                    color = LocalContentColor.current,
                ),
                decorationBox = { innerTextField ->
                    if (block.content.isEmpty()) {
                        Text(
                            "Enter code...",
                            style = LocalTextStyle.current.copy(color = LocalContentColor.current.copy(alpha = 0.5f)),
                        )
                    }
                    innerTextField()
                },
            )
        } else {
            Text(
                text = block.content,
                style =
                LocalTextStyle.current.copy(
                    fontFamily = LocalTopicContentFonts.current.code ?: FontFamily.Monospace,
                    color = LocalContentColor.current,
                ),
                modifier = Modifier.clickable { onFocusChange(true) },
            )
        }
    }
}

/**
 * 可编辑的图片块.
 */
@Composable
fun EditableImageBlock(block: ContentBlock.ImageBlock, isFocused: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = "Image: ${block.name.ifBlank { "No image selected" }}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (isFocused) {
            Text(
                text = "Click to select image (TODO: integrate with FilePicker)",
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.5f),
            )
        }
    }
}

/**
 * 可编辑的对象嵌入块.
 */
@Composable
fun EditableObjectBlock(block: ContentBlock.ObjectBlock, isFocused: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = block.title ?: block.name.ifBlank { "Embedded Object" },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (isFocused) {
            Text(
                text = "Click to edit object (TODO: integrate with media picker)",
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.5f),
            )
        }
    }
}

/**
 * 可编辑的引用块 (CSA).
 */
@Composable
fun EditableRefBlock(block: ContentBlock.RefBlock) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "Reference: ${block.refPath}",
            style = MaterialTheme.typography.bodyMedium,
            modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small,
                )
                .padding(8.dp),
        )
    }
}

/**
 * 可编辑的数学公式块.
 */
@Composable
fun EditableMathBlock(
    block: ContentBlock.MathBlock,
    isFocused: Boolean,
    onContentChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(12.dp),
    ) {
        Text(
            text = "Math Formula",
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.6f),
        )

        if (isFocused) {
            BasicTextField(
                value = block.content,
                onValueChange = onContentChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textStyle =
                LocalTextStyle.current.copy(
                    fontFamily = LocalTopicContentFonts.current.code ?: FontFamily.Monospace,
                    color = LocalContentColor.current,
                ),
                decorationBox = { innerTextField ->
                    if (block.content.isEmpty()) {
                        Text(
                            "Enter LaTeX...",
                            style = LocalTextStyle.current.copy(color = LocalContentColor.current.copy(alpha = 0.5f)),
                        )
                    }
                    innerTextField()
                },
            )
        } else {
            Text(
                text = block.content,
                style =
                LocalTextStyle.current.copy(
                    fontFamily = LocalTopicContentFonts.current.code ?: FontFamily.Monospace,
                    color = LocalContentColor.current,
                ),
                modifier = Modifier.clickable { onFocusChange(true) },
            )
        }
    }
}

/**
 * 分割线块.
 */
@Composable
fun DividerBlock() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        thickness = 1.dp,
        color = LocalContentColor.current.copy(alpha = 0.2f),
    )
}
