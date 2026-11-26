/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.alphaheart.client.core.updateRoomStatus
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OnRoomStatusUpdated
import com.storyteller_f.alphaheart.panel.common.PanelLogsTab
import com.storyteller_f.alphaheart.panel.common.createPanelRoomFilesViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelRoomMembersViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelRoomViewModel
import com.storyteller_f.alphaheart.panel.components.DetailNavigationScaffold
import com.storyteller_f.alphaheart.panel.components.PanelDetailHeading
import com.storyteller_f.alphaheart.panel.components.PanelInfoTable
import com.storyteller_f.alphaheart.panel.components.panelOptionalValueText
import com.storyteller_f.alphaheart.panel.components.panelValueText
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.private_room_detail_title
import com.storyteller_f.alphaheart.panel.room_detail_title
import com.storyteller_f.alphaheart.panel.set_normal
import com.storyteller_f.alphaheart.panel.set_read_only
import com.storyteller_f.alphaheart.panel.tab_basic_info
import com.storyteller_f.alphaheart.panel.tab_files
import com.storyteller_f.alphaheart.panel.tab_logs
import com.storyteller_f.alphaheart.panel.tab_members
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.UpdateObjectStatusBody
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingHorizontalPager as HorizontalPager
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailPage(id: PrimaryKey) {
    val pagerState = rememberPagerState { 2 }
    val navRoutes =
        listOf(
            NavRoute("/info", navigationIcon(NavigationGlyph.INFO), stringResource(Res.string.tab_basic_info)),
            NavRoute("/logs", navigationIcon(NavigationGlyph.FILE_TEXT), stringResource(Res.string.tab_logs)),
        )
    DetailNavigationScaffold(pagerState, navRoutes, topBar = { RoomTopBar(id) }) { pageIndex ->
        when (pageIndex) {
            0 -> RoomInfoTabs(id)
            else -> RoomLogsTab(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoomTopBar(id: PrimaryKey) {
    val vm = createPanelRoomViewModel(id)
    val info by vm.handler.data.collectAsState(null)
    val name = info?.name
    val privateRoom = info?.isPrivate == true
    val titleResource = if (privateRoom) Res.string.private_room_detail_title else Res.string.room_detail_title
    val title = stringResource(titleResource)
    val nav = LocalPanelNav.current
    TopAppBar(
        title = { PanelDetailHeading(title, name) },
        navigationIcon = {
            IconButton(onClick = { nav.open() }) {
                Icon(actionIcon(ActionGlyph.MENU), stringResource(Res.string.menu))
            }
        },
    )
}

@Composable
private fun RoomInfoTabs(id: PrimaryKey) {
    val tabs =
        listOf(
            stringResource(Res.string.tab_basic_info),
            stringResource(Res.string.tab_members),
            stringResource(Res.string.tab_files),
        )
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    Column {
        ReadingTabRow(wrap = true) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = pagerState.currentPage == i, onClick = {
                    scope.launch { pagerState.scrollToPage(i) }
                }) {
                    Text(label)
                }
            }
        }
        HorizontalPager(pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            when (pageIndex) {
                0 -> RoomBasicInfoSection(id)
                1 -> RoomMembersTab(id)
                else -> RoomFilesTab(id)
            }
        }
    }
}

@Composable
private fun RoomBasicInfoSection(id: PrimaryKey) {
    val vm = createPanelRoomViewModel(id)
    val dialogController = LocalPanelGlobalDialog.current
    val scope = rememberCoroutineScope()
    StateView(vm.handler, modifier = Modifier.fillMaxSize()) { info ->
        val items =
            buildList {
                add("id" to info.id.toString())
                add("name" to info.name)
                add("aid" to info.aid)
                add("creator" to info.creator.toString())
                add("createdTime" to info.createdTime.formatDisplayTime())
                add("memberCount" to info.memberCount.toString())
                add("icon" to (info.icon?.name ?: "null"))
                add("joinedTime" to (info.joinedTime?.formatDisplayTime() ?: "null"))
                add("communityId" to panelOptionalValueText(info.communityId?.toString()))
                add("latestTopic" to (info.latestTopic?.toString() ?: "null"))
                add("isPrivate" to info.isPrivate.toString())
                add("isJoined" to info.isJoined.toString())
                add("hasUnread" to info.hasUnread.toString())
                add("readOnly" to info.readOnly.toString())
            }
        Column {
            PanelInfoTable(items, Modifier.padding(16.dp).weight(1f))
            Button(onClick = {
                val newStatus = if (info.readOnly) ObjectStatus.NORMAL else ObjectStatus.READ_ONLY
                scope.launch {
                    dialogController.useResult {
                        context.request {
                            updateRoomStatus(id, UpdateObjectStatusBody(newStatus))
                        }
                    }.onSuccess {
                        dialogController.emitEvent(OnRoomStatusUpdated(id, newStatus))
                    }
                }
            }, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(if (info.readOnly) Res.string.set_normal else Res.string.set_read_only))
            }
        }
    }
}

@Composable
private fun RoomLogsTab(id: PrimaryKey) {
    PanelLogsTab(id, ObjectType.ROOM)
}

@Composable
private fun RoomMembersTab(id: PrimaryKey) {
    val nav = LocalPanelNav.current
    val vm = createPanelRoomMembersViewModel(id)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.userInfo.id }) { index ->
                val m = items[index]
                if (m != null) {
                    ListItem(
                        modifier = Modifier.clickable { nav.gotoUserDetail(m.userInfo.id) },
                        headlineContent = { Text(m.userInfo.nickname) },
                        supportingContent = {
                            val joined = m.joinedTime?.formatDisplayTime().orEmpty()
                            val status = panelValueText(m.status.name)
                            Text(listOf(joined, status).joinToString(" • "))
                        },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun RoomFilesTab(id: PrimaryKey) {
    val nav = LocalPanelNav.current
    val vm = createPanelRoomFilesViewModel(id)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    ListItem(
                        modifier = Modifier.clickable { nav.gotoFileDetail(info.id) },
                        headlineContent = { Text(info.name) },
                        supportingContent = {
                            val ct = info.contentType
                            val s = listOf(ct).filter { it.isNotEmpty() }.joinToString(" • ")
                            Text(s)
                        },
                        leadingContent = { Icon(actionIcon(ActionGlyph.FILE), null) },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}
