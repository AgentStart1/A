/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalClientFileProvider
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.DownloadViewModel
import com.storyteller_f.alphaheart.app.common.createMediaListViewModel
import com.storyteller_f.alphaheart.app.common.createUploadViewModel
import com.storyteller_f.alphaheart.app.common.getDownloadListViewModel
import com.storyteller_f.alphaheart.app.common.getDownloadViewModel
import com.storyteller_f.alphaheart.app.community_files
import com.storyteller_f.alphaheart.app.components.ReadingSheet
import com.storyteller_f.alphaheart.app.file.DownloadRecordItemPreviewProvider
import com.storyteller_f.alphaheart.app.file.getDownloadProgress
import com.storyteller_f.alphaheart.app.file.getPercent
import com.storyteller_f.alphaheart.app.file_explorer
import com.storyteller_f.alphaheart.app.files_download_records
import com.storyteller_f.alphaheart.app.files_upload_records
import com.storyteller_f.alphaheart.app.files_uploaded
import com.storyteller_f.alphaheart.app.media.PlatformClientFile
import com.storyteller_f.alphaheart.app.navigation.FileExplorerRoute
import com.storyteller_f.alphaheart.app.pages.search.CustomSearchBar
import com.storyteller_f.alphaheart.app.pages.search.searchScopeName
import com.storyteller_f.alphaheart.app.room_files
import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.alphaheart.app.transfer_completed
import com.storyteller_f.alphaheart.app.transfer_download_details
import com.storyteller_f.alphaheart.app.transfer_field_error
import com.storyteller_f.alphaheart.app.transfer_field_message
import com.storyteller_f.alphaheart.app.transfer_field_path
import com.storyteller_f.alphaheart.app.transfer_field_size
import com.storyteller_f.alphaheart.app.transfer_field_status
import com.storyteller_f.alphaheart.app.transfer_field_url
import com.storyteller_f.alphaheart.app.transfer_pause
import com.storyteller_f.alphaheart.app.transfer_resume
import com.storyteller_f.alphaheart.app.transfer_retry
import com.storyteller_f.alphaheart.app.transfer_start
import com.storyteller_f.alphaheart.app.user_files
import com.storyteller_f.alphaheart.client.compose_core.components.InfoTable
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadStatus
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.collections.immutable.persistentListOf
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerPage(mediaTarget: ObjectTuple) {
    val fileScope = SearchScope.UploadedFiles(mediaTarget.objectId, mediaTarget.objectType)
    val owner = searchScopeName(fileScope)
    val title =
        stringResource(
            when (mediaTarget.objectType) {
                ObjectType.COMMUNITY -> Res.string.community_files
                ObjectType.ROOM -> Res.string.room_files
                ObjectType.USER -> Res.string.user_files
                else -> Res.string.file_explorer
            },
        )
    val backStack = rememberFileExplorerBackStack()
    var showQuota by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    FileExplorerNavigation(
        backStack = backStack,
        routes = fileNavRoutes(mediaTarget),
        search = {
            CustomSearchBar(
                scope = SearchScope.UploadedFiles(mediaTarget.objectId, mediaTarget.objectType),
                title = title,
                subtitle = owner,
                leadingIcon = {},
            )
        },
        floatingActionButton = { FileExplorerFab(mediaTarget, backStack.last()) { showQuota = true } },
    ) { route ->
        when (route) {
            FileExplorerRoute.Uploaded -> UploadedPage(mediaTarget)
            FileExplorerRoute.UploadRecord -> UploadRecordPage(mediaTarget)
            FileExplorerRoute.DownloadRecord -> DownloadRecordPage()
        }
    }
    QuotaSheet(showQuota, sheetState, { showQuota = false }, mediaTarget)
}

@Composable
private fun fileNavRoutes(mediaTarget: ObjectTuple): List<NavRoute> {
    val list =
        mutableListOf(
            NavRoute("/uploaded", navigationIcon(NavigationGlyph.FOLDER), stringResource(Res.string.files_uploaded)),
            NavRoute(
                "/upload-record",
                navigationIcon(NavigationGlyph.CLOUD_UPLOAD),
                stringResource(Res.string.files_upload_records),
            ),
        )
    if (mediaTarget.objectType == ObjectType.USER) {
        list.add(
            NavRoute(
                "/download-record",
                navigationIcon(NavigationGlyph.DOWNLOAD),
                stringResource(Res.string.files_download_records),
            ),
        )
    }
    return list
}

