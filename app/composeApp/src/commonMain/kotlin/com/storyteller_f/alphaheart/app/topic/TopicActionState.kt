/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.shared.model.ReactionInfo

/** Page-local presentation, independent of Home or navigation ownership. */
enum class TopicActionsPresentation(val horizontalGap: Dp, val topGap: Dp) {
    FEED(18.dp, 16.dp),
    CHAT_LIST(8.dp, 4.dp),
    DETAIL_REPLY(12.dp, 12.dp),
    DETAIL_MAIN(12.dp, 12.dp),
}

internal data class TopicActionState(
    val reactions: List<ReactionInfo>,
    val commentCount: Long,
    val hasComment: Boolean,
    val presentation: TopicActionsPresentation,
    val alignEnd: Boolean = false,
    val reactionCount: Long = 0,
)
