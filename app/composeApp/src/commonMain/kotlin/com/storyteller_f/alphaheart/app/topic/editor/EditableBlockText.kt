/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic.editor

import com.storyteller_f.alphaheart.client.compose_core.markdown.ContentBlock

internal fun getBlockText(block: ContentBlock): String =
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

internal fun String.normalizeParagraphMarkdown(): String = trimEnd('\r', '\n')
