/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_reaction
import com.storyteller_f.alphaheart.app.topic.PillAppearance
import com.storyteller_f.alphaheart.app.topic.TopicActionState
import com.storyteller_f.alphaheart.app.topic.TopicActionsPresentation
import com.storyteller_f.alphaheart.app.topic_action_comments
import com.storyteller_f.alphaheart.app.topic_action_reply_count
import com.storyteller_f.alphaheart.app.topic_action_view_replies_count
import com.storyteller_f.alphaheart.app.topic_menu
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import org.jetbrains.compose.resources.stringResource

/** Pure action/aggregate layout; callbacks retain their application-owned lifetime. */
@Composable
internal fun TopicActionsRow(
    state: TopicActionState,
    onComment: (() -> Unit)?,
    onReaction: () -> Unit,
    onMore: () -> Unit,
    onOverflow: (Int) -> Unit,
    emojiContent: @Composable (Int) -> Unit,
) {
    val appearance =
        if (state.presentation == TopicActionsPresentation.FEED) {
            PillAppearance.FEED_ACTION
        } else {
            PillAppearance.CONTEXT_ACTION
        }
    Box(Modifier.padding(top = state.presentation.topGap).testTag("topic-actions")) {
        EmojiRow(
            state.reactions,
            state.alignEnd,
            overflow = { shown ->
                if (shown < state.reactions.size) {
                    Pill(text = "+${state.reactions.size - shown}") { onOverflow(shown) }
                }
                Pill(
                    text = state.reactionCount.toString(),
                    icon = actionIcon(ActionGlyph.SMILE),
                    description = stringResource(Res.string.add_reaction),
                    appearance = appearance,
                    onClick = onReaction,
                )
                Pill(
                    icon = actionIcon(ActionGlyph.ELLIPSIS),
                    description = stringResource(Res.string.topic_menu),
                    appearance = appearance,
                    onClick = onMore,
                )
            },
            content = { index, showEmoji ->
                if (index == 0 && onComment != null) TopicCommentAction(state, appearance, onComment)
                if (showEmoji && state.reactions.isNotEmpty()) emojiContent(index)
            },
            horizontalSpacing = state.presentation.horizontalGap,
        )
    }
}

@Composable
private fun TopicCommentAction(state: TopicActionState, appearance: PillAppearance, onClick: () -> Unit) {
    val count = state.commentCount.toString()
    if (state.presentation == TopicActionsPresentation.DETAIL_MAIN) return
    val description =
        when (state.presentation) {
            TopicActionsPresentation.CHAT_LIST -> stringResource(Res.string.topic_action_reply_count, count)
            TopicActionsPresentation.DETAIL_REPLY -> stringResource(Res.string.topic_action_view_replies_count, count)
            else -> stringResource(Res.string.topic_action_comments, count)
        }
    Pill(
        count,
        icon = actionIcon(ActionGlyph.MESSAGE_SQUARE),
        selected = state.hasComment,
        highlightWithLine = true,
        description = description,
        appearance = appearance,
        onClick = onClick,
    )
}
