/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.storyteller_f.alphaheart.client.compose_core.components.CustomBottomNav
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.components.PdfView
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.globalLoader
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.updateFileStatus
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OnFileStatusUpdated
import com.storyteller_f.alphaheart.panel.common.PanelLogsTab
import com.storyteller_f.alphaheart.panel.common.createPanelFileRefsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelFileViewModel
import com.storyteller_f.alphaheart.panel.components.InfoTable
import com.storyteller_f.alphaheart.panel.file_can_preview
import com.storyteller_f.alphaheart.panel.file_detail_title
import com.storyteller_f.alphaheart.panel.file_detail_title_with_info
import com.storyteller_f.alphaheart.panel.fullscreen_preview
import com.storyteller_f.alphaheart.panel.preview_image
import com.storyteller_f.alphaheart.panel.tab_basic_info_file
import com.storyteller_f.alphaheart.panel.tab_file_refs
import com.storyteller_f.alphaheart.panel.tab_info
import com.storyteller_f.alphaheart.panel.tab_logs
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.UpdateObjectStatusBody
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileDetailPage(id: PrimaryKey) {
    val pagerState = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    Scaffold(topBar = { FileTopBar(id) }, bottomBar = {
        val navRoutes =
            listOf(
                NavRoute("/info", Icons.Default.FilePresent, stringResource(Res.string.tab_info)),
                NavRoute("/logs", Icons.AutoMirrored.Filled.Article, stringResource(Res.string.tab_logs)),
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
                    0 -> FileInfoTabs(id)
                    else -> FileLogsTab(id)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileTopBar(id: PrimaryKey) {
    val vm = createPanelFileViewModel(id)
    val info by vm.handler.data.collectAsState(null)
    val name = info?.name
    val title =
        if (name != null) {
            stringResource(Res.string.file_detail_title_with_info, name)
        } else {
            stringResource(Res.string.file_detail_title)
        }
    val nav = LocalPanelNav.current
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
private fun FileInfoTabs(id: PrimaryKey) {
    val tabs = listOf(stringResource(Res.string.tab_basic_info_file), stringResource(Res.string.tab_file_refs))
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
        HorizontalPager(pagerState, modifier = Modifier.weight(1f)) { pageIndex ->
            when (pageIndex) {
                0 -> FileBasicInfoSection(id)
                1 -> FileRefsSection(id)
                else -> FileBasicInfoSection(id)
            }
        }
    }
}

@Composable
private fun FileBasicInfoSection(id: PrimaryKey) {
    val vm = createPanelFileViewModel(id)
    val nav = LocalPanelNav.current
    val dialogController = LocalPanelGlobalDialog.current
    val scope = rememberCoroutineScope()
    StateView(vm.handler, modifier = Modifier.fillMaxSize()) { info ->
        Column(Modifier.padding(16.dp)) {
            Box(modifier = Modifier.heightIn(max = 200.dp).padding(top = 16.dp)) {
                when {
                    info.contentType.startsWith("image") -> {
                        CoilZoomAsyncImage(
                            model = globalLoader(info.url),
                            contentDescription = stringResource(Res.string.preview_image),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    info.contentType == com.storyteller_f.shared.model.FileInfo.PDF_CONTENT_TYPE -> {
                        PdfView(info.url, Modifier.fillMaxSize())
                    }

                    info.contentType.startsWith("video") || info.contentType.startsWith("audio") -> {
                        Text(stringResource(Res.string.file_can_preview))
                    }
                }
            }
            Button(
                onClick = {
                    nav.gotoFilePreview(info.id, info.url, info.contentType, info.name)
                },
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(Res.string.fullscreen_preview))
            }
            InfoTable(fileBasicInfoItems(info))
            FileReadOnlyToggleButton(info.readOnly) { newValue ->
                val newStatus = if (newValue) ObjectStatus.READ_ONLY else ObjectStatus.NORMAL
                scope.launch {
                    dialogController.useResult {
                        context.request {
                            updateFileStatus(id, UpdateObjectStatusBody(newStatus))
                        }
                    }.onSuccess {
                        dialogController.emitEvent(OnFileStatusUpdated(id, newStatus))
                    }
                }
            }
        }
    }
}

private fun fileBasicInfoItems(info: com.storyteller_f.shared.model.FileInfo): List<Pair<String, String>> =
    buildList {
        add("id" to info.id.toString())
        add("name" to info.name)
        add("fullName" to info.fullName)
        add("url" to info.url)
        add("contentType" to info.contentType)
        add("size" to info.size.toString())
        add("owner" to info.owner.toString())
        add("ownerType" to info.ownerType.name)
        add("lastModified" to info.lastModified.toString())
        add("dimension" to (info.dimension?.let { "${it.width}x${it.height}" } ?: "null"))
        add("readOnly" to info.readOnly.toString())
    }

@Composable
private fun FileReadOnlyToggleButton(readOnly: Boolean, onToggle: (Boolean) -> Unit) {
    val label = if (readOnly) "Set to Writable" else "Set to ReadOnly"
    val toggle = { onToggle(!readOnly) }
    Button(
        onClick = toggle,
        modifier =
        Modifier.padding(top = 16.dp)
            .appiumSemantics(text = label, onClick = toggle),
    ) {
        Text(label)
    }
}

@Composable
private fun FileLogsTab(id: PrimaryKey) {
    PanelLogsTab(id, ObjectType.FILE)
}

@Composable
private fun FileRefsSection(id: PrimaryKey) {
    val nav = LocalPanelNav.current
    val vm = createPanelFileRefsViewModel(id)
    StateView(vm, modifier = Modifier.fillMaxSize()) { fileRefs ->
        LazyColumn {
            pagingItems(fileRefs, key = { it.fileId }) { index ->
                val ref = fileRefs[index]
                ListItem(
                    modifier =
                    Modifier.clickable {
                        when (ref?.objectType) {
                            com.storyteller_f.shared.type.ObjectType.COMMUNITY ->
                                nav.gotoCommunityDetail(ref.objectId)

                            com.storyteller_f.shared.type.ObjectType.ROOM ->
                                nav.gotoRoomDetail(ref.objectId)

                            com.storyteller_f.shared.type.ObjectType.TOPIC ->
                                nav.gotoTopicDetail(ref.objectId)

                            com.storyteller_f.shared.type.ObjectType.USER ->
                                nav.gotoUserDetail(ref.objectId)

                            else -> {}
                        }
                    },
                    headlineContent = {
                        Text("${ref?.objectType?.name ?: "Unknown"} #${ref?.objectId ?: "Unknown"}")
                    },
                    supportingContent = {
                        Text("Author: ${ref?.author ?: "Unknown"}")
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
