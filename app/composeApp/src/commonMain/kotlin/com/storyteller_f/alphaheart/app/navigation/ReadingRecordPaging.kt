/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending

internal fun LazyListScope.recordListFooter(items: LazyPagingItems<*>, endLabel: String) {
    bottomAppending(items.loadState, items::retry)
    val append = items.loadState.append
    if (items.itemCount > 0 && items.loadState.refresh is LoadState.NotLoading &&
        append is LoadState.NotLoading && append.endOfPaginationReached
    ) {
        item("records-end") {
            Box(Modifier.fillParentMaxWidth(), contentAlignment = Alignment.Center) {
                Text(endLabel, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        }
    }
}
