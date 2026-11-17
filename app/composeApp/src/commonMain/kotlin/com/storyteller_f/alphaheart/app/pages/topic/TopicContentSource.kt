/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import com.storyteller_f.shared.model.TopicContent

/** Feed extraction preserves the full source separately from the displayed summary. */
internal fun TopicContent.originalPlainContent(): TopicContent.Plain? =
    when (this) {
        is TopicContent.Plain -> this
        is TopicContent.Extracted -> TopicContent.Plain(origin, fileInfos)
        else -> null
    }
