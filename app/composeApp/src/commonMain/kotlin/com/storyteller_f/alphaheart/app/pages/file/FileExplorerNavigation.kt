/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.savedstate.serialization.SavedStateConfiguration
import com.storyteller_f.alphaheart.app.navigation.FileExplorerRoute
import com.storyteller_f.alphaheart.app.navigation.FilePagerPosition
import com.storyteller_f.alphaheart.app.navigation.availableFileRoute
import com.storyteller_f.alphaheart.app.navigation.fileExplorerRoute
import com.storyteller_f.alphaheart.app.navigation.normalizeFileExplorerHistory
import com.storyteller_f.alphaheart.app.navigation.restoreFilePage
import com.storyteller_f.alphaheart.app.navigation.selectFileExplorerRoute
import com.storyteller_f.alphaheart.app.navigation.settleIdleFilePage
import com.storyteller_f.alphaheart.app.pages.AdaptiveSectionScaffold
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.utils.BindBrowserNavigation
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

@Composable
internal fun rememberFileExplorerBackStack(): NavBackStack<NavKey> {
    val config =
        remember {
            SavedStateConfiguration {
                serializersModule =
                    SerializersModule {
                        polymorphic(NavKey::class) {
                            subclass(FileExplorerRoute.Uploaded::class)
                            subclass(FileExplorerRoute.UploadRecord::class)
                            subclass(FileExplorerRoute.DownloadRecord::class)
                        }
                    }
            }
        }
    val backStack = rememberNavBackStack(config, FileExplorerRoute.Uploaded)
    BindBrowserNavigation(backStack, config.serializersModule, "app/files")
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = backStack.size > 1,
    ) { if (backStack.size > 1) backStack.removeLastOrNull() }
    return backStack
}

/** Navigation and page composition remain mounted while only the rail/bottom controls change. */
@Composable
internal fun FileExplorerNavigation(
    backStack: NavBackStack<NavKey>,
    routes: List<NavRoute>,
    modifier: Modifier = Modifier,
    search: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit = {},
    section: @Composable (FileExplorerRoute) -> Unit,
) {
    require(routes.isNotEmpty()) { "File navigation requires an uploaded-files route" }
    val availablePaths = routes.map { it.path }.toSet()
    val initialRoute = availableFileRoute(backStack.lastOrNull(), availablePaths)
    val initialPage = routes.indexOfFirst { it.path == initialRoute.toString() }
    val pagerState = key(availablePaths) { rememberPagerState(initialPage = initialPage) { routes.size } }
    BindFileExplorerPager(backStack, pagerState, routes, initialRoute)
    AdaptiveSectionScaffold(
        pagerState = pagerState,
        routes = routes,
        modifier = modifier,
        search = { _, _ -> search() },
        floatingActionButton = floatingActionButton,
        onSelectRoute = { selectFileExplorerRoute(backStack, it) },
        searchStateKey = "file-search",
    ) { page -> section(fileExplorerRoute(routes[page].path)) }
}

@Composable
private fun BindFileExplorerPager(
    backStack: NavBackStack<NavKey>,
    pagerState: PagerState,
    routes: List<NavRoute>,
    initialRoute: NavKey,
) {
    val availablePaths = routes.map { it.path }.toSet()
    LaunchedEffect(backStack, pagerState, availablePaths) {
        var route: NavKey = normalizeFileExplorerHistory(backStack, availablePaths)
        var page = routes.indexOfFirst { it.path == route.toString() }
        if (route != initialRoute) pagerState.restoreFilePage(page)
        snapshotFlow {
            FilePagerPosition(
                backStack.toList(),
                pagerState.settledPage,
                pagerState.isScrollInProgress,
                pagerState.currentPageOffsetFraction,
            )
        }
            .collect { position ->
                val selectedRoute = position.history.lastOrNull()
                if (selectedRoute == null || position.history.any { it.toString() !in availablePaths }) {
                    route = normalizeFileExplorerHistory(backStack, availablePaths)
                    page = routes.indexOfFirst { it.path == route.toString() }
                    pagerState.restoreFilePage(page)
                } else if (selectedRoute != route) {
                    route = selectedRoute
                    page = routes.indexOfFirst { it.path == route.toString() }
                    // Browser navigation wins over an obsolete drag, without publishing a new route.
                    pagerState.restoreFilePage(page)
                } else if (!position.scrolling && position.page != page) {
                    page = position.page
                    selectFileExplorerRoute(backStack, routes[page].path)
                    route = backStack.last()
                } else if (!position.scrolling && position.offsetFraction != 0f) {
                    // Accessibility ScrollBy can stop between pages without a fling to settle it.
                    pagerState.settleIdleFilePage(page)
                }
            }
    }
}
