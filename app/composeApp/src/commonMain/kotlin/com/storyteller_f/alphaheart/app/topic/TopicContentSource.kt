/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.utils.extractMarkdownHeadline

internal fun TopicDetail.asFeedPreview(): TopicDetail {
    val content = content as? TopicContent.Plain ?: return this
    val preview = TopicContent.Extracted(extractMarkdownHeadline(content.plain), content.fileInfos, content.plain)
    return copy(topicInfo = topicInfo.copy(content = preview))
}

/** Feed extraction preserves the full source separately from the displayed summary. */
internal fun TopicContent.originalPlainContent(): TopicContent.Plain? =
    when (this) {
        is TopicContent.Plain -> this
        is TopicContent.Extracted -> TopicContent.Plain(origin, fileInfos)
        else -> null
    }
