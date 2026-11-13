/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.storyteller_f.alphaheart.app.common.UserCommentsViewModel
import com.storyteller_f.alphaheart.app.common.getUserCommentsViewModel
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea

@Composable
fun UserCommentsPage() {
    val viewModel = getUserCommentsViewModel()
    UserCommentsPageInternal(viewModel)
}

@Composable
fun UserCommentsPageInternal(viewModel: UserCommentsViewModel) {
    Scaffold { paddingValues ->
        StateView(
            viewModel,
            modifier = Modifier.safeArea(paddingValues, LocalLayoutDirection.current),
        ) { items ->
            AdaptiveItemGrid {
                pagingItems(items, {
                    it.id
                }) {
                    val topicInfo = items[it]
                    if (topicInfo != null) {
                        TopicCell(topicInfo)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
