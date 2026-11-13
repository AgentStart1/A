/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.storyteller_f.alphaheart.app.pages.AdaptiveSectionScaffold
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
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
    val initialRoute = backStack.last()
    val initialPage = routes.indexOfFirst { it.path == initialRoute.toString() }
    val pagerState = rememberPagerState(initialPage = initialPage) { routes.size }
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

private data class FilePagerPosition(val route: NavKey, val page: Int, val scrolling: Boolean)

@Composable
private fun BindFileExplorerPager(
    backStack: NavBackStack<NavKey>,
    pagerState: PagerState,
    routes: List<NavRoute>,
    initialRoute: NavKey,
) {
    LaunchedEffect(backStack, pagerState) {
        var route = initialRoute
        var page = routes.indexOfFirst { it.path == route.toString() }
        snapshotFlow { FilePagerPosition(backStack.last(), pagerState.settledPage, pagerState.isScrollInProgress) }
            .collect { position ->
                if (position.route != route) {
                    route = position.route
                    page = routes.indexOfFirst { it.path == route.toString() }
                    // Browser navigation wins over an obsolete drag, without publishing a new route.
                    pagerState.restoreFilePage(page)
                } else if (!position.scrolling && position.page != page) {
                    page = position.page
                    selectFileExplorerRoute(backStack, routes[page].path)
                    route = backStack.last()
                }
            }
    }
}

private fun selectFileExplorerRoute(backStack: NavBackStack<NavKey>, path: String) {
    val target = fileExplorerRoute(path)
    if (backStack.last() != target) {
        val index = backStack.indexOf(target)
        if (index >= 0) {
            repeat(backStack.size - index - 1) { backStack.removeLastOrNull() }
        } else {
            backStack.add(target)
        }
    }
}

private fun fileExplorerRoute(path: String): FileExplorerRoute =
    when (path) {
        "/upload-record" -> FileExplorerRoute.UploadRecord
        "/download-record" -> FileExplorerRoute.DownloadRecord
        else -> FileExplorerRoute.Uploaded
    }

private suspend fun PagerState.restoreFilePage(page: Int) {
    scroll(MutatePriority.PreventUserInput) { updateCurrentPage(page) }
}
