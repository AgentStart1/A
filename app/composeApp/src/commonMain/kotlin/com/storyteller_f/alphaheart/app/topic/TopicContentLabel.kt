/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import com.mikepenz.markdown.model.State

internal fun topicContentLabel(plain: String, parsed: State?, blockIndex: Int?): String? {
    val block = blockIndex?.let { (parsed as? State.Success)?.node?.children?.get(it) }
    val source =
        if (block != null && parsed is State.Success) {
            parsed.content.substring(block.startOffset, block.endOffset)
        } else {
            plain
        }
    return source.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
}
