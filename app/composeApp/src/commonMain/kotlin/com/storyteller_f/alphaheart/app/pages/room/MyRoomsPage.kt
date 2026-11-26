/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
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
import com.storyteller_f.alphaheart.app.previews.RoomCellPreviewProvider
import com.storyteller_f.alphaheart.app.room_members
import com.storyteller_f.alphaheart.app.room_read_only
import com.storyteller_f.alphaheart.app.room_status
import com.storyteller_f.alphaheart.app.room_unread
import com.storyteller_f.alphaheart.app.rooms
import com.storyteller_f.alphaheart.app.semantics.roomCellSemantics
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CommunityIcon
import com.storyteller_f.alphaheart.client.compose_core.components.RoomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBadge as Badge

@Composable
fun MyRoomsPage() {
    RoomList(createJoinedRoomsViewModel(), showAid = false)
}

@Composable
fun RoomList(roomsViewModel: RoomListViewModel, onClick: ((RoomInfo) -> Unit)? = null, showAid: Boolean = true) {
    val activities by roomsViewModel.previews.rows.collectAsStateWithLifecycle()
    val events = LocalGlobalDialog.current.context.events
    val refreshEvents = remember(events) { events.filter { it.changesRoomActivity() } }
    val refreshBadge = RememberUnreadBadgeRefresh()
    val listState = rememberLazyListState()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        StateView(
            roomsViewModel,
            Modifier.fillMaxSize(),
            onRefresh = refreshBadge,
            refreshEvents = refreshEvents,
        ) { items ->
            BoxWithConstraints(Modifier.widthIn(max = 740.dp).fillMaxSize()) {
                val wide = maxWidth >= 760.dp
                Column(Modifier.fillMaxSize()) {
                    if (wide) RoomTableHeader()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(0.dp),
                    ) {
                        topPrepend(items.loadState, items::retry)
                        pagingItems(items, { it.id }) { index ->
                            val room = items[index]
                            Column {
                                PrimaryRoomCell(room, onClick, wide, activities[room?.id], showAid)
                                if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(horizontal = 20.dp))
                            }
                        }
                        bottomAppending(items.loadState, items::retry)
                    }
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
    showAid: Boolean = true,
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
            .background(MaterialTheme.colorScheme.background, shape)
            .clip(shape)
            .clickable { openRoom?.invoke() }
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .roomCellSemantics(
                label = currentRoom?.name,
                onClick = openRoom,
            ),
        wide,
        activity,
        showAid,
    )
}

@Preview(widthDp = 300)
@Composable
fun UnboundedRoomCell(@PreviewParameter(RoomCellPreviewProvider::class) roomInfo: RoomInfo?) {
    val modifier = Modifier.fillMaxWidth()
    RoomCellInternal(roomInfo, modifier)
}

@Composable
private fun RoomCellInternal(
    roomInfo: RoomInfo?,
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    activity: RoomActivityPreview? = null,
    showAid: Boolean = true,
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
    RoomRowContent(roomInfo, communityInfo, wide, modifier, {
        RoomIconWithDialog(
            roomInfo,
            showDialog = roomShown,
            updateDialog = roomDialog::update,
        )
    }, activity, showAid, communityIcon = {
        CommunityIconWithDialog(communityInfo, communityShown, 22.dp, onClickIcon = communityDialog::update)
    })
}

/** Pure room presentation; callers provide navigation-aware icons outside screenshot fixtures. */
@Composable
fun RoomRowContent(
    roomInfo: RoomInfo?,
    communityInfo: CommunityInfo?,
    wide: Boolean,
    modifier: Modifier = Modifier,
    roomIcon: @Composable () -> Unit,
    activity: RoomActivityPreview? = null,
    showAid: Boolean = false,
    communityIcon: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier.appiumSemantics(text = roomActivityDescription(roomInfo?.name.orEmpty(), activity)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        roomIcon()
        Column(Modifier.weight(1f).padding(start = 8.dp, end = 12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    roomInfo?.name.orEmpty(),
                    Modifier.weight(1f, fill = false).appiumSemantics(text = roomInfo?.name),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (communityInfo != null) {
                    if (communityIcon != null) {
                        communityIcon()
                    } else {
                        CommunityIcon(communityInfo, 22.dp, false) {}
                    }
                }
            }
            if (showAid && roomInfo != null && roomInfo.aid.isNotBlank()) {
                Text(CoreStrings.aid(roomInfo.aid), style = MaterialTheme.typography.bodySmall)
            }
            if (activity?.message != null) ActivityMessageContent(activity.message)
        }
        RoomRowMetadata(roomInfo, wide, activity)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.RoomRowMetadata(
    roomInfo: RoomInfo?,
    wide: Boolean,
    activity: RoomActivityPreview?,
) {
    if (wide) {
        Text(roomInfo?.memberCount?.toString().orEmpty(), Modifier.width(88.dp))
        Text(
            if (roomInfo?.readOnly == true) stringResource(Res.string.room_read_only) else "—",
            Modifier.width(100.dp),
            maxLines = 2,
            style = MaterialTheme.typography.labelMedium,
        )
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            RoomUnreadBadge(roomInfo, activity)
        }
    } else {
        RoomUnreadBadge(roomInfo, activity)
    }
}

@Composable
private fun RoomUnreadBadge(roomInfo: RoomInfo?, activity: RoomActivityPreview?) {
    val count = activity?.unreadCount
    if (count != null && count > 0) {
        val label = count.toString()
        Badge { Text(label, Modifier.appiumSemantics(text = label)) }
    } else if (count == null && roomInfo?.hasUnread == true) {
        val label = stringResource(Res.string.room_unread)
        Badge { Text(label, Modifier.appiumSemantics(text = label)) }
    }
}

@Composable
private fun RoomTableHeader() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Spacer(Modifier.width(68.dp))
        Text(stringResource(Res.string.rooms), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
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
    setClickEvent: Boolean = true,
    updateDialog: (Boolean) -> Unit,
) {
    RoomIcon(roomInfo, width, setClickEvent, updateDialog)
    RoomDialog(showDialog, roomInfo) {
        updateDialog(false)
    }
}
