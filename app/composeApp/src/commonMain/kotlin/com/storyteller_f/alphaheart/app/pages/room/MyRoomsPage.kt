/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.RoomActivityPreview
import com.storyteller_f.alphaheart.app.common.RoomListViewModel
import com.storyteller_f.alphaheart.app.common.changesRoomActivity
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.common.createJoinedRoomsViewModel
import com.storyteller_f.alphaheart.app.pages.ActivityMessageContent
import com.storyteller_f.alphaheart.app.pages.RememberUnreadBadgeRefresh
import com.storyteller_f.alphaheart.app.pages.community.CommunityIconWithDialog
import com.storyteller_f.alphaheart.app.pages.roomActivityDescription
import com.storyteller_f.alphaheart.app.room_community
import com.storyteller_f.alphaheart.app.room_members
import com.storyteller_f.alphaheart.app.room_no_community
import com.storyteller_f.alphaheart.app.room_read_only
import com.storyteller_f.alphaheart.app.room_status
import com.storyteller_f.alphaheart.app.room_unread
import com.storyteller_f.alphaheart.app.rooms
import com.storyteller_f.alphaheart.client.compose_core.components.LayoutDefaults
import com.storyteller_f.alphaheart.client.compose_core.components.RoomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

@Composable
fun MyRoomsPage() {
    val viewModel = createJoinedRoomsViewModel()
    RoomList(viewModel)
}

@Composable
fun RoomList(roomsViewModel: RoomListViewModel, onClick: ((RoomInfo) -> Unit)? = null) {
    val activities by roomsViewModel.previews.rows.collectAsStateWithLifecycle()
    val events = LocalGlobalDialog.current.context.events
    val refreshEvents = remember(events) { events.filter { it.changesRoomActivity() } }
    val refreshBadge = RememberUnreadBadgeRefresh()
    val listState = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp
        Column(Modifier.fillMaxSize()) {
            if (wide) RoomTableHeader()
            StateView(
                roomsViewModel,
                Modifier.weight(1f).fillMaxWidth(),
                onRefresh = refreshBadge,
                refreshEvents = refreshEvents,
            ) { items ->
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = LayoutDefaults.contentPadding,
                    verticalArrangement = LayoutDefaults.pagingVerticalArrangement,
                ) {
                    topPrepend(items.loadState)
                    pagingItems(items, { it.id }) { index ->
                        val room = items[index]
                        PrimaryRoomCell(room, onClick, wide, activities[room?.id])
                    }
                    bottomAppending(items.loadState)
                }
            }
        }
    }
}

@Preview(widthDp = 300)
@Composable
fun PrimaryRoomCell(
    @PreviewParameter(RoomCellPreviewProvider::class) roomInfo: RoomInfo?,
    onClick: ((RoomInfo) -> Unit)? = null,
    wide: Boolean = false,
    activity: RoomActivityPreview? = null,
) {
    val appNavFactory = LocalAppNavFactory.current
    val currentRoom = activity?.room ?: roomInfo
    val openRoom =
        currentRoom?.let { info ->
            {
                onClick?.invoke(info) ?: appNavFactory.newAppNav().gotoRoom(info.id, false)
            }
        }
    val shape = RoundedCornerShape(10.dp)
    RoomCellInternal(
        currentRoom,
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, shape)
            .clip(shape)
            .clickable { openRoom?.invoke() }
            .padding(10.dp)
            .roomCellSemantics(
                label = currentRoom?.name,
                onClick = openRoom,
            ),
        wide,
        activity,
    )
}

@Preview(widthDp = 300)
@Composable
fun UnboundedRoomCell(@PreviewParameter(RoomCellPreviewProvider::class) roomInfo: RoomInfo?) {
    val modifier = Modifier.fillMaxWidth()
    RoomCellInternal(roomInfo, modifier)
}

class RoomCellPreviewProvider : PreviewParameterProvider<RoomInfo> {
    override val values: Sequence<RoomInfo>
        get() =
            sequenceOf(
                RoomInfo.EMPTY.copy(name = "Room Name", latestTopic = 1, lastRead = 0),
            )
}

