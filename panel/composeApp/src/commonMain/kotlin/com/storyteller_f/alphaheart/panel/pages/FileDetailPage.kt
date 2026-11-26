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
import androidx.paging.compose.LazyPagingItems
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.storyteller_f.alphaheart.client.compose_core.components.PdfView
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.globalLoader
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.updateFileStatus
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.OnFileStatusUpdated
import com.storyteller_f.alphaheart.panel.common.PanelLogsTab
import com.storyteller_f.alphaheart.panel.common.createPanelFileRefsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelFileViewModel
import com.storyteller_f.alphaheart.panel.components.DetailNavigationScaffold
import com.storyteller_f.alphaheart.panel.components.PanelInfoTable
import com.storyteller_f.alphaheart.panel.components.panelValueText
import com.storyteller_f.alphaheart.panel.file.fileBasicInfoItems
import com.storyteller_f.alphaheart.panel.file_can_preview
import com.storyteller_f.alphaheart.panel.file_detail_title
import com.storyteller_f.alphaheart.panel.file_detail_title_with_info
import com.storyteller_f.alphaheart.panel.file_preview_title
import com.storyteller_f.alphaheart.panel.fullscreen_preview
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.preview_image
import com.storyteller_f.alphaheart.panel.review_author_reference
import com.storyteller_f.alphaheart.panel.set_normal
import com.storyteller_f.alphaheart.panel.set_read_only
import com.storyteller_f.alphaheart.panel.tab_basic_info_file
import com.storyteller_f.alphaheart.panel.tab_file_refs
import com.storyteller_f.alphaheart.panel.tab_info
import com.storyteller_f.alphaheart.panel.tab_logs
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.FileRefInfo
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
fun FileDetailPage(id: PrimaryKey) {
    val pagerState = rememberPagerState { 2 }
    val navRoutes =
        listOf(
            NavRoute("/info", navigationIcon(NavigationGlyph.INFO), stringResource(Res.string.tab_info)),
            NavRoute("/logs", navigationIcon(NavigationGlyph.FILE_TEXT), stringResource(Res.string.tab_logs)),
        )
    DetailNavigationScaffold(pagerState, navRoutes, topBar = { FileTopBar(id) }) { pageIndex ->
        when (pageIndex) {
            0 -> FileInfoTabs(id)
            else -> FileLogsTab(id)
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
                Icon(actionIcon(ActionGlyph.MENU), stringResource(Res.string.menu))
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
            val audio = info.contentType.startsWith("audio/")
            IconButton(
                onClick = {
                    nav.gotoFilePreview(info.id, info.url, info.contentType, info.name)
                },
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Icon(
                    actionIcon(if (audio) ActionGlyph.EXTERNAL_LINK else ActionGlyph.MAXIMIZE_2),
                    stringResource(if (audio) Res.string.file_preview_title else Res.string.fullscreen_preview),
                )
            }
            PanelInfoTable(fileBasicInfoItems(info), Modifier.weight(1f))
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

@Composable
private fun FileReadOnlyToggleButton(readOnly: Boolean, onToggle: (Boolean) -> Unit) {
    val label = stringResource(if (readOnly) Res.string.set_normal else Res.string.set_read_only)
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
        PanelFileReferenceList(fileRefs) { ref ->
            when (ref.objectType) {
                ObjectType.COMMUNITY -> nav.gotoCommunityDetail(ref.objectId)
                ObjectType.ROOM -> nav.gotoRoomDetail(ref.objectId)
                ObjectType.TOPIC -> nav.gotoTopicDetail(ref.objectId)
                ObjectType.USER -> nav.gotoUserDetail(ref.objectId)
                else -> {}
            }
        }
    }
}

@Composable
internal fun PanelFileReferenceList(fileRefs: LazyPagingItems<FileRefInfo>, navigate: (FileRefInfo) -> Unit) {
    LazyColumn {
        pagingItems(fileRefs, key = { it.id }) { index ->
            val ref = fileRefs[index]
            ListItem(
                modifier = Modifier.clickable { ref?.let(navigate) },
                headlineContent = {
                    val type = panelValueText(ref?.objectType?.name ?: "Unknown")
                    val objectId = ref?.objectId?.toString() ?: panelValueText("Unknown")
                    Text("$type #$objectId")
                },
                supportingContent = {
                    val author = ref?.author?.toString() ?: panelValueText("Unknown")
                    Text(stringResource(Res.string.review_author_reference, author))
                },
            )
            if (index < fileRefs.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
        }
    }
}
