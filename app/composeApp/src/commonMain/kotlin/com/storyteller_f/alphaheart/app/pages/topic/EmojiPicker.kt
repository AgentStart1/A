/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.LocalGlobalTask
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_reaction
import com.storyteller_f.alphaheart.app.clear_input
import com.storyteller_f.alphaheart.app.common.OnAddReaction
import com.storyteller_f.alphaheart.app.common.OnRemoveReaction
import com.storyteller_f.alphaheart.app.error.ReactionActionError
import com.storyteller_f.alphaheart.app.no_matching_emoji
import com.storyteller_f.alphaheart.app.search_emoji
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalTaskContext
import com.storyteller_f.alphaheart.client.compose_core.components.NestedGlobalTask
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.addReaction
import com.storyteller_f.alphaheart.client.core.deleteReaction
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.TopicDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.kodein.emoji.Emoji
import org.kodein.emoji.list
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPicker(sheetState: SheetState, showSheet: Boolean, topic: TopicDetail, hideSheet: () -> Unit) {
    var query by remember {
        mutableStateOf("")
    }
    BaseSheet(showSheet, sheetState, hideSheet) {
        EmojiPickerInternal(query, topic, hideSheet) {
            query = it
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EmojiPickerInternal(
    query: String,
    topic: TopicDetail,
    hideSheet: () -> Unit,
    updateQuery: (String) -> Unit,
) {
    Text(
        stringResource(Res.string.add_reaction),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(end = 40.dp, bottom = 24.dp),
    )
    OutlinedTextField(
        query,
        updateQuery,
        label = { Text(stringResource(Res.string.search_emoji)) },
        placeholder = { Text(stringResource(Res.string.search_emoji)) },
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton({ updateQuery("") }) {
                    Icon(actionIcon(ActionGlyph.X), stringResource(Res.string.clear_input))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    val emojiList by produceState<List<Emoji>?>(null, query) {
        value = null
        value =
            withContext(Dispatchers.Default) {
                val normalizedQuery = query.trim()
                Emoji.list().filter { emoji ->
                    normalizedQuery.isEmpty() || emoji.details.string.contains(normalizedQuery) ||
                        emoji.details.description.contains(normalizedQuery, true)
                }
            }
    }
    Box(Modifier.fillMaxWidth().height(300.dp).padding(top = 20.dp)) {
        val results = emojiList
        if (results == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else if (results.isEmpty() && query.isNotBlank()) {
            Text(
                stringResource(Res.string.no_matching_emoji),
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyVerticalGrid(
                GridCells.Adaptive(64.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 4.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { it.details.string }) {
                    EmojiItem(topic, it, hideSheet)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EmojiItem(topic: TopicDetail, emoji: Emoji, hideSheet: () -> Unit) {
    val globalTask = LocalGlobalTask.current
    val emojiText = emoji.details.string
    Box(
        modifier =
        Modifier.fillMaxWidth().height(56.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(9.dp))
            .clickable {
                hideSheet()
                globalTask.launch("${topic.id} $emojiText") {
                    use {
                        addReaction(topic, emojiText)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(emojiText, fontSize = 25.sp)
    }
}

internal suspend fun NestedGlobalTask<GlobalTaskContext<SimpleUserSessionManager>>.addReaction(
    topic: TopicDetail,
    emojiText: String,
): CustomResult<ReactionInfo, ReactionActionError> {
    val existing =
        topic.reactions.firstOrNull {
            it.emoji == emojiText
        }
    val fakeInfo =
        existing?.copy(count = existing.count + 1) ?: ReactionInfo(
            emojiText,
            topic.id,
            1,
            true,
            Long.MAX_VALUE,
        )
    emitEvent(OnAddReaction(fakeInfo, topic))
    return request {
        addReaction(topic.id, emojiText)
    }.onSuccess {
        emitEvent(OnAddReaction(it, topic))
    }.onFailure {
        emitEvent(OnRemoveReaction(fakeInfo.copy(count = fakeInfo.count - 1), topic))
    }.mapError(ReactionActionError::fromError)
}

internal suspend fun NestedGlobalTask<GlobalTaskContext<SimpleUserSessionManager>>.deleteReaction(
    topic: TopicDetail,
    emojiText: String,
    existing: ReactionInfo,
): CustomResult<ReactionInfo, ReactionActionError> {
    val fakeInfo = existing.copy(count = existing.count - 1)
    emitEvent(OnRemoveReaction(fakeInfo, topic))
    return request { deleteReaction(emojiText, topic.id) }.onSuccess {
        emitEvent(OnRemoveReaction(it, topic))
    }.onFailure {
        emitEvent(OnRemoveReaction(existing, topic))
    }.mapError(ReactionActionError::fromError)
}
