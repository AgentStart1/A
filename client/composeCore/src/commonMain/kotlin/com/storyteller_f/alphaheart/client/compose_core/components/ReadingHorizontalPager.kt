/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerScope
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.semantics
import kotlin.math.roundToInt

/** Native accessibility scrolls settle on pages, just like pointer and keyboard navigation. */
@Composable
fun ReadingHorizontalPager(
    state: PagerState,
    modifier: Modifier = Modifier,
    content: @Composable PagerScope.(Int) -> Unit,
) {
    var pendingPosition by remember(state) { mutableStateOf<Float?>(null) }
    val requestedPosition = pendingPosition
    LaunchedEffect(state, requestedPosition) {
        val position = requestedPosition ?: return@LaunchedEffect
        try {
            state.scrollToPage(position.roundToInt())
        } finally {
            if (pendingPosition == requestedPosition) pendingPosition = null
        }
    }
    // A completed no-op request must also republish semantics to restore the mirrored DOM offset.
    val nativeScroll =
        remember(state, requestedPosition) {
            Modifier.semantics {
                scrollBy { horizontal, _ ->
                    val pageSize = state.layoutInfo.pageSize
                    if (pageSize <= 0 || state.pageCount == 0 || !horizontal.isFinite()) {
                        false
                    } else {
                        val current = pendingPosition ?: state.currentPage + state.currentPageOffsetFraction
                        pendingPosition =
                            (current + horizontal / pageSize).coerceIn(0f, (state.pageCount - 1).toFloat())
                        true
                    }
                }
            }
        }
    HorizontalPager(state, modifier.then(nativeScroll), pageContent = content)
}
