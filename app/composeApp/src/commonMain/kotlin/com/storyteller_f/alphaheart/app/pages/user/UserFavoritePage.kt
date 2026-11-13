/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.storyteller_f.alphaheart.app.common.FavoritesViewModel
import com.storyteller_f.alphaheart.app.common.getFavoriteViewModel
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UserFavoriteItem
import com.storyteller_f.shared.type.ObjectType

@Composable
fun UserFavoritePage() {
    val viewModel = getFavoriteViewModel()
    FavoritePageInternal(viewModel)
}

@Composable
fun FavoritePageInternal(viewModel: FavoritesViewModel) {
    Scaffold { paddingValues ->
        StateView(viewModel, modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) { items ->
            AdaptiveItemGrid {
                pagingItems(items, {
                    it.id
                }) {
                    UserFavoriteCell(items[it])
                }
            }
        }
    }
}

class UserFavoritePreviewProvider : PreviewParameterProvider<UserFavoriteItem> {
    override val values: Sequence<UserFavoriteItem>
        get() =
            sequenceOf(
                UserFavoriteItem.EMPTY.copy(
                    topicInfo =
                    TopicDetail.EMPTY.copy(
                        topicInfo = TopicDetail.EMPTY.topicInfo.copy(content = TopicContent.Plain("hello")),
                    ),
                ),
            )
}

@Preview(widthDp = 300)
@Composable
fun UserFavoriteCell(@PreviewParameter(UserFavoritePreviewProvider::class) userFavoriteInfo: UserFavoriteItem?) {
    when (userFavoriteInfo?.objectType) {
        ObjectType.TOPIC -> {
            val topicInfo = userFavoriteInfo.topicInfo
            if (topicInfo != null) {
                TopicCell(topicInfo)
            }
        }

        else -> {
        }
    }
}
