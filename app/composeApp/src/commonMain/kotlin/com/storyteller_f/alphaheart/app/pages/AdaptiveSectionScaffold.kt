/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.CustomBottomNav
import com.storyteller_f.alphaheart.client.compose_core.components.CustomRailNav
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import kotlinx.coroutines.launch

/** Page-local navigation shares one pager and preserves section state across resizes. */
@Composable
fun AdaptiveSectionScaffold(
    pagerState: PagerState,
    routes: List<NavRoute>,
    hasUnread: Boolean = false,
    unreadMessageCount: Long = 0,
    modifier: Modifier = Modifier,
    search: @Composable (Int, Boolean) -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    onSelectRoute: ((String) -> Unit)? = null,
    searchStateKey: String? = null,
    section: @Composable (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val searchStates = rememberSaveableStateHolder()
    val selectSection: (String) -> Unit = { path ->
        if (onSelectRoute != null) {
            onSelectRoute(path)
        } else {
            scope.launch { pagerState.animateScrollToPage(routes.indexOfFirst { it.path == path }) }
        }
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxWidth < 600.dp
        Scaffold(
            floatingActionButton = floatingActionButton,
            bottomBar = {
                if (compact) {
                    CustomBottomNav(
                        routes[pagerState.currentPage].path,
                        routes,
                        hasUnread,
                        unreadMessageCount,
                        selectSection,
                    )
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                if (!compact) {
                    CustomRailNav(
                        routes[pagerState.currentPage].path,
                        routes,
                        hasUnread,
                        unreadMessageCount,
                        selectSection,
                    )
                }
                Column(Modifier.weight(1f).fillMaxSize()) {
                    searchStates.SaveableStateProvider(searchStateKey ?: routes[pagerState.currentPage].path) {
                        search(pagerState.currentPage, compact)
                    }
                    HorizontalPager(
                        pagerState,
                        Modifier.weight(1f),
                        userScrollEnabled = compact,
                    ) { section(it) }
                }
            }
        }
    }
}
