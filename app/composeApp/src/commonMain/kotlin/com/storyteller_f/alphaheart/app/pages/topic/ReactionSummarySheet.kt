/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.all_reactions
import com.storyteller_f.alphaheart.app.common.createReactionsViewModel
import com.storyteller_f.alphaheart.app.components.Pill
import com.storyteller_f.alphaheart.app.components.ReadingSheet
import com.storyteller_f.alphaheart.app.topic.PillAppearance
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ReactionSummarySheet(topicId: PrimaryKey, onDismiss: () -> Unit) {
    val viewModel = createReactionsViewModel(topicId)
    ReadingSheet(stringResource(Res.string.all_reactions), onDismiss, ReadingOverlaySize.REACTIONS) {
        StateView(viewModel, modifier = Modifier.fillMaxWidth()) { items ->
            ReactionSummaryList(items)
        }
    }
}

@Composable
private fun ReactionSummaryList(items: LazyPagingItems<ReactionInfo>) {
    val snapshot = items.itemSnapshotList
    val listState = rememberLazyListState()
    val endKey = "reaction-summary-end"
    LaunchedEffect(items, snapshot) {
        if (snapshot.items.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.any { it.key == endKey } }.first { it }
        if (items.itemSnapshotList === snapshot) {
            items[snapshot.placeholdersBefore + snapshot.items.lastIndex]
        }
    }
    LazyColumn(state = listState) {
        topPrepend(items.loadState, items::retry)
        item {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                snapshot.items.forEach { reaction ->
                    Pill(
                        text = reaction.count.toString(),
                        emoji = reaction.emoji,
                        selected = reaction.hasReacted,
                        appearance = PillAppearance.REACTION,
                        onClick = null,
                    )
                }
            }
        }
        item(key = endKey) { Spacer(Modifier.height(1.dp)) }
        bottomAppending(items.loadState, items::retry)
    }
}
