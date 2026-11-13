/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.common.UserReactionRecordsViewModel
import com.storyteller_f.alphaheart.app.common.getUserReactionRecordsViewModel
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.shared.model.ReactionRecordInfo

@Composable
fun UserReactionRecordsPage() {
    val viewModel = getUserReactionRecordsViewModel()
    UserReactionRecordsPageInternal(viewModel)
}

@Composable
fun UserReactionRecordsPageInternal(viewModel: UserReactionRecordsViewModel) {
    Scaffold { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        StateView(
            viewModel,
            modifier = Modifier.safeArea(paddingValues, layoutDirection),
        ) { items ->
            AdaptiveItemGrid {
                pagingItems(items, {
                    it.id
                }) {
                    UserReactionRecordCell(items[it])
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun UserReactionRecordCell(reactionRecordInfo: ReactionRecordInfo?) {
    if (reactionRecordInfo != null) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(reactionRecordInfo.emoji, modifier = Modifier.padding(end = 8.dp))
            Column {
                Text("Object ID: ${reactionRecordInfo.objectId}")
                Text("Type: ${reactionRecordInfo.objectType}")
                Text("Created: ${reactionRecordInfo.createdTime}")
            }
        }
    }
}
