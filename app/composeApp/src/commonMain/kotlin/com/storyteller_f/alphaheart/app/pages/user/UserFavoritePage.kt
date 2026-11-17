/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.FavoritesViewModel
import com.storyteller_f.alphaheart.app.common.OnAddFavorite
import com.storyteller_f.alphaheart.app.common.OnRemoveFavorite
import com.storyteller_f.alphaheart.app.common.getFavoriteViewModel
import com.storyteller_f.alphaheart.app.my_favorites
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.ReadingRecordList
import com.storyteller_f.alphaheart.app.pages.community.CommunityCell
import com.storyteller_f.alphaheart.app.pages.file.FileCell
import com.storyteller_f.alphaheart.app.pages.recordListFooter
import com.storyteller_f.alphaheart.app.pages.room.PrimaryRoomCell
import com.storyteller_f.alphaheart.app.pages.title.TitleItem
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.app.record_favorites_empty
import com.storyteller_f.alphaheart.app.record_favorites_end
import com.storyteller_f.alphaheart.app.record_favorites_failure
import com.storyteller_f.alphaheart.app.record_favorites_loading
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UserFavoriteItem
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

@Composable
fun UserFavoritePage() {
    val viewModel = getFavoriteViewModel()
    val events = LocalGlobalDialog.current.context.events
    val refreshEvents = remember(events) { events.filter { it is OnAddFavorite || it is OnRemoveFavorite } }
    FavoritePageInternal(viewModel, refreshEvents)
}

@Composable
fun FavoritePageInternal(viewModel: FavoritesViewModel, refreshEvents: Flow<Any>? = null) {
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.my_favorites)) }) { paddingValues ->
        StateView(
            viewModel,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            refreshEvents = refreshEvents,
            emptyText = stringResource(Res.string.record_favorites_empty),
            loadingText = stringResource(Res.string.record_favorites_loading),
            failureText = stringResource(Res.string.record_favorites_failure),
        ) { items ->
            val endLabel = stringResource(Res.string.record_favorites_end)
            ReadingRecordList {
                pagingItems(items, {
                    it.id
                }) {
                    UserFavoriteCell(items[it])
                }
                recordListFooter(items, endLabel)
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
    val navigation = LocalAppNavFactory.current
    when (userFavoriteInfo?.objectType) {
        ObjectType.TOPIC -> userFavoriteInfo.topicInfo?.let { TopicCell(it) }

        ObjectType.COMMUNITY -> userFavoriteInfo.communityInfo?.let { CommunityCell(it) }

        ObjectType.ROOM -> userFavoriteInfo.roomInfo?.let { PrimaryRoomCell(it) }

        ObjectType.USER ->
            userFavoriteInfo.userInfo?.let { info ->
                UserCell(info) { navigation.newAppNav().gotoUser(it.id) }
            }

        ObjectType.TITLE -> userFavoriteInfo.titleInfo?.let { TitleItem(it) }

        ObjectType.FILE ->
            userFavoriteInfo.fileInfo?.let { info ->
                FileCell(info) { navigation.newAppNav().gotoMedia(info) }
            }

        else -> Unit
    }
}
