/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components.block

import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.utils.MarkdownObject

/**
 * 将 ContentBlock 列表转换为 Markdown 文本.
 */
fun generateMarkdownFromBlocks(blocks: List<ContentBlock>): String {
    if (blocks.isEmpty()) {
        return ""
    }

    return blocks.joinToString("\n\n") { block ->
        when (block) {
            is ContentBlock.Paragraph -> generateParagraphMarkdown(block)
            is ContentBlock.ListItem -> generateListItemMarkdown(block)
            is ContentBlock.Quote -> generateQuoteMarkdown(block)
            is ContentBlock.CodeBlock -> generateCodeBlockMarkdown(block)
            is ContentBlock.ImageBlock -> generateImageMarkdown(block)
            is ContentBlock.ObjectBlock -> generateObjectBlockMarkdown(block)
            is ContentBlock.RefBlock -> generateRefBlockMarkdown(block)
            is ContentBlock.MathBlock -> generateMathBlockMarkdown(block)
            is ContentBlock.Divider -> "---"
        }
    }
}

private fun generateParagraphMarkdown(block: ContentBlock.Paragraph): String =
    if (block.level > 0) {
        "${"#".repeat(block.level)} ${block.content}"
    } else {
        block.content
    }

private fun generateListItemMarkdown(block: ContentBlock.ListItem): String {
    val prefix = if (block.ordered) "1." else "-"
    val indent = "  ".repeat(block.indent)
    return "$indent$prefix ${block.content}"
}

private fun generateQuoteMarkdown(block: ContentBlock.Quote): String =
    block.content.lines().joinToString("\n") {
        "> $it"
    }

private fun generateCodeBlockMarkdown(block: ContentBlock.CodeBlock): String {
    val lang = if (block.language.isNotBlank()) " ${block.language}" else ""
    return "```$lang\n${block.content}\n```"
}

private fun generateImageMarkdown(block: ContentBlock.ImageBlock): String {
    val alt = block.alt.ifBlank { block.name }
    val title = if (block.title.isNotBlank()) " \"${block.title}\"" else ""
    val url = block.url.ifBlank { block.name }
    return "![$alt]($url$title)"
}

private fun generateObjectBlockMarkdown(block: ContentBlock.ObjectBlock): String {
    val obj =
        MarkdownObject(
            name = block.name,
            url = block.url,
            isPlayList = block.isPlaylist,
            cover = block.cover,
            title = block.title,
        )
    val json = commonJson.encodeToString(obj)
    return "```object\n$json\n```"
}

private fun generateRefBlockMarkdown(block: ContentBlock.RefBlock): String = "```csa\n${block.refPath}\n```"

private fun generateMathBlockMarkdown(block: ContentBlock.MathBlock): String =
    if (block.inline) {
        "$${block.content}$"
    } else {
        "```math\n${block.content}\n```"
    }
