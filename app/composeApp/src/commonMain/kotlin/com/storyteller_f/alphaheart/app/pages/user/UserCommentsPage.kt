/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.UserCommentsViewModel
import com.storyteller_f.alphaheart.app.common.getUserCommentsViewModel
import com.storyteller_f.alphaheart.app.my_comments
import com.storyteller_f.alphaheart.app.navigation.recordListFooter
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.ReadingRecordList
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.app.record_comments_empty
import com.storyteller_f.alphaheart.app.record_comments_end
import com.storyteller_f.alphaheart.app.record_comments_failure
import com.storyteller_f.alphaheart.app.record_comments_loading
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import org.jetbrains.compose.resources.stringResource

@Composable
fun UserCommentsPage() {
    val viewModel = getUserCommentsViewModel()
    UserCommentsPageInternal(viewModel)
}

@Composable
fun UserCommentsPageInternal(viewModel: UserCommentsViewModel) {
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.my_comments)) }) { paddingValues ->
        StateView(
            viewModel,
            modifier = Modifier.safeArea(paddingValues, LocalLayoutDirection.current),
            emptyText = stringResource(Res.string.record_comments_empty),
            loadingText = stringResource(Res.string.record_comments_loading),
            failureText = stringResource(Res.string.record_comments_failure),
        ) { items ->
            val endLabel = stringResource(Res.string.record_comments_end)
            ReadingRecordList {
                pagingItems(items, {
                    it.id
                }) {
                    val topicInfo = items[it]
                    if (topicInfo != null) {
                        TopicCell(topicInfo)
                    }
                    if (it < items.itemCount - 1) HorizontalDivider()
                }
                recordListFooter(items, endLabel)
            }
        }
    }
}
