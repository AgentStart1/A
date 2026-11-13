/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.CustomBottomNav
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.core.updateTitleStatus
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OnTitleStatusUpdated
import com.storyteller_f.alphaheart.panel.common.PanelLogsTab
import com.storyteller_f.alphaheart.panel.common.createPanelTitleViewModel
import com.storyteller_f.alphaheart.panel.components.InfoTable
import com.storyteller_f.alphaheart.panel.tab_basic_info
import com.storyteller_f.alphaheart.panel.title_detail_title
import com.storyteller_f.alphaheart.panel.title_detail_title_with_info
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.UpdateObjectStatusBody
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleDetailPage(id: PrimaryKey) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { TitleTopBar(id) }, bottomBar = {
        val navRoutes =
            listOf(
                NavRoute("/info", Icons.Default.Title, "Info"),
                NavRoute("/logs", Icons.AutoMirrored.Filled.Article, "Logs"),
            )
        CustomBottomNav(navRoutes[pagerState.currentPage].path, navRoutes) { path ->
            scope.launch {
                pagerState.animateScrollToPage(navRoutes.indexOfFirst { it.path == path })
            }
        }
    }) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            HorizontalPager(pagerState) { pageIndex ->
                when (pageIndex) {
                    0 -> TitleInfoTabs(id)
                    else -> TitleLogsTab(id)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TitleTopBar(id: PrimaryKey) {
    val vm = createPanelTitleViewModel(id)
    val info by vm.handler.data.collectAsState(null)
    val title =
        if (info?.name != null) {
            stringResource(Res.string.title_detail_title_with_info, id)
        } else {
            stringResource(Res.string.title_detail_title)
        }
    val nav = com.storyteller_f.alphaheart.panel.LocalPanelNav.current
    TopAppBar(
        title = {
            Text(title)
        },
        navigationIcon = {
            IconButton(onClick = { nav.open() }) {
                Icon(Icons.Default.Menu, null)
            }
        },
    )
}

@Composable
private fun TitleInfoTabs(id: PrimaryKey) {
    val tabs = listOf(stringResource(Res.string.tab_basic_info))
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()
    Column {
        PrimaryScrollableTabRow(pagerState.currentPage) {
            tabs.forEachIndexed { i, label ->
                Tab(selected = pagerState.currentPage == i, onClick = {
                    scope.launch { pagerState.scrollToPage(i) }
                }) {
                    Text(label, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
        HorizontalPager(pagerState, modifier = Modifier.weight(1f)) { _ ->
            TitleBasicInfoSection(id)
        }
    }
}

@Composable
private fun TitleBasicInfoSection(id: PrimaryKey) {
    val vm = createPanelTitleViewModel(id)
    val dialogController = LocalPanelGlobalDialog.current
    val scope = rememberCoroutineScope()
    StateView(vm.handler, modifier = Modifier.fillMaxSize()) { info ->
        val items =
            buildList {
                add("id" to info.id.toString())
                add("name" to info.name)
                add("createdTime" to info.createdTime.toString())
                add("type" to info.type.name)
                add("creator" to info.creator.toString())
                add("receiver" to info.receiver.toString())
                add("scopeId" to info.scopeId.toString())
                add("scopeType" to info.scopeType.name)
                add("descriptionTopicId" to info.descriptionTopicId.toString())
                add("readOnly" to info.readOnly.toString())
            }
        Column {
            InfoTable(items, Modifier.padding(16.dp).weight(1f))
            Button(onClick = {
                val newStatus = if (info.readOnly) ObjectStatus.NORMAL else ObjectStatus.READ_ONLY
                scope.launch {
                    dialogController.useResult {
                        context.request {
                            updateTitleStatus(id, UpdateObjectStatusBody(newStatus))
                        }
                    }.onSuccess {
                        dialogController.emitEvent(OnTitleStatusUpdated(id, newStatus))
                    }
                }
            }, modifier = Modifier.padding(16.dp)) {
                Text(if (info.readOnly) "Set to Writable" else "Set to ReadOnly")
            }
        }
    }
}

@Composable
private fun TitleLogsTab(id: PrimaryKey) {
    PanelLogsTab(id, ObjectType.TITLE)
}
