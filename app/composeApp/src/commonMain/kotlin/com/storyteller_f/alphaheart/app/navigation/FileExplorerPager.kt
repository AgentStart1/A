/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.navigation

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.pager.PagerState
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal data class FilePagerPosition(
    val history: List<NavKey>,
    val page: Int,
    val scrolling: Boolean,
    val offsetFraction: Float,
)

internal fun selectFileExplorerRoute(backStack: NavBackStack<NavKey>, path: String) {
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

internal fun fileExplorerRoute(path: String): FileExplorerRoute =
    when (path) {
        "/upload-record" -> FileExplorerRoute.UploadRecord
        "/download-record" -> FileExplorerRoute.DownloadRecord
        else -> FileExplorerRoute.Uploaded
    }

internal suspend fun PagerState.restoreFilePage(page: Int) {
    scroll(MutatePriority.PreventUserInput) { updateCurrentPage(page) }
}

internal suspend fun PagerState.settleIdleFilePage(page: Int) {
    try {
        scroll { updateCurrentPage(page) }
    } catch (e: Exception) {
        if (e is CancellationException) currentCoroutineContext().ensureActive() else throw e
    }
}

/** Removes unavailable owner-specific tabs from restored history, keeping the URL and pager in agreement. */
internal fun normalizeFileExplorerHistory(
    backStack: NavBackStack<NavKey>,
    availablePaths: Set<String>,
): FileExplorerRoute {
    val current = backStack.lastOrNull()
    val restored = availableFileRoute(current, availablePaths)
    val validHistory = backStack.filter { it.toString() in availablePaths }
    if (validHistory.size != backStack.size) {
        backStack.clear()
        backStack.addAll(validHistory)
    }
    if (backStack.isEmpty()) {
        backStack.add(restored)
    } else if (current?.toString() !in availablePaths) {
        selectFileExplorerRoute(backStack, restored.toString())
    }
    return restored
}

internal fun availableFileRoute(route: NavKey?, availablePaths: Set<String>): FileExplorerRoute =
    if (route is FileExplorerRoute && route.toString() in availablePaths) {
        route
    } else {
        require(FileExplorerRoute.Uploaded.toString() in availablePaths) { "Uploaded files must remain available" }
        FileExplorerRoute.Uploaded
    }
