/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_reaction
import com.storyteller_f.alphaheart.app.topic_action_comments
import com.storyteller_f.alphaheart.app.topic_action_react
import com.storyteller_f.alphaheart.app.topic_action_reply_count
import com.storyteller_f.alphaheart.app.topic_action_view_replies
import com.storyteller_f.alphaheart.app.topic_action_view_replies_count
import com.storyteller_f.alphaheart.app.topic_menu
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.Pill
import com.storyteller_f.alphaheart.client.compose_core.components.PillAppearance
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.shared.model.ReactionInfo
import org.jetbrains.compose.resources.stringResource

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
)

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
    val showReactionText =
        state.presentation == TopicActionsPresentation.FEED ||
            state.presentation == TopicActionsPresentation.CHAT_LIST
    Box(Modifier.padding(top = state.presentation.topGap).testTag("topic-actions")) {
        EmojiRow(
            state.reactions,
            state.alignEnd,
            overflow = { shown ->
                if (shown < state.reactions.size) {
                    Pill(text = "+${state.reactions.size - shown}") { onOverflow(shown) }
                }
                Pill(
                    text = if (showReactionText) stringResource(Res.string.topic_action_react) else null,
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
    val text =
        when (state.presentation) {
            TopicActionsPresentation.FEED -> stringResource(Res.string.topic_action_comments, count)
            TopicActionsPresentation.CHAT_LIST -> count
            TopicActionsPresentation.DETAIL_REPLY -> stringResource(Res.string.topic_action_view_replies)
            TopicActionsPresentation.DETAIL_MAIN -> return
        }
    val description =
        when (state.presentation) {
            TopicActionsPresentation.CHAT_LIST -> stringResource(Res.string.topic_action_reply_count, count)
            TopicActionsPresentation.DETAIL_REPLY -> stringResource(Res.string.topic_action_view_replies_count, count)
            else -> text
        }
    Pill(
        text,
        icon =
        if (state.presentation == TopicActionsPresentation.DETAIL_REPLY) {
            null
        } else {
            actionIcon(ActionGlyph.MESSAGE_SQUARE)
        },
        selected = state.hasComment,
        description = description,
        appearance = appearance,
        onClick = onClick,
    )
}
