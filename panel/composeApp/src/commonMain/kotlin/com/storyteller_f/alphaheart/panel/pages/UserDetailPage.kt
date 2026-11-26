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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CommunityIcon
import com.storyteller_f.alphaheart.client.compose_core.components.FileIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow
import com.storyteller_f.alphaheart.client.compose_core.components.RoomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.alphaheart.client.core.updateUserStatus
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OnUserStatusUpdated
import com.storyteller_f.alphaheart.panel.common.PanelLogsTab
import com.storyteller_f.alphaheart.panel.common.createPanelJoinedCommunitiesViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelJoinedRoomsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelUserCommentsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelUserFavoritesViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelUserLogsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelUserOverviewViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelUserReactionsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelUserSubscriptionsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelUserUploadRecordsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelUserViewModel
import com.storyteller_f.alphaheart.panel.components.DetailNavigationScaffold
import com.storyteller_f.alphaheart.panel.components.PanelDetailHeading
import com.storyteller_f.alphaheart.panel.components.PanelInfoTable
import com.storyteller_f.alphaheart.panel.components.TopicCell
import com.storyteller_f.alphaheart.panel.components.panelOptionalValueText
import com.storyteller_f.alphaheart.panel.components.panelUserLogText
import com.storyteller_f.alphaheart.panel.components.panelValueText
import com.storyteller_f.alphaheart.panel.file_progress
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.review_user_log_supporting
import com.storyteller_f.alphaheart.panel.set_normal
import com.storyteller_f.alphaheart.panel.set_read_only
import com.storyteller_f.alphaheart.panel.tab_basic_info
import com.storyteller_f.alphaheart.panel.tab_created_files
import com.storyteller_f.alphaheart.panel.tab_joined_communities
import com.storyteller_f.alphaheart.panel.tab_joined_rooms
import com.storyteller_f.alphaheart.panel.tab_logs
import com.storyteller_f.alphaheart.panel.tab_received_titles
import com.storyteller_f.alphaheart.panel.tab_user_comments
import com.storyteller_f.alphaheart.panel.tab_user_favorites
import com.storyteller_f.alphaheart.panel.tab_user_reactions
import com.storyteller_f.alphaheart.panel.tab_user_subscriptions
import com.storyteller_f.alphaheart.panel.upload_records
import com.storyteller_f.alphaheart.panel.user_detail_title
import com.storyteller_f.alphaheart.panel.user_info
import com.storyteller_f.alphaheart.panel.user_logs
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.UpdateUserStatusBody
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import kotlinx.coroutines.launch
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingHorizontalPager as HorizontalPager
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDetailPage(uid: PrimaryKey) {
    val pagerState = rememberPagerState { 3 }
    val navRoutes =
        listOf(
            NavRoute("/info", navigationIcon(NavigationGlyph.USERS), stringResource(Res.string.user_info)),
            NavRoute("/logs", navigationIcon(NavigationGlyph.FILE_TEXT), stringResource(Res.string.user_logs)),
            NavRoute("/panel-logs", navigationIcon(NavigationGlyph.SCROLL_TEXT), stringResource(Res.string.tab_logs)),
        )
    DetailNavigationScaffold(pagerState, navRoutes, topBar = { UserTopBar(uid) }) { pageIndex ->
        when (pageIndex) {
            0 -> UserInfoTabs(uid)
            1 -> UserLogsTab(uid)
            else -> UserPanelLogsTab(uid)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserTopBar(uid: PrimaryKey) {
    val vm = createPanelUserViewModel(uid)
    val info by vm.handler.data.collectAsState(null)
    val nickname = info?.nickname
    val title = stringResource(Res.string.user_detail_title)
    val nav = LocalPanelNav.current
    TopAppBar(
        title = { PanelDetailHeading(title, nickname) },
        navigationIcon = {
            IconButton(onClick = { nav.open() }) {
                Icon(actionIcon(ActionGlyph.MENU), stringResource(Res.string.menu))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserInfoTabs(uid: PrimaryKey) {
    val tabs =
        listOf(
            stringResource(Res.string.tab_basic_info),
            stringResource(Res.string.tab_joined_communities),
            stringResource(Res.string.tab_joined_rooms),
            stringResource(Res.string.tab_received_titles),
            stringResource(Res.string.tab_created_files),
            stringResource(Res.string.upload_records),
            stringResource(Res.string.tab_user_reactions),
            stringResource(Res.string.tab_user_comments),
            stringResource(Res.string.tab_user_favorites),
            stringResource(Res.string.tab_user_subscriptions),
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
        HorizontalPager(pagerState, modifier = Modifier.weight(1f)) { index ->
            when (index) {
                0 -> UserBasicInfoSection(uid)
                1 -> UserJoinedCommunitiesSection(uid)
                2 -> UserJoinedRoomsSection(uid)
                3 -> UserReceivedTitlesSection(uid)
                4 -> UserCreatedFilesSection(uid)
                5 -> UserUploadRecordsSection(uid)
                6 -> UserReactionsSection(uid)
                7 -> UserCommentsSection(uid)
                8 -> UserFavoritesSection(uid)
                else -> UserSubscriptionsSection(uid)
            }
        }
    }
}

@Composable
private fun UserJoinedCommunitiesSection(uid: PrimaryKey) {
    val vm = createPanelJoinedCommunitiesViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    ListItem(
                        leadingContent = {
                            CommunityIcon(info, 40.dp, false) {}
                        },
                        headlineContent = { Text(info.name) },
                        supportingContent = { Text(CoreStrings.aid(info.aid)) },
                        modifier = Modifier.clickable { panelNav.gotoCommunityDetail(info.id) },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserJoinedRoomsSection(uid: PrimaryKey) {
    val vm = createPanelJoinedRoomsViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    ListItem(
                        leadingContent = {
                            RoomIcon(info, 40.dp, false) {}
                        },
                        headlineContent = { Text(info.name) },
                        supportingContent = { Text(CoreStrings.aid(info.aid)) },
                        modifier = Modifier.clickable { panelNav.gotoRoomDetail(info.id) },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserReceivedTitlesSection(uid: PrimaryKey) {
    val vm = com.storyteller_f.alphaheart.panel.common.createPanelReceivedTitlesViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    ListItem(
                        headlineContent = { Text(info.name) },
                        supportingContent = { Text(panelValueText(info.type.name)) },
                        modifier = Modifier.clickable { panelNav.gotoTitleDetail(info.id) },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserCreatedFilesSection(uid: PrimaryKey) {
    val vm = com.storyteller_f.alphaheart.panel.common.createPanelUserFilesViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    ListItem(
                        headlineContent = { Text(info.name) },
                        supportingContent = { Text(HumanReadable.fileSize(info.size)) },
                        leadingContent = { FileIcon(info) },
                        modifier =
                        Modifier.clickable { panelNav.gotoFileDetail(info.id) }.semantics(mergeDescendants = true) {
                            contentDescription =
                                listOf(info.name, info.contentType, HumanReadable.fileSize(info.size))
                                    .joinToString(", ")
                        },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserUploadRecordsSection(uid: PrimaryKey) {
    val vm = createPanelUserUploadRecordsViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    ListItem(
                        headlineContent = { Text(info.name) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    Res.string.file_progress,
                                    panelValueText(info.status.name),
                                    HumanReadable.fileSize(info.progress),
                                    HumanReadable.fileSize(info.total),
                                ),
                            )
                        },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserLogsTab(uid: PrimaryKey) {
    val vm = createPanelUserLogsViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    ListItem(
                        headlineContent = { Text(panelUserLogText(info.type)) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    Res.string.review_user_log_supporting,
                                    panelValueText(info.objectType.name),
                                    info.objectId.toString(),
                                    info.createdTime.formatDisplayTime(),
                                ),
                            )
                        },
                        modifier =
                        Modifier.clickable {
                            when (info.objectType) {
                                ObjectType.USER -> panelNav.gotoUserDetail(info.objectId)
                                ObjectType.COMMUNITY -> panelNav.gotoCommunityDetail(info.objectId)
                                ObjectType.ROOM -> panelNav.gotoRoomDetail(info.objectId)
                                ObjectType.TOPIC -> panelNav.gotoTopicDetail(info.objectId)
                                ObjectType.TITLE -> panelNav.gotoTitleDetail(info.objectId)
                                ObjectType.FILE -> panelNav.gotoFileDetail(info.objectId)
                                else -> {}
                            }
                        },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserPanelLogsTab(uid: PrimaryKey) {
    PanelLogsTab(uid, ObjectType.USER)
}

@Composable
private fun UserBasicInfoSection(uid: PrimaryKey) {
    val vm = createPanelUserOverviewViewModel(uid)
    val dialogController = LocalPanelGlobalDialog.current
    val scope = rememberCoroutineScope()
    StateView(vm.handler, modifier = Modifier.fillMaxSize()) { overview ->
        val items =
            buildList {
                add("id" to overview.userInfo.id.toString())
                add("nickname" to overview.userInfo.nickname)
                add("status" to overview.userInfo.status.name)
                add("aid" to panelOptionalValueText(overview.userInfo.aid))
                add("address" to overview.userInfo.address)
                add("favoriteCount" to overview.favoriteCount.toString())
                add("subscriptionCount" to overview.subscriptionCount.toString())
                add("acg" to overview.acg.toString())
                add("reactionRecordCount" to overview.reactionRecordCount.toString())
                add("commentCount" to overview.commentCount.toString())
                add("childAccountCount" to overview.childAccountCount.toString())
            }
        val newStatus = if (overview.userInfo.status == UserStatus.NORMAL) UserStatus.READ_ONLY else UserStatus.NORMAL
        val updateStatus = {
            scope.launch {
                dialogController.useResult {
                    context.request { updateUserStatus(uid, UpdateUserStatusBody(newStatus)) }
                }.onSuccess {
                    dialogController.emitEvent(OnUserStatusUpdated(uid, newStatus))
                }
            }
            Unit
        }
        val action =
            stringResource(if (newStatus == UserStatus.READ_ONLY) Res.string.set_read_only else Res.string.set_normal)
        Column {
            PanelInfoTable(items, Modifier.padding(16.dp).weight(1f))
            Button(
                onClick = updateStatus,
                modifier =
                Modifier.padding(16.dp).appiumSemantics(
                    text = action,
                    onClick = updateStatus,
                ),
            ) {
                Text(action)
            }
        }
    }
}

@Composable
private fun UserReactionsSection(uid: PrimaryKey) {
    val vm = createPanelUserReactionsViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    ListItem(
                        headlineContent = {
                            Text("${info.emoji} · ${panelValueText(info.objectType.name)} ${info.objectId}")
                        },
                        supportingContent = {
                            Text("${panelValueText(info.objectType.name)} · ${info.createdTime.formatDisplayTime()}")
                        },
                        modifier = Modifier.clickable { panelNav.gotoTopicDetail(info.objectId) },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserCommentsSection(uid: PrimaryKey) {
    val vm = createPanelUserCommentsViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    TopicCell(info, panelNav)
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserFavoritesSection(uid: PrimaryKey) {
    val vm = createPanelUserFavoritesViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    ListItem(
                        headlineContent = { Text("${panelValueText(info.objectType.name)} ${info.objectId}") },
                        supportingContent = {
                            Text(
                                info.createdTime.formatDisplayTime(),
                            )
                        },
                        modifier =
                        Modifier.clickable {
                            when (info.objectType) {
                                ObjectType.USER -> panelNav.gotoUserDetail(info.objectId)
                                ObjectType.COMMUNITY -> panelNav.gotoCommunityDetail(info.objectId)
                                ObjectType.ROOM -> panelNav.gotoRoomDetail(info.objectId)
                                ObjectType.TOPIC -> panelNav.gotoTopicDetail(info.objectId)
                                ObjectType.TITLE -> panelNav.gotoTitleDetail(info.objectId)
                                ObjectType.FILE -> panelNav.gotoFileDetail(info.objectId)
                                else -> {}
                            }
                        },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}

@Composable
private fun UserSubscriptionsSection(uid: PrimaryKey) {
    val vm = createPanelUserSubscriptionsViewModel(uid)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    val panelNav = LocalPanelNav.current
                    ListItem(
                        headlineContent = { Text("${panelValueText(info.objectType.name)} ${info.objectId}") },
                        supportingContent = {
                            Text(
                                info.createdTime.formatDisplayTime(),
                            )
                        },
                        modifier =
                        Modifier.clickable {
                            when (info.objectType) {
                                ObjectType.USER -> panelNav.gotoUserDetail(info.objectId)
                                ObjectType.COMMUNITY -> panelNav.gotoCommunityDetail(info.objectId)
                                ObjectType.ROOM -> panelNav.gotoRoomDetail(info.objectId)
                                ObjectType.TOPIC -> panelNav.gotoTopicDetail(info.objectId)
                                ObjectType.TITLE -> panelNav.gotoTitleDetail(info.objectId)
                                ObjectType.FILE -> panelNav.gotoFileDetail(info.objectId)
                                else -> {}
                            }
                        },
                    )
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                } else {
                    ListItem(headlineContent = { Text("") })
                    if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                }
            }
        }
    }
}
