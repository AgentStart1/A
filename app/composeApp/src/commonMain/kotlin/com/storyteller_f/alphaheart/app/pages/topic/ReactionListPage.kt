/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.itemKey
import com.storyteller_f.alphaheart.app.common.createReactionsViewModel
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.shared.type.PrimaryKey

@Composable
fun ReactionListPage(topicId: PrimaryKey) {
    val viewModel = createReactionsViewModel(topicId)
    Scaffold { paddingValues ->
        StateView(
            viewModel,
            modifier = Modifier.padding(paddingValues),
        ) { pagingItems ->
            LazyColumn(
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    pagingItems.itemCount,
                    pagingItems.itemKey {
                        it.emoji
                    },
                ) { index ->
                    val info = pagingItems[index]
                    if (info != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(info.emoji, fontSize = 25.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(info.count.toString())
                        }
                    }
                }
            }
        }
    }
}
