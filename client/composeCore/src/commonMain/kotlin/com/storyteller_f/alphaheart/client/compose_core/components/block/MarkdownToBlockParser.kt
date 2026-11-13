/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components.block

import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.utils.MarkdownObject
import com.storyteller_f.shared.utils.astNode
import com.storyteller_f.shared.utils.getLang
import com.storyteller_f.shared.utils.readCodeFence
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes

/**
 * 将 Markdown 文本解析为 ContentBlock 列表.
 */
fun parseMarkdownToBlocks(markdown: String): List<ContentBlock> {
    if (markdown.isBlank()) {
        return listOf(createEmptyParagraphBlock())
    }

    val ast = astNode(markdown)
    val blocks = mutableListOf<ContentBlock>()

    parseAstNode(ast, markdown, blocks)

    return if (blocks.isEmpty()) {
        listOf(createEmptyParagraphBlock())
    } else {
        blocks
    }
}

private fun parseAstNode(node: ASTNode, content: String, blocks: MutableList<ContentBlock>) {
    when (node.type) {
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6,
        -> {
            blocks.add(parseHeader(node, content))
        }

        MarkdownElementTypes.PARAGRAPH -> {
            val text = node.getTextInNode(content).trim().toString()
            if (text.isNotEmpty()) {
                blocks.add(
                    ContentBlock.Paragraph(
                        id = generateBlockId(),
                        content = text,
                    ),
                )
            }
        }

        MarkdownElementTypes.UNORDERED_LIST,
        MarkdownElementTypes.ORDERED_LIST,
        -> {
            parseList(node, content, blocks)
        }

        MarkdownElementTypes.BLOCK_QUOTE -> {
            blocks.add(parseQuote(node, content))
        }

        MarkdownElementTypes.CODE_FENCE -> {
            blocks.add(parseCodeFence(node, content))
        }

        MarkdownElementTypes.CODE_BLOCK -> {
            blocks.add(
                ContentBlock.CodeBlock(
                    id = generateBlockId(),
                    content = node.getTextInNode(content).trim().toString(),
                    language = "",
                ),
            )
        }

        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            blocks.add(ContentBlock.Divider(id = generateBlockId()))
        }

        GFMElementTypes.BLOCK_MATH -> {
            blocks.add(parseMathBlock(node, content, inline = false))
        }

        else -> {
            // 递归处理子节点
            node.children.forEach { child ->
                parseAstNode(child, content, blocks)
            }
        }
    }
}

private fun parseHeader(node: ASTNode, content: String): ContentBlock.Paragraph {
    val text = node.getTextInNode(content).trim().toString()
    // 移除开头的 # 符号
    val contentText = text.replace(Regex("^#+\\s*"), "")
    val level =
        when (node.type) {
            MarkdownElementTypes.ATX_1 -> 1
            MarkdownElementTypes.ATX_2 -> 2
            MarkdownElementTypes.ATX_3 -> 3
            MarkdownElementTypes.ATX_4 -> 4
            MarkdownElementTypes.ATX_5 -> 5
            MarkdownElementTypes.ATX_6 -> 6
            else -> 0
        }
    return ContentBlock.Paragraph(
        id = generateBlockId(),
        content = contentText,
        level = level,
    )
}

private fun parseList(node: ASTNode, content: String, blocks: MutableList<ContentBlock>) {
    val ordered = node.type == MarkdownElementTypes.ORDERED_LIST

    for (child in node.children) {
        if (child.type == MarkdownElementTypes.LIST_ITEM) {
            val itemText = extractListItemText(child, content)
            blocks.add(
                ContentBlock.ListItem(
                    id = generateBlockId(),
                    content = itemText,
                    ordered = ordered,
                    indent = 0,
                ),
            )
        }
    }
}

private fun extractListItemText(listItemNode: ASTNode, content: String): String {
    // 查找实际的文本内容，跳过列表标记
    val children = listItemNode.children
    // 第一个子节点通常是列表标记，跳过它
    val textNodes =
        children.filter {
            it.type != MarkdownTokenTypes.LIST_BULLET &&
                it.type != MarkdownTokenTypes.LIST_NUMBER &&
                it.type != MarkdownTokenTypes.WHITE_SPACE
        }
    return textNodes.joinToString("") { it.getTextInNode(content).toString() }.trim()
}

private fun parseQuote(node: ASTNode, content: String): ContentBlock.Quote {
    val text = node.getTextInNode(content).trim().toString()
    // 使用多行模式移除所有行首的 > 符号
    val contentText = text.replace(Regex("(?m)^>\\s*"), "")
    return ContentBlock.Quote(
        id = generateBlockId(),
        content = contentText,
    )
}

private fun parseCodeFence(node: ASTNode, content: String): ContentBlock {
    // getLang 在没有语言时可能返回 "null" 字符串，需要处理
    val lang = getLang(node, content).lowercase().takeIf { it.isNotBlank() && it != "null" }.orEmpty()
    val codeContent = readCodeFence(node, content).trim()

    return when {
        lang == "object" -> parseObjectBlock(codeContent)

        lang in listOf("com.storyteller_f.alphaheart", "c.s.a", "csa") -> {
            ContentBlock.RefBlock(
                id = generateBlockId(),
                refPath = codeContent.trim(),
            )
        }

        lang == "math" -> {
            ContentBlock.MathBlock(
                id = generateBlockId(),
                content = codeContent,
                inline = false,
            )
        }

        else -> {
            ContentBlock.CodeBlock(
                id = generateBlockId(),
                content = codeContent,
                language = lang,
            )
        }
    }
}

private fun parseObjectBlock(jsonContent: String): ContentBlock =
    try {
        val obj = commonJson.decodeFromString<MarkdownObject>(jsonContent)
        ContentBlock.ObjectBlock(
            id = generateBlockId(),
            name = obj.name,
            url = obj.url,
            cover = obj.cover,
            title = obj.title,
            isPlaylist = obj.isPlayList,
        )
    } catch (_: Exception) {
        // JSON 解析失败，降级为代码块
        ContentBlock.CodeBlock(
            id = generateBlockId(),
            content = jsonContent,
            language = "object",
        )
    }

private fun parseMathBlock(node: ASTNode, content: String, inline: Boolean): ContentBlock.MathBlock {
    val text = node.getTextInNode(content).trim().toString()
    // 移除 $ 符号
    val mathContent = text.replace(Regex("^\\$+|\\$+$"), "")
    return ContentBlock.MathBlock(
        id = generateBlockId(),
        content = mathContent,
        inline = inline,
    )
}
