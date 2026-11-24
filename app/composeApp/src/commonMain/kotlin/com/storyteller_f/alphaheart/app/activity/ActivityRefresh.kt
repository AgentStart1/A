/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.activity

import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import kotlinx.coroutines.flow.first

internal suspend fun <T> LoadingHandler<T>.refreshAfterLoading() {
    // SimpleLoadingHandler drops refreshes while loading; preserve the latest read/membership update.
    state.first { it !is LoadingState.Loading }
    refresh()
}
