/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.navigation

import androidx.compose.foundation.pager.PagerState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal suspend fun PagerState.settleIdleAttachmentPage(page: Int) {
    try {
        scroll { updateCurrentPage(page) }
    } catch (e: Exception) {
        if (e is CancellationException) currentCoroutineContext().ensureActive() else throw e
    }
}
