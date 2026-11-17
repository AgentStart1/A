/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending

/** Personal records retain one reading column across viewport changes. */
@Composable
internal fun ReadingRecordList(content: LazyListScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 740.dp).fillMaxSize().testTag("personal-record-list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = content,
        )
    }
}

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
