/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.load_more_failed_retry
import com.storyteller_f.alphaheart.client.compose_core.loading_more
import com.storyteller_f.alphaheart.client.compose_core.no_content_yet
import com.storyteller_f.alphaheart.client.compose_core.paging.LoadingStatePreviewProvider
import com.storyteller_f.alphaheart.client.compose_core.paging.REFRESH_AFTER
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.toLoadingState
import com.storyteller_f.alphaheart.client.compose_core.refresh
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

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
            Icon(actionIcon(ActionGlyph.REFRESH_CW), contentDescription = null)
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
    header: @Composable () -> Unit = {},
    emptyText: String? = null,
    loadingText: String? = null,
    failureText: String? = null,
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
        Column(Modifier.fillMaxSize()) {
            header()
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                if (pagingItems.itemCount > 0) {
                    content(pagingItems)
                    StateViewTopIndicator(pagingItems, pullRefreshing, refresh)
                } else {
                    CenterBox {
                        when (val state = pagingItems.loadState.refresh.toLoadingState()) {
                            is LoadingState.Error ->
                                ExceptionCell(state.e, failureText) {
                                    refresh()
                                }

                            is LoadingState.Done ->
                                Text(text = emptyText ?: stringResource(Res.string.no_content_yet))

                            else -> ReadingLoadingState(loadingText)
                        }
                    }
                }
            }
        }
        if (showRefresh) {
            Box(Modifier.align(Alignment.TopEnd)) {
                PagingRefreshAction(refreshLoadState !is LoadState.Loading, refresh)
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
    Box(modifier = modifier.pullRefresh(refreshState), contentAlignment = Alignment.TopCenter) {
        val data by handler.data.collectAsState()
        HandlerStateViewInternal(data, handler, pullRefreshing, content)
        PullRefreshIndicator(pullRefreshing, refreshState, Modifier.align(Alignment.TopCenter))
    }
}

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun <T> BoxScope.HandlerStateViewInternal(
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
                    ReadingLoadingState()
                }
            }
        }
    }
}

@Composable
private fun <T> BoxScope.StateViewTopIndicator(
    state: LoadingState?,
    handler: LoadingHandler<T>,
    pullRefreshing: Boolean,
) {
    val newState = if (pullRefreshing) LoadingState.Done else state
    CachedRefreshOverlay(newState, handler::refresh)
}

@Composable
private fun <T : Any> BoxScope.StateViewTopIndicator(
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
    CachedRefreshOverlay(newState, refresh)
}

@Composable
fun BoxScope.CachedRefreshOverlay(state: LoadingState?, refresh: () -> Unit) {
    // A refresh status must not determine the size of cached, wrap-content data.
    Box(Modifier.matchParentSize().zIndex(1f), contentAlignment = Alignment.TopCenter) {
        CachedRefreshIndicator(state, refresh)
    }
}

@Preview
@Composable
fun CachedRefreshIndicator(
    @PreviewParameter(LoadingStatePreviewProvider::class) loadingState: LoadingState?,
    refresh: () -> Unit = {},
) {
    Box(
        Modifier.fillMaxWidth().zIndex(1f).testTag("cached-refresh-indicator"),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (loadingState) {
            is LoadingState.Loading ->
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    gapSize = 0.dp,
                )

            is LoadingState.Error -> {
                ReadingRefreshFailure(refresh)
            }

            else -> Unit
        }
    }
}

@Composable
internal fun PagingBoundaryState(state: LoadState, retry: (() -> Unit)?) {
    Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
        when (state) {
            is LoadState.Loading ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.loading_more))
                }

            is LoadState.Error ->
                TextButton({ retry?.invoke() }, enabled = retry != null) {
                    Icon(actionIcon(ActionGlyph.REFRESH_CW), null, Modifier.size(16.dp))
                    Text(stringResource(Res.string.load_more_failed_retry), Modifier.padding(start = 6.dp))
                }

            else -> Unit
        }
    }
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