@Composable
private fun FileExplorerFab(mediaTarget: ObjectTuple, current: NavKey, showQuota: () -> Unit) {
    when (current) {
        FileExplorerRoute.Uploaded -> {
            FileQuotaActionButton(showQuota)
        }

        FileExplorerRoute.UploadRecord -> {
            UploadFileActionButton(mediaTarget)
        }
    }
}

@Composable
private fun FileQuotaActionButton(onClick: () -> Unit) {
    FloatingActionButton(onClick) {
        Icon(actionIcon(ActionGlyph.INFO), "quota info")
    }
}

@Composable
private fun UploadFileActionButton(mediaTarget: ObjectTuple) {
    val fileProvider = LocalClientFileProvider.current
    val globalDialogController = LocalGlobalDialog.current
    val openPicker = {
        globalDialogController.launch {
            useResult {
                val f = FileKit.openFilePicker()
                if (f != null) {
                    fileProvider.getUploader()?.upload(persistentListOf(PlatformClientFile(f)), mediaTarget)
                }
                CustomResult.success(Unit)
            }
        }
        Unit
    }
    FloatingActionButton(
        openPicker,
        modifier =
        Modifier.appiumSemantics(
            description = "upload file",
            onClick = openPicker,
        ),
    ) {
        Icon(navigationIcon(NavigationGlyph.CLOUD_UPLOAD), "upload file")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadedPage(mediaTarget: ObjectTuple) {
    val vm = createMediaListViewModel(mediaTarget)
    val appNavFactory = LocalAppNavFactory.current
    Column(modifier = Modifier.fillMaxSize()) {
        StateView(vm, modifier = Modifier.fillMaxSize()) { pagingItems ->
            LazyColumn(contentPadding = PaddingValues(10.dp)) {
                topPrepend(pagingItems.loadState, pagingItems::retry)
                pagingItems(pagingItems, key = { it.id }) {
                    val item: FileInfo? = pagingItems[it]
                    FileCell(item) { items ->
                        val first = items.firstOrNull()
                        if (first != null) {
                            appNavFactory.newAppNav().gotoMedia(first)
                        }
                    }
                    if (it < pagingItems.itemCount - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                }
                bottomAppending(pagingItems.loadState, pagingItems::retry)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotaSheet(showSheet: Boolean, sheetState: SheetState, hideSheet: () -> Unit, mediaTarget: ObjectTuple) {
    key(sheetState) { QuotaDialog(showSheet, hideSheet, mediaTarget) }
}

@Composable
private fun UploadRecordPage(mediaTarget: ObjectTuple) {
    val viewModel = createUploadViewModel(mediaTarget)
    StateView(viewModel, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
        ) {
            pagingItems(items, key = { it.id }) {
                val file = items[it]
                UploadItem(file)
                if (it != items.itemCount - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadRecordPage() {
    val vm = getDownloadListViewModel()
    StateView(vm, modifier = Modifier.fillMaxSize()) { items ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
        ) {
            pagingItems(items, key = { it.fileInfo.id }) {
                DownloadRecordItem(items[it])
                if (it != items.itemCount - 1) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadRecordItem(@PreviewParameter(DownloadRecordItemPreviewProvider::class) d: DownloadInfo?) {
    d ?: return
    var showSheet by remember { mutableStateOf(false) }
    val statusLabel = downloadStatusLabel(d.status)
    val rowDescription =
        listOf(
            d.fileInfo.name,
            d.fileInfo.contentType,
            HumanReadable.fileSize(d.fileInfo.size),
            statusLabel,
            d.getPercent(),
        )
            .joinToString(", ")
    TransferRecordContent(
        name = d.fileInfo.name,
        status = statusLabel,
        progress = { getDownloadProgress(d) },
        percent = d.getPercent(),
        modifier =
        Modifier.fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = rowDescription }
            .clickable { showSheet = true }
            .padding(vertical = 20.dp),
    ) {
        DownloadStatusButton(d)
    }
    DownloadInfoPage(showSheet, d.fileInfo.id) {
        showSheet = false
    }
}

@Composable
private fun DownloadStatusButton(d: DownloadInfo?) {
    d ?: return
    val fileProvider = LocalClientFileProvider.current
    val globalDialogController = LocalGlobalDialog.current
    // 根据下载状态显示操作按钮
    when (d.status) {
        DownloadStatus.DOWNLOADING -> {
            IconButton(onClick = {
                globalDialogController.launch {
                    useResult {
                        fileProvider.getDownloader()?.pause(d.fileInfo.id)
                        CustomResult.success(Unit)
                    }
                }
            }) {
                Icon(actionIcon(ActionGlyph.PAUSE), contentDescription = stringResource(Res.string.transfer_pause))
            }
        }

        DownloadStatus.PAUSED,
        DownloadStatus.DOWNLOAD_FAILED,
        DownloadStatus.PROCESS_FAILED,
        DownloadStatus.DOWNLOADED,
        -> {
            IconButton(onClick = {
                globalDialogController.launch {
                    useResult {
                        fileProvider.getDownloader()?.resume(d.fileInfo.id)
                        CustomResult.success(Unit)
                    }
                }
            }) {
                if (d.status == DownloadStatus.DOWNLOAD_FAILED || d.status == DownloadStatus.PROCESS_FAILED) {
                    Icon(actionIcon(ActionGlyph.REFRESH_CW), stringResource(Res.string.transfer_retry))
                } else {
                    Icon(actionIcon(ActionGlyph.PLAY), stringResource(Res.string.transfer_resume))
                }
            }
        }

        DownloadStatus.PROCESSED -> {
            Icon(actionIcon(ActionGlyph.CHECK), contentDescription = stringResource(Res.string.transfer_completed))
        }

        DownloadStatus.NOT_DOWNLOADED -> {
            IconButton({
                globalDialogController.launch {
                    useResult {
                        fileProvider.getDownloader()?.resume(d.fileInfo.id)
                        CustomResult.success(Unit)
                    }
                }
            }) {
                Icon(actionIcon(ActionGlyph.PLAY), stringResource(Res.string.transfer_start))
            }
        }
    }
}

@Composable
fun DownloadInfoPage(showSheet: Boolean, id: PrimaryKey, hideSheet: () -> Unit) {
    if (!showSheet || LocalInspectionMode.current) return
    val downloadViewModel = getDownloadViewModel(id)
    ReadingSheet(stringResource(Res.string.transfer_download_details), hideSheet) {
        DownloadInfoPageInternal(downloadViewModel)
    }
}

@Composable
private fun DownloadInfoPageInternal(downloadViewModel: DownloadViewModel) {
    val downloadInfo by downloadViewModel.data.collectAsState(null)
    DownloadInfoTitle(downloadInfo)
    downloadInfo?.let { info ->
        TransferProgress({ getDownloadProgress(info) }, info.getPercent())
    }
    DownloadInfoTable(downloadInfo)
}

@Composable
private fun DownloadInfoTitle(data: DownloadInfo?) {
    val it = data?.fileInfo
    rememberCoroutineScope()
    LocalGlobalDialog.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        LocalClientFileProvider.current
        Text(it?.name ?: "-", modifier = Modifier.weight(1f))
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { DownloadStatusButton(data) }
    }
}

@Composable
fun DownloadInfoTable(downloadInfo: DownloadInfo?) {
    val tableData =
        if (downloadInfo == null) {
            emptyList()
        } else {
            buildList {
                add(stringResource(Res.string.transfer_field_path) to downloadInfo.path)
                val size = HumanReadable.fileSize(downloadInfo.fileInfo.size)
                add(stringResource(Res.string.transfer_field_size) to size)
                add(stringResource(Res.string.transfer_field_status) to downloadStatusLabel(downloadInfo.status))
                if (downloadInfo.status == DownloadStatus.DOWNLOAD_FAILED) {
                    add(stringResource(Res.string.transfer_field_error) to downloadInfo.message)
                }
                add(stringResource(Res.string.transfer_field_message) to downloadInfo.message)
                add(stringResource(Res.string.transfer_field_url) to downloadInfo.fileInfo.url)
            }
        }
    InfoTable(tableData, singleColumn = true)
}
