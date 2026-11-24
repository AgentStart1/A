/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.storyteller_f.alphaheart.app.common.TopicsViewModel
import com.storyteller_f.alphaheart.app.room.sameRoomMessageGroup
import com.storyteller_f.alphaheart.app.topic.addLatestRoomAnchor
import com.storyteller_f.alphaheart.app.utils.TopicReadingAnchor
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun TopicList(topicsViewModel: PagingViewModel<TopicDetail>) {
    CommonTopicList(topicsViewModel) { index, info, size ->
        TopicCell(info)
        if (index != size - 1) {
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
fun UserTopicList(topicsViewModel: PagingViewModel<TopicDetail>) {
    CommonTopicList(topicsViewModel = topicsViewModel) { index, info, size ->
        UserTopicCell(info)
        if (index != size - 1) {
            HorizontalDivider(Modifier.padding(vertical = 16.dp))
        }
    }
}

@Composable
fun CommonTopicList(
    topicsViewModel: PagingViewModel<TopicDetail>,
    block: @Composable (index: Int, info: TopicDetail?, Int) -> Unit,
) {
    val listState = rememberSaveable(topicsViewModel, saver = LazyListState.Saver) { LazyListState() }
    val readingAnchor = remember(topicsViewModel) { mutableStateOf<TopicReadingAnchor?>(null) }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        StateView(topicsViewModel, Modifier.fillMaxSize().testTag("topic-list-data-region")) { items ->
            LaunchedEffect(listState, items) {
                readingAnchor.value?.let { anchor ->
                    val row = items.itemSnapshotList.indexOfFirst { it?.id == anchor.topicId }
                    val listIndex =
                        if (row >= 0) {
                            val prependItems = if (items.loadState.prepend is LoadState.NotLoading) 0 else 1
                            row + prependItems + 1
                        } else {
                            anchor.index
                        }
                    listState.requestScrollToItem(listIndex, anchor.offset)
                }
                snapshotFlow {
                    val first = listState.firstVisibleItemIndex
                    val visible = listState.layoutInfo.visibleItemsInfo
                    if (visible.any { it.key is PrimaryKey }) {
                        val key = visible.firstOrNull { it.index == first }?.key as? PrimaryKey
                        TopicReadingAnchor(key, first, listState.firstVisibleItemScrollOffset)
                    } else {
                        null
                    }
                }.filterNotNull().collect { readingAnchor.value = it }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.widthIn(max = 740.dp).fillMaxSize().testTag("topic-list-reading-content"),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                topPrepend(items.loadState, items::retry)
                pagingItems(items, {
                    it.id
                }) {
                    block(it, items[it], items.itemCount)
                }
                bottomAppending(items.loadState, items::retry)
            }
        }
    }
}

@Composable
fun RoomTopicList(items: LazyPagingItems<TopicDetail>, topicsViewModel: TopicsViewModel, lazyListState: LazyListState) {
    StateView(topicsViewModel, Modifier.fillMaxSize().testTag("room-topic-data-region")) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.widthIn(max = 740.dp).fillMaxSize().padding(top = 10.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
            reverseLayout = true,
        ) {
            addLatestRoomAnchor()
            bottomAppending(items.loadState, items::retry)
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
                RoomTopicCell(info, info != null && !sameRoomMessageGroup(next, info))
            }
            topPrepend(items.loadState, items::retry)
        }
    }
}
