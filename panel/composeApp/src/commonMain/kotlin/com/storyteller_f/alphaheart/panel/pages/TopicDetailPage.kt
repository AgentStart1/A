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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.alphaheart.client.core.updateTopicStatus
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OnTopicStatusUpdated
import com.storyteller_f.alphaheart.panel.common.PanelLogsTab
import com.storyteller_f.alphaheart.panel.common.PanelNav
import com.storyteller_f.alphaheart.panel.common.createPanelTopicTopicsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelTopicViewModel
import com.storyteller_f.alphaheart.panel.common.gotoChildTopicDetail
import com.storyteller_f.alphaheart.panel.components.DetailNavigationScaffold
import com.storyteller_f.alphaheart.panel.components.PanelInfoTable
import com.storyteller_f.alphaheart.panel.components.panelOptionalValueText
import com.storyteller_f.alphaheart.panel.encrypted
import com.storyteller_f.alphaheart.panel.interaction
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.pinned
import com.storyteller_f.alphaheart.panel.set_normal
import com.storyteller_f.alphaheart.panel.set_read_only
import com.storyteller_f.alphaheart.panel.tab_basic_info
import com.storyteller_f.alphaheart.panel.tab_logs
import com.storyteller_f.alphaheart.panel.tab_topics
import com.storyteller_f.alphaheart.panel.topic_detail_title_with_info
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
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
fun TopicDetailPage(id: PrimaryKey) {
    val pagerState = rememberPagerState { 3 }
    val navRoutes =
        listOf(
            NavRoute("/info", navigationIcon(NavigationGlyph.INFO), stringResource(Res.string.tab_basic_info)),
            NavRoute("/topics", navigationIcon(NavigationGlyph.MESSAGES_SQUARE), stringResource(Res.string.tab_topics)),
            NavRoute("/logs", navigationIcon(NavigationGlyph.FILE_TEXT), stringResource(Res.string.tab_logs)),
        )
    DetailNavigationScaffold(pagerState, navRoutes, topBar = { TopicTopBar(id) }) { pageIndex ->
        when (pageIndex) {
            0 -> TopicInfoTabs(id)
            1 -> TopicTopicsTab(id)
            else -> TopicLogsTab(id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicTopBar(id: PrimaryKey) {
    val title = stringResource(Res.string.topic_detail_title_with_info, id)
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
private fun TopicInfoTabs(id: PrimaryKey) {
    val tabs = listOf(stringResource(Res.string.tab_basic_info))
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
        HorizontalPager(pagerState, modifier = Modifier.weight(1f)) { _ ->
            TopicBasicInfoSection(id)
        }
    }
}

@Composable
private fun TopicBasicInfoSection(id: PrimaryKey) {
    val vm = createPanelTopicViewModel(id)
    val dialogController = LocalPanelGlobalDialog.current
    val scope = rememberCoroutineScope()
    StateView(vm.handler, modifier = Modifier.fillMaxSize()) { info ->
        val items =
            buildList {
                add("id" to info.id.toString())
                add("author" to info.author.toString())
                add("rootId" to info.rootId.toString())
                add("rootType" to info.rootType.name)
                add("parentId" to info.parentId.toString())
                add("parentType" to info.parentType.name)
                add("createdTime" to info.createdTime.formatDisplayTime())
                add("commentCount" to info.commentCount.toString())
                add("reactionCount" to info.reactionCount.toString())
                add("hasComment" to info.hasComment.toString())
                add("isEncrypted" to info.isEncrypted.toString())
                add("level" to info.level.toString())
                add("isPin" to info.isPin.toString())
                add("lastModifiedTime" to (info.lastModifiedTime?.formatDisplayTime() ?: "null"))
                add("aid" to panelOptionalValueText(info.aid))
                add("readOnly" to info.readOnly.toString())
            }
        Column {
            PanelInfoTable(items, Modifier.padding(16.dp).weight(1f))
            Button(onClick = {
                val newStatus = if (info.readOnly) ObjectStatus.NORMAL else ObjectStatus.READ_ONLY
                scope.launch {
                    dialogController.useResult {
                        context.request {
                            updateTopicStatus(id, UpdateObjectStatusBody(newStatus))
                        }
                    }.onSuccess {
                        dialogController.emitEvent(OnTopicStatusUpdated(id, newStatus))
                    }
                }
            }, modifier = Modifier.padding(16.dp)) {
                Text(stringResource(if (info.readOnly) Res.string.set_normal else Res.string.set_read_only))
            }
        }
    }
}

@Composable
private fun TopicLogsTab(id: PrimaryKey) {
    PanelLogsTab(id, ObjectType.TOPIC)
}

@Composable
private fun TopicTopicsTab(id: PrimaryKey) {
    val nav = LocalPanelNav.current
    val vm = createPanelTopicTopicsViewModel(id)
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn {
            pagingItems(items, key = { it.id }) { index ->
                val info = items[index]
                if (info != null) {
                    TopicCell(info, nav)
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
private fun TopicCell(info: TopicDetail, panelNav: PanelNav) {
    val text =
        when (val content = info.content) {
            is TopicContent.Plain -> content.plain
            is TopicContent.Extracted -> content.plain
            else -> ""
        }
    val author = info.authorInfo?.nickname ?: info.author.toString()
    val room = info.roomInfo?.name.orEmpty()
    val overline = listOf(author, room).filter { it.isNotEmpty() }.joinToString(" @ ")
    val counts =
        if (info.commentCount > 0 || info.reactionCount > 0) {
            stringResource(Res.string.interaction, info.commentCount, info.reactionCount)
        } else {
            ""
        }
    val flags =
        listOfNotNull(
            if (info.isEncrypted) stringResource(Res.string.encrypted) else null,
            if (info.isPin) stringResource(Res.string.pinned) else null,
        ).joinToString(" • ")
    val supporting =
        listOf(
            info.createdTime.formatDisplayTime(),
            counts,
            flags,
        ).filter { it.isNotEmpty() }.joinToString(" • ")

    ListItem(
        modifier = Modifier.clickable { panelNav.gotoChildTopicDetail(info.id) },
        headlineContent = { Text(text) },
        overlineContent = { Text(overline) },
        supportingContent = {
            Text(supporting)
        },
    )
}
