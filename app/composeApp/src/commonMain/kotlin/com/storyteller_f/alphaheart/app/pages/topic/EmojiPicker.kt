/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.LocalGlobalTask
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_reaction
import com.storyteller_f.alphaheart.app.clear_input
import com.storyteller_f.alphaheart.app.components.ReadingSheet
import com.storyteller_f.alphaheart.app.no_matching_emoji
import com.storyteller_f.alphaheart.app.search_emoji
import com.storyteller_f.alphaheart.app.topic.addReaction
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
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
fun EmojiPicker(showSheet: Boolean, topic: TopicDetail, hideSheet: () -> Unit) {
    if (!showSheet) return
    var query by remember {
        mutableStateOf("")
    }
    ReadingSheet(stringResource(Res.string.add_reaction), hideSheet, ReadingOverlaySize.FORM) {
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
    val searchLabel = stringResource(Res.string.search_emoji)
    OutlinedTextField(
        query,
        updateQuery,
        placeholder = { Text(searchLabel) },
        singleLine = true,
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton({ updateQuery("") }) {
                    Icon(actionIcon(ActionGlyph.X), stringResource(Res.string.clear_input))
                }
            }
        },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = searchLabel },
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
