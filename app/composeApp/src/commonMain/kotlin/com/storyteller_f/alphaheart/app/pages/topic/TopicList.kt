/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import com.storyteller_f.alphaheart.app.common.TopicsViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.shared.model.TopicDetail

@Composable
fun TopicList(topicsViewModel: PagingViewModel<TopicDetail>) {
    CommonTopicList(topicsViewModel) { index, info, size ->
        TopicCell(info)
        if (index != size - 1) {
            HorizontalDivider()
        }
    }
}

@Composable
fun UserTopicList(topicsViewModel: PagingViewModel<TopicDetail>) {
    CommonTopicList(topicsViewModel = topicsViewModel) { index, info, size ->
        UserTopicCell(info)
        if (index != size - 1) {
            HorizontalDivider()
        }
    }
}

@Composable
fun CommonTopicList(
    topicsViewModel: PagingViewModel<TopicDetail>,
    block: @Composable (index: Int, info: TopicDetail?, Int) -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        StateView(topicsViewModel, Modifier.widthIn(max = 900.dp).fillMaxSize()) { items ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                topPrepend(items.loadState)
                pagingItems(items, {
                    it.id
                }) {
                    block(it, items[it], items.itemCount)
                }
                bottomAppending(items.loadState)
            }
        }
    }
}

@Composable
fun RoomTopicList(items: LazyPagingItems<TopicDetail>, topicsViewModel: TopicsViewModel, lazyListState: LazyListState) {
    StateView(topicsViewModel) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.padding(top = 10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            reverseLayout = true,
        ) {
            addLatestRoomAnchor()
            bottomAppending(items.loadState)
            pagingItems(items, {
                it.id
            }) { index ->
                val next =
                    if (index + 1 < items.itemCount) {
                        items[index + 1]
                    } else {
                        null
                    }
                val info = items[index]
                RoomTopicCell(info, info != null && next?.author != info.author)
            }
            topPrepend(items.loadState)
        }
    }
}

/** Keeps the latest edge anchored during inserts while history retains its visible message key. */
internal fun LazyListScope.addLatestRoomAnchor() {
    item("latest-room-anchor") { Spacer(Modifier.height(1.dp)) }
}