@Composable
private fun RoomCellInternal(
    roomInfo: RoomInfo?,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    activity: RoomActivityPreview? = null,
) {
    val communityInfo =
        roomInfo?.communityId?.let { communityId ->
            val model = createCommunityViewModel(communityId)
            val info by model.handler.data.collectAsState()
            info
        }
    val roomDialog = rememberCommonDialogController()
    val roomShown by roomDialog.shown
    val communityDialog = rememberCommonDialogController()
    val communityShown by communityDialog.shown
    BadgedBox(badge = {
        if (!wide && activity == null && roomInfo?.hasUnread == true) Badge(containerColor = Color.Red)
    }) {
        RoomRowContent(roomInfo, communityInfo, wide, modifier, {
            RoomIconWithDialog(roomInfo, showDialog = roomShown, updateDialog = roomDialog::update)
        }, {
            CommunityIconWithDialog(communityInfo, communityShown, onClickIcon = communityDialog::update)
        }, activity)
    }
}

/** Pure room presentation; callers provide navigation-aware icons outside screenshot fixtures. */
@Composable
fun RoomRowContent(
    roomInfo: RoomInfo?,
    communityInfo: CommunityInfo?,
    wide: Boolean,
    modifier: Modifier = Modifier,
    roomIcon: @Composable () -> Unit,
    communityIcon: @Composable () -> Unit,
    activity: RoomActivityPreview? = null,
) {
    Row(
        modifier.appiumSemantics(text = roomActivityDescription(roomInfo?.name.orEmpty(), activity)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        roomIcon()
        Column(Modifier.weight(1f).padding(start = 8.dp, end = 12.dp)) {
            Text(
                roomInfo?.name.orEmpty(),
                Modifier.appiumSemantics(text = roomInfo?.name),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (activity != null) ActivityMessageContent(activity.message)
        }
        if (wide) {
            Text(
                communityInfo?.name ?: if (roomInfo?.communityId == null) {
                    stringResource(Res.string.room_no_community)
                } else {
                    "—"
                },
                Modifier.weight(0.8f).padding(end = 12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(roomInfo?.memberCount?.toString().orEmpty(), Modifier.width(88.dp))
            Text(
                if (roomInfo?.readOnly == true) stringResource(Res.string.room_read_only) else "—",
                Modifier.width(100.dp),
                maxLines = 2,
                style = MaterialTheme.typography.labelMedium,
            )
            Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
                if (activity?.unreadCount?.let { it > 0 } ?: (roomInfo?.hasUnread == true)) {
                    Badge {
                        val label = activity?.unreadCount?.toString() ?: stringResource(Res.string.room_unread)
                        Text(label, Modifier.appiumSemantics(text = label))
                    }
                }
            }
        } else {
            if (roomInfo?.communityId != null) communityIcon()
            if (activity != null && activity.unreadCount > 0) {
                Badge {
                    Text(
                        activity.unreadCount.toString(),
                        Modifier.appiumSemantics(text = activity.unreadCount.toString()),
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomTableHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Spacer(Modifier.width(68.dp))
        Text(stringResource(Res.string.rooms), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(
            stringResource(Res.string.room_community),
            Modifier.weight(0.8f),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            stringResource(Res.string.room_members),
            Modifier.width(88.dp).appiumSemantics(text = stringResource(Res.string.room_members)),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            stringResource(Res.string.room_status),
            Modifier.width(100.dp),
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            stringResource(Res.string.room_unread),
            Modifier.width(64.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun RoomIconWithDialog(
    roomInfo: RoomInfo?,
    showDialog: Boolean,
    width: Dp = 60.dp,
    setClickEvent: Boolean = false,
    updateDialog: (Boolean) -> Unit,
) {
    RoomIcon(roomInfo, width, setClickEvent, updateDialog)
    RoomDialog(showDialog, roomInfo) {
        updateDialog(false)
    }
}
