/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalSessionManager
import com.storyteller_f.alphaheart.app.common.ActivityPreviewHost
import com.storyteller_f.alphaheart.app.common.OnRoomActivityRefreshRequested
import com.storyteller_f.alphaheart.app.common.changesRoomActivity
import com.storyteller_f.alphaheart.app.common.previewCoordination
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.getJoinedPinnedTopics
import com.storyteller_f.alphaheart.client.core.getUnreadRoomActivities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
internal fun RememberActivityPreviewHost(): ActivityPreviewHost {
    val session = LocalSessionManager.current
    val scope = rememberCoroutineScope()
    val host =
        remember(session) {
            ActivityPreviewHost(
                scope,
                previewCoordination,
                Dispatchers.Default,
                session::getUnreadRoomActivities,
                session::getJoinedPinnedTopics,
            )
        }
    DisposableEffect(host) { onDispose { host.close() } }
    return host
}

@Composable
internal fun ObserveActivityEvents(
    host: ActivityPreviewHost,
    context: Boolean = false,
    events: Flow<Any> = LocalGlobalDialog.current.context.events,
) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(host, owner, context, events) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                if (context) {
                    host.refreshUnread()
                    host.refreshPinned()
                }
                events.collect { host.onEvent(it, context) }
            } finally {
                host.pause()
            }
        }
    }
}

/** Keeps the navigation badge aligned with read/membership events while Home is active. */
@Composable
internal fun <T> ObserveUnreadMessageCountEvents(
    handler: LoadingHandler<T>,
    events: Flow<Any> = LocalGlobalDialog.current.context.events,
) {
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(handler, owner, events) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            handler.refreshAfterLoading()
            events.collectLatest {
                if (it is OnRoomActivityRefreshRequested || it.changesRoomActivity()) handler.refreshAfterLoading()
            }
        }
    }
}

private suspend fun <T> LoadingHandler<T>.refreshAfterLoading() {
    // SimpleLoadingHandler drops refreshes while loading; preserve the latest read/membership update.
    state.first { it !is LoadingState.Loading }
    refresh()
}

/** Explicit source refresh also updates navigation without restarting the row preview request. */
@Composable
internal fun RememberUnreadBadgeRefresh(): () -> Unit {
    val events = LocalGlobalDialog.current.context.events
    val scope = rememberCoroutineScope()
    return remember(events, scope) { { scope.launch { events.emit(OnRoomActivityRefreshRequested) } } }
}
