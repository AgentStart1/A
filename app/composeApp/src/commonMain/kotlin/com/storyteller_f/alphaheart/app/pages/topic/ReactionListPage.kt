/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.createReactionsViewModel
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.reactions_title
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReactionListPage(topicId: PrimaryKey) {
    val viewModel = createReactionsViewModel(topicId)
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.reactions_title)) }) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.TopCenter) {
            StateView(viewModel, modifier = Modifier.widthIn(max = 740.dp).fillMaxSize()) { items ->
                ReactionAggregatePagingList(items)
            }
        }
    }
}

/** Count, keys and rows belong to one immutable paging presentation, including during route disposal. */
@Composable
internal fun ReactionAggregatePagingList(items: LazyPagingItems<ReactionInfo>) {
    val snapshot = items.itemSnapshotList
    ReactionAggregateList(
        rows = snapshot,
        onItemAccess = { index ->
            // Access hints only belong to the still-current generation; stale rows remain safe to measure.
            if (items.itemSnapshotList === snapshot) {
                items[index]
            }
        },
        before = { topPrepend(items.loadState, items::retry) },
        after = { bottomAppending(items.loadState, items::retry) },
    )
}

/** Aggregate presentation only: no session, navigation, or user-list context is required. */
@Composable
internal fun ReactionAggregateList(
    rows: List<ReactionInfo?>,
    modifier: Modifier = Modifier,
    onItemAccess: (Int) -> Unit = {},
    before: LazyListScope.() -> Unit = {},
    after: LazyListScope.() -> Unit = {},
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 740.dp).fillMaxSize().testTag("reaction-aggregate-list"),
            contentPadding = PaddingValues(20.dp),
        ) {
            before()
            items(rows.size, key = { rows[it]?.emoji ?: it }) { index ->
                onItemAccess(index)
                Column {
                    val info = rows[index]
                    if (info != null) {
                        Row(
                            Modifier.fillMaxWidth().testTag("reaction-aggregate-row-$index"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                info.emoji,
                                Modifier.testTag("reaction-emoji-$index"),
                                fontSize = 25.sp,
                                lineHeight = 32.sp,
                            )
                            Text(info.count.toString(), Modifier.testTag("reaction-count-$index"))
                        }
                    } else {
                        Spacer(Modifier.height(32.dp))
                    }
                    if (index < rows.lastIndex) {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp).testTag("reaction-divider-$index"))
                    }
                }
            }
            after()
        }
    }
}
