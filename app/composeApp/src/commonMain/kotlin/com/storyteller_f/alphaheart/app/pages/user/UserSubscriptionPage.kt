/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.SubscriptionsViewModel
import com.storyteller_f.alphaheart.app.common.getSubscriptionViewModel
import com.storyteller_f.alphaheart.app.my_subscriptions
import com.storyteller_f.alphaheart.app.navigation.recordListFooter
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.ReadingRecordList
import com.storyteller_f.alphaheart.app.pages.community.CommunityRefCell
import com.storyteller_f.alphaheart.app.pages.room.RoomRefCell
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.app.previews.UserSubscriptionPreviewProvider
import com.storyteller_f.alphaheart.app.record_subscriptions_empty
import com.storyteller_f.alphaheart.app.record_subscriptions_end
import com.storyteller_f.alphaheart.app.record_subscriptions_failure
import com.storyteller_f.alphaheart.app.record_subscriptions_loading
import com.storyteller_f.alphaheart.app.subscribed_at
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.shared.model.UserSubscriptionItem
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.stringResource

@Composable
fun UserSubscriptionPage() {
    val viewModel = getSubscriptionViewModel()
    SubscriptionPageInternal(viewModel)
}

@Composable
fun SubscriptionPageInternal(viewModel: SubscriptionsViewModel) {
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.my_subscriptions)) }) { paddingValues ->
        StateView(
            viewModel,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            emptyText = stringResource(Res.string.record_subscriptions_empty),
            loadingText = stringResource(Res.string.record_subscriptions_loading),
            failureText = stringResource(Res.string.record_subscriptions_failure),
        ) { items ->
            val endLabel = stringResource(Res.string.record_subscriptions_end)
            ReadingRecordList {
                pagingItems(items, {
                    it.id
                }) {
                    Column {
                        UserSubscriptionCell(items[it])
                        if (it < items.itemCount - 1) {
                            HorizontalDivider()
                        }
                    }
                }
                recordListFooter(items, endLabel)
            }
        }
    }
}

@Preview(widthDp = 300)
@Composable
fun UserSubscriptionCell(
    @PreviewParameter(UserSubscriptionPreviewProvider::class) userFavoriteInfo: UserSubscriptionItem?,
) {
    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            userFavoriteInfo?.createdTime?.let {
                stringResource(
                    Res.string.subscribed_at,
                    it.formatDisplayTime(),
                )
            }.orEmpty(),
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        when (userFavoriteInfo?.objectType) {
            ObjectType.USER -> UserRefCell(userFavoriteInfo.objectId)
            ObjectType.COMMUNITY -> CommunityRefCell(userFavoriteInfo.objectId)
            ObjectType.ROOM -> RoomRefCell(userFavoriteInfo.objectId)
            ObjectType.TOPIC -> userFavoriteInfo.topicInfo?.let { TopicCell(it) }
            else -> Unit
        }
    }
}
