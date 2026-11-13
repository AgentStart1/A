/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.no_content_yet
import com.storyteller_f.alphaheart.client.compose_core.refresh
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource

const val REFRESH_AFTER = 300L

@Composable
private fun PagingRefreshAction(enabled: Boolean, refresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = refresh,
            modifier =
            Modifier.appiumSemantics(
                text = stringResource(Res.string.refresh),
                onClick = refresh,
            ),
            enabled = enabled,
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Text(stringResource(Res.string.refresh), Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun <T : Any> ObservePagingRefresh(pagingItems: LazyPagingItems<T>, refreshEvents: Flow<Any>?) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(pagingItems, owner, refreshEvents) {
        if (refreshEvents != null) {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                pagingItems.refresh()
                refreshEvents.collect { pagingItems.refresh() }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun <T : Any> StateView(
    pagingViewModel: PagingViewModel<T>,
    modifier: Modifier = Modifier,
    showRefresh: Boolean = false,
    onRefresh: () -> Unit = {},
    refreshEvents: Flow<Any>? = null,
    content: @Composable (LazyPagingItems<T>) -> Unit,
) {
    val pagingItems = pagingViewModel.flow.collectAsLazyPagingItems()
    ObservePagingRefresh(pagingItems, refreshEvents)
    val refresh = {
        onRefresh()
        pagingItems.refresh()
    }
    var pullRefreshing by remember { mutableStateOf(false) }
    val refreshState =
        rememberPullRefreshState(refreshing = pullRefreshing, onRefresh = {
            pullRefreshing = true
            refresh()
        })
    val refreshLoadState = pagingItems.loadState.refresh
    LaunchedEffect(key1 = pullRefreshing, key2 = refreshLoadState) {
        // 增加延时，确保pagingItems真正进入刷新状态
        delay(REFRESH_AFTER)
        // 刷新结束或者当前没有内容时停止刷新，如果没有内容会使用列表刷新控件
        if (pullRefreshing && (refreshLoadState !is LoadState.Loading || pagingItems.itemCount == 0)) {
            pullRefreshing = false
        }
    }
    BoxWithConstraints(modifier = modifier.pullRefresh(refreshState)) {
        val desktopRefresh = showRefresh || maxWidth >= 600.dp
        Column(Modifier.fillMaxSize()) {
            if (desktopRefresh) {
                PagingRefreshAction(refreshLoadState !is LoadState.Loading, refresh)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (pagingItems.itemCount > 0) {
                    content(pagingItems)
                    StateViewTopIndicator(pagingItems, pullRefreshing, refresh)
                } else {
                    CenterBox {
                        when (val state = pagingItems.loadState.refresh.toLoadingState()) {
                            is LoadingState.Error ->
                                ExceptionCell(state.e) {
                                    refresh()
                                }

                            is LoadingState.Done ->
                                Text(text = stringResource(Res.string.no_content_yet))

                            else -> CircularProgressIndicator()
                        }
                    }
                }
            }
        }
        PullRefreshIndicator(pullRefreshing, refreshState, Modifier.align(Alignment.TopCenter))
    }
}

@OptIn(ExperimentalMaterialApi::class, FlowPreview::class)
@Composable
fun <T> StateView(handler: LoadingHandler<T>, modifier: Modifier = Modifier, content: @Composable (T & Any) -> Unit) {
    val state by handler.state.collectAsState()
    var pullRefreshing by remember { mutableStateOf(false) }
    val refreshState =
        rememberPullRefreshState(refreshing = pullRefreshing, onRefresh = {
            pullRefreshing = true
            handler.refresh()
        })
    LaunchedEffect(key1 = pullRefreshing, key2 = state) {
        delay(REFRESH_AFTER)
        if (pullRefreshing && state !is LoadingState.Loading) pullRefreshing = false
    }
    Box(modifier = modifier.pullRefresh(refreshState)) {
        val data by handler.data.collectAsState()
        HandlerStateViewInternal(data, handler, pullRefreshing, content)
        PullRefreshIndicator(pullRefreshing, refreshState, Modifier.align(Alignment.TopCenter))
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun <T> HandlerStateViewInternal(
    data: T?,
    handler: LoadingHandler<T>,
    pullRefreshing: Boolean,
    content: @Composable ((T & Any) -> Unit),
) {
    val state by handler.state.collectAsState()
    if (data != null) {
        content(data)
        StateViewTopIndicator(state, handler, pullRefreshing)
    } else {
        CenterBox {
            when (val capturedState = state) {
                is LoadingState.Error -> {
                    ExceptionCell(capturedState.e) {
                        handler.refresh()
                    }
                }

                else -> {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun <T> StateViewTopIndicator(state: LoadingState?, handler: LoadingHandler<T>, pullRefreshing: Boolean) {
    val newState = if (state is LoadingState.Loading || pullRefreshing) LoadingState.Done else state
    StateViewTopIndicatorInternal(newState) {
        handler.refresh()
    }
}

@Composable
private fun <T : Any> StateViewTopIndicator(
    pagingItems: LazyPagingItems<T>,
    pullRefreshing: Boolean,
    refresh: () -> Unit,
) {
    val combinedLoadStates = pagingItems.loadState
    val refreshState = combinedLoadStates.refresh.toLoadingState()
    val newState =
        if (pullRefreshing && refreshState is LoadingState.Loading) {
            LoadingState.Done
        } else {
            refreshState
        }
    StateViewTopIndicatorInternal(
        newState,
    ) {
        refresh()
    }
}

class LoadingStatePreviewProvider : PreviewParameterProvider<LoadingState> {
    override val values: Sequence<LoadingState>
        get() =
            sequenceOf(
                LoadingState.Loading,
                LoadingState.Done,
                LoadingState.Error(Exception("Preview loading failure")),
            )
}

@Preview
@Composable
private fun StateViewTopIndicatorInternal(
    @PreviewParameter(LoadingStatePreviewProvider::class) loadingState: LoadingState?,
    refresh: () -> Unit = {},
) {
    val visible = loadingState is LoadingState.Error || loadingState is LoadingState.Loading
    val shape = RoundedCornerShape(8.dp)
    AnimatedVisibility(visible) {
        Box(
            Modifier.padding(16.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            if (loadingState is LoadingState.Error) {
                ExceptionCell(loadingState.e) {
                    refresh()
                }
            } else if (loadingState is LoadingState.Loading) {
                RemoteMediatorLoadingView()
            }
        }
    }
}

@Composable
fun <T : Any> RefCellStateView(
    handler: LoadingHandler<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val data by handler.data.collectAsState()
    val state by handler.state.collectAsState()
    Box(modifier = modifier) {
        data.let { value ->
            if (value != null) {
                content(value)
            } else {
                val localState = state
                if (localState is LoadingState.Error) {
                    Box(
                        modifier =
                        Modifier.fillMaxSize().clickable {
                            // TODO show message
                        }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ExceptionView(localState.e)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

fun LazyListScope.topPrepend(combinedLoadStates: CombinedLoadStates) {
    if (combinedLoadStates.prepend == LoadState.Loading) {
        item("prepend") {
            RemoteMediatorLoadingView()
        }
    }
}

@Composable
private fun RemoteMediatorLoadingView() {
    Column(
        modifier = Modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
            text = "Waiting for items to load from the backend",
            modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally),
        )
    }
}

fun LazyGridScope.topPrepend(combinedLoadStates: CombinedLoadStates) {
    if (combinedLoadStates.prepend == LoadState.Loading) {
        item("prepend", span = {
            GridItemSpan(maxLineSpan)
        }) {
            RemoteMediatorLoadingView()
        }
    }
}

fun LazyListScope.bottomAppending(combinedLoadStates: CombinedLoadStates) {
    if (combinedLoadStates.append == LoadState.Loading) {
        item("append") {
            RemoteMediatorLoadingView()
        }
    }
}

fun LazyGridScope.bottomAppending(combinedLoadStates: CombinedLoadStates) {
    if (combinedLoadStates.append == LoadState.Loading) {
        item("append", span = {
            GridItemSpan(maxLineSpan)
        }) {
            RemoteMediatorLoadingView()
        }
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

private fun LoadState?.toLoadingState() =
    when (this) {
        null -> null
        is LoadState.Loading -> LoadingState.Loading
        is LoadState.Error -> LoadingState.Error(error)
        is LoadState.NotLoading -> LoadingState.Done
    }

@Composable
fun debounce(v: LoadState): LoadingState? {
    val debounced by produceState<LoadingState?>(null, v) {
        if (v is LoadState.NotLoading) {
            delay(500)
        }
        value = v.toLoadingState()
    }
    return debounced
}
