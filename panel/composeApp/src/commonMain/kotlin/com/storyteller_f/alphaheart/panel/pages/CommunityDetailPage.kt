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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import com.storyteller_f.alphaheart.client.compose_core.components.DetailNavigationScaffold
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.components.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.alphaheart.client.core.updateCommunityStatus
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OnCommunityStatusUpdated
import com.storyteller_f.alphaheart.panel.common.PanelLogsTab
import com.storyteller_f.alphaheart.panel.common.createPanelCommunityMembersViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelCommunityViewModel
import com.storyteller_f.alphaheart.panel.community_detail_title
import com.storyteller_f.alphaheart.panel.community_detail_title_with_info
import com.storyteller_f.alphaheart.panel.components.PanelInfoTable
import com.storyteller_f.alphaheart.panel.components.panelValueText
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.set_normal
import com.storyteller_f.alphaheart.panel.set_read_only
import com.storyteller_f.alphaheart.panel.tab_basic_info
import com.storyteller_f.alphaheart.panel.tab_info
import com.storyteller_f.alphaheart.panel.tab_logs
import com.storyteller_f.alphaheart.panel.tab_members
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.UpdateObjectStatusBody
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingHorizontalPager as HorizontalPager
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow as PrimaryScrollableTabRow

private val compactJson = Json { prettyPrint = false }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDetailPage(id: PrimaryKey) {
    val pagerState = rememberPagerState { 2 }
    val navRoutes =
        listOf(
            NavRoute("/info", navigationIcon(NavigationGlyph.INFO), stringResource(Res.string.tab_info)),
            NavRoute("/logs", navigationIcon(NavigationGlyph.FILE_TEXT), stringResource(Res.string.tab_logs)),
        )
    DetailNavigationScaffold(pagerState, navRoutes, topBar = { CommunityTopBar(id) }) { pageIndex ->
        when (pageIndex) {
            0 -> CommunityInfoTabs(id)
            else -> CommunityLogsTab(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommunityTopBar(id: PrimaryKey) {
    val vm = createPanelCommunityViewModel(id)
    val info by vm.handler.data.collectAsState(null)
    val name = info?.name
    val aid = info?.aid
    val title =
        if (name != null && aid != null) {
            stringResource(Res.string.community_detail_title_with_info, name, aid)
        } else {
            stringResource(Res.string.community_detail_title)
        }
    val nav = LocalPanelNav.current
    TopAppBar(
        title = {
            Text(title)
        },
        navigationIcon = {
            IconButton(onClick = { nav.open() }) {
                Icon(Icons.Default.Menu, stringResource(Res.string.menu))
            }
        },
    )
}

@Composable
private fun CommunityInfoTabs(id: PrimaryKey) {
    val tabs = listOf(stringResource(Res.string.tab_basic_info), stringResource(Res.string.tab_members))
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    Column {
        PrimaryScrollableTabRow {
            tabs.forEachIndexed { i, label ->
                Tab(selected = pagerState.currentPage == i, onClick = {
                    scope.launch { pagerState.scrollToPage(i) }
                }) {
                    Text(label, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
        HorizontalPager(pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            when (pageIndex) {
                0 -> CommunityBasicInfoSection(id)
                else -> CommunityMembersTab(id)
            }
        }
    }
}

@Composable
private fun CommunityBasicInfoSection(id: PrimaryKey) {
    val vm = createPanelCommunityViewModel(id)
    val dialogController = LocalPanelGlobalDialog.current
    val scope = rememberCoroutineScope()
    StateView(vm.handler, modifier = Modifier.fillMaxSize()) { info ->
        val items =
            buildList {
                add("id" to info.id.toString())
                add("name" to info.name)
                add("aid" to info.aid)
                add("owner" to info.owner.toString())
                add("createdTime" to info.createdTime.formatDisplayTime())
                add("memberCount" to info.memberCount.toString())
                add("memberPolicy" to info.memberPolicy.name)
                add("icon" to (info.icon?.name ?: "null"))
                add("poster" to (info.poster?.name ?: "null"))
                add("latestTopic" to (info.latestTopic?.toString() ?: "null"))
                add("hasPoster" to info.hasPoster.toString())
                add(
                    "fontSettings" to (
                        info.fontSettings?.let { fontSettings ->
                            compactJson.encodeToString(
                                com.storyteller_f.shared.model.FontSettings.serializer(),
                                fontSettings.settings,
                            )
                        } ?: "null"
                        ),
                )
                add("readOnly" to info.readOnly.toString())
            }
        Column {
            PanelInfoTable(items, Modifier.padding(16.dp).weight(1f))
            Button(onClick = {
                val newStatus = if (info.readOnly) ObjectStatus.NORMAL else ObjectStatus.READ_ONLY
                scope.launch {
                    dialogController.useResult {
                        context.request {
                            updateCommunityStatus(id, UpdateObjectStatusBody(newStatus))
                        }
                    }.onSuccess {
                        dialogController.emitEvent(OnCommunityStatusUpdated(id, newStatus))
                    }
                }
            }, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(if (info.readOnly) Res.string.set_normal else Res.string.set_read_only))
            }
        }
    }
}

@Composable
private fun CommunityLogsTab(id: PrimaryKey) {
    PanelLogsTab(id, ObjectType.COMMUNITY)
}

@Composable
private fun CommunityMembersTab(id: PrimaryKey) {
    val nav = LocalPanelNav.current
    val vm = createPanelCommunityMembersViewModel(id)
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
