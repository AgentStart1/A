/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@Composable
internal fun FilePickerPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    content: @Composable PagerScope.(Int) -> Unit,
) {
    LaunchedEffect(state) {
        snapshotFlow { Triple(state.isScrollInProgress, state.currentPageOffsetFraction, state.currentPage) }
            .collect { (scrolling, offset, page) ->
                if (!scrolling && offset != 0f) {
                    // Accessibility ScrollBy can finish without the fling that normally aligns a page.
                    state.settleIdleAttachmentPage(page)
                }
            }
    }
    HorizontalPager(state, modifier = modifier, pageContent = content)
}

private suspend fun PagerState.settleIdleAttachmentPage(page: Int) {
    try {
        scroll { updateCurrentPage(page) }
    } catch (e: Exception) {
        if (e is CancellationException) currentCoroutineContext().ensureActive() else throw e
    }
}
