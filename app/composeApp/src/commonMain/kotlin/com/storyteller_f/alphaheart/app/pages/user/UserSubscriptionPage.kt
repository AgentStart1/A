/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.common.SubscriptionsViewModel
import com.storyteller_f.alphaheart.app.common.getSubscriptionViewModel
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UserSubscriptionItem
import com.storyteller_f.shared.type.ObjectType

@Composable
fun UserSubscriptionPage() {
    val viewModel = getSubscriptionViewModel()
    SubscriptionPageInternal(viewModel)
}

@Composable
fun SubscriptionPageInternal(viewModel: SubscriptionsViewModel) {
    Scaffold { paddingValues ->
        StateView(
            viewModel,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
        ) { items ->
            AdaptiveItemGrid {
                pagingItems(items, {
                    it.id
                }) {
                    UserSubscriptionCell(items[it])
                    HorizontalDivider()
                }
            }
        }
    }
}

class UserSubscriptionPreviewProvider : PreviewParameterProvider<UserSubscriptionItem> {
    override val values: Sequence<UserSubscriptionItem>
        get() =
            sequenceOf(
                UserSubscriptionItem.EMPTY.copy(
                    topicInfo =
                    TopicDetail.EMPTY.copy(
                        topicInfo = TopicDetail.EMPTY.topicInfo.copy(content = TopicContent.Plain("hello")),
                    ),
                ),
            )
}

@Preview(widthDp = 300)
@Composable
fun UserSubscriptionCell(
    @PreviewParameter(UserSubscriptionPreviewProvider::class) userFavoriteInfo: UserSubscriptionItem?,
) {
    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Subscribed at ${userFavoriteInfo?.createdTime ?: "<none>"}",
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        if (userFavoriteInfo?.objectType == ObjectType.TOPIC) {
            val topicInfo = userFavoriteInfo.topicInfo
            if (topicInfo != null) {
                Box(
                    modifier =
                    Modifier.padding(horizontal = 10.dp).background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(8.dp),
                    ),
                ) {
                    TopicCell(topicInfo)
                }
            }
        }
    }
}
