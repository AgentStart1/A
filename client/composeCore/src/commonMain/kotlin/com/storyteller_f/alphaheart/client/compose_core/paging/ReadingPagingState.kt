/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.paging

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.storyteller_f.alphaheart.client.compose_core.components.PagingBoundaryState
import com.storyteller_f.alphaheart.client.core.LoadingState

const val REFRESH_AFTER = 300L

class LoadingStatePreviewProvider : PreviewParameterProvider<LoadingState> {
    override val values: Sequence<LoadingState>
        get() =
            sequenceOf(
                LoadingState.Loading,
                LoadingState.Done,
                LoadingState.Error(Exception("Preview loading failure")),
            )
}

fun LazyListScope.topPrepend(combinedLoadStates: CombinedLoadStates, retry: (() -> Unit)? = null) {
    val state = combinedLoadStates.prepend
    if (state is LoadState.Loading || state is LoadState.Error) {
        item("prepend") { PagingBoundaryState(state, retry) }
    }
}

fun LazyGridScope.topPrepend(combinedLoadStates: CombinedLoadStates, retry: (() -> Unit)? = null) {
    val state = combinedLoadStates.prepend
    if (state is LoadState.Loading || state is LoadState.Error) {
        item("prepend", span = { GridItemSpan(maxLineSpan) }) { PagingBoundaryState(state, retry) }
    }
}

fun LazyListScope.bottomAppending(combinedLoadStates: CombinedLoadStates, retry: (() -> Unit)? = null) {
    val state = combinedLoadStates.append
    if (state is LoadState.Loading || state is LoadState.Error) {
        item("append") { PagingBoundaryState(state, retry) }
    }
}

fun LazyGridScope.bottomAppending(combinedLoadStates: CombinedLoadStates, retry: (() -> Unit)? = null) {
    val state = combinedLoadStates.append
    if (state is LoadState.Loading || state is LoadState.Error) {
        item("append", span = { GridItemSpan(maxLineSpan) }) { PagingBoundaryState(state, retry) }
    }
}

fun <T : Any> LazyListScope.pagingItems(
    lazyPagingItems: LazyPagingItems<T>,
    key: ((it: T) -> Any)? = null,
    contentType: (index: Int) -> Any? = { null },
    listId: String? = null,
    itemContent: @Composable LazyItemScope.(index: Int) -> Unit,
) {
    val k =
        if (key != null) {
            lazyPagingItems.itemKey {
                key(it)
            }
        } else {
            null
        }
    val spacerId = if (listId != null) "spacer_$listId" else "spacer_placeholder"
    item(spacerId) {
        Spacer(modifier = Modifier.height(1.dp))
    }
    items(lazyPagingItems.itemCount, k, contentType, itemContent)
}

fun <T : Any> LazyGridScope.pagingItems(
    lazyPagingItems: LazyPagingItems<T>,
    key: ((index: T) -> Any)? = null,
    span: (LazyGridItemSpanScope.(index: T) -> GridItemSpan)? = null,
    contentType: (index: T) -> Any? = { null },
    listId: String? = null,
    itemContent: @Composable LazyGridItemScope.(index: Int) -> Unit,
) {
    val k =
        if (key != null) {
            lazyPagingItems.itemKey {
                key(it)
            }
        } else {
            null
        }
    val c =
        lazyPagingItems.itemContentType {
            contentType(it)
        }
    val s: (LazyGridItemSpanScope.(Int) -> GridItemSpan)? =
        if (span != null) {
            {
                span(checkNotNull(lazyPagingItems[it]) { "Paging item at index $it is unavailable" })
            }
        } else {
            null
        }
    val spacerId = if (listId != null) "spacer_$listId" else "spacer_placeholder"
    item(spacerId, span = {
        GridItemSpan(maxLineSpan)
    }) {
        Spacer(modifier = Modifier.height(1.dp))
    }
    items(lazyPagingItems.itemCount, k, s, c, itemContent)
}

internal fun LoadState?.toLoadingState() =
    when (this) {
        null -> null
        is LoadState.Loading -> LoadingState.Loading
        is LoadState.Error -> LoadingState.Error(error)
        is LoadState.NotLoading -> LoadingState.Done
    }
