/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotStarted
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.paging.compose.collectAsLazyPagingItems
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalClientFileProvider
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.common.DownloadViewModel
import com.storyteller_f.alphaheart.app.common.createMediaListViewModel
import com.storyteller_f.alphaheart.app.common.createUploadViewModel
import com.storyteller_f.alphaheart.app.common.getDownloadListViewModel
import com.storyteller_f.alphaheart.app.common.getDownloadViewModel
import com.storyteller_f.alphaheart.app.common.getQuotaViewModel
import com.storyteller_f.alphaheart.app.pages.search.CustomSearchBar
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.app.pages.topic.PlatformClientFile
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.FileIcon
import com.storyteller_f.alphaheart.client.compose_core.components.InfoTable
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.components.SheetContainer
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.QuotaDetail
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadStatus
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import nl.jacobras.humanreadable.HumanReadable
import kotlin.math.pow
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerPage(mediaTarget: ObjectTuple) {
    val backStack = rememberFileExplorerBackStack()
    var showQuota by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    FileExplorerNavigation(
        backStack = backStack,
        routes = fileNavRoutes(mediaTarget),
        search = {
            CustomSearchBar(
                scope = SearchScope.UploadedFiles(mediaTarget.objectId, mediaTarget.objectType),
                leadingIcon = {},
            )
        },
        floatingActionButton = { FileExplorerFab(backStack.last()) { showQuota = true } },
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
            NavRoute("/uploaded", Icons.Filled.Folder, "Uploaded"),
            NavRoute("/upload-record", Icons.Filled.CloudUpload, "Upload"),
        )
    if (mediaTarget.objectType == ObjectType.USER) {
        list.add(NavRoute("/download-record", Icons.Filled.Download, "App downloads"))
    }
    return list
}

@Serializable
sealed interface FileExplorerRoute : NavKey {
    @Serializable
    data object Uploaded : FileExplorerRoute {
        override fun toString(): String = "/uploaded"
    }

    @Serializable
    data object UploadRecord : FileExplorerRoute {
        override fun toString(): String = "/upload-record"
    }

    @Serializable
    data object DownloadRecord : FileExplorerRoute {
        override fun toString(): String = "/download-record"
    }
}

@Composable
private fun FileExplorerFab(current: NavKey, showQuota: () -> Unit) {
    when (current) {
        FileExplorerRoute.Uploaded -> {
            FileQuotaActionButton(showQuota)
        }

        FileExplorerRoute.UploadRecord -> {
            UploadFileActionButton()
        }
    }
}

@Composable
private fun FileQuotaActionButton(onClick: () -> Unit) {
    FloatingActionButton(onClick) {
        Icon(Icons.Filled.Info, "quota info")
    }
}

@Composable
private fun UploadFileActionButton() {
    val fileProvider = LocalClientFileProvider.current
    val globalDialogController = LocalGlobalDialog.current
    val openPicker = {
        globalDialogController.launch {
            useResult {
                val f = FileKit.openFilePicker()
                if (f != null) {
                    fileProvider.getUploader()?.upload(persistentListOf(PlatformClientFile(f)))
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
        Icon(Icons.Filled.CloudUpload, "upload file")
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
                topPrepend(pagingItems.loadState)
                pagingItems(pagingItems, key = { it.id }) {
                    val item: FileInfo? = pagingItems[it]
                    FileCell(item) { items ->
                        val first = items.firstOrNull()
                        if (first != null) {
                            appNavFactory.newAppNav().gotoMedia(first)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                }
                bottomAppending(pagingItems.loadState)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotaSheet(showSheet: Boolean, sheetState: SheetState, hideSheet: () -> Unit, mediaTarget: ObjectTuple) {
    BaseSheet(showSheet, sheetState, hideSheet) {
        val quotaVm = getQuotaViewModel(mediaTarget)
        StateView(quotaVm.handler) { q: QuotaDetail ->
            Column(modifier = Modifier.padding(20.dp)) {
                val totalText = HumanReadable.fileSize(q.total)
                val usedText = HumanReadable.fileSize(q.used)
                Text("总空间: $totalText")
                Text("已用空间: $usedText")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { q.used.toFloat() / q.total.coerceAtLeast(1) },
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    val percent = if (q.total > 0) q.used * 100f / q.total else 0f
                    Text("${percent.toInt()}%")
                }
                val rec = q.uploadRecord
                if (rec != null) {
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text("上传记录: ${rec.name}")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = {
                                (rec.progress.toFloat() / rec.total.coerceAtLeast(1)).coerceIn(
                                    0f,
                                    1f,
                                )
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        val uploadPercent = if (rec.total > 0) rec.progress * 100f / rec.total else 0f
                        Text("${uploadPercent.toInt()}%")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(HumanReadable.fileSize(rec.progress))
                        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                        Text("/ ${HumanReadable.fileSize(rec.total)}")
                        Spacer(modifier = Modifier.weight(1f))
                        Text(rec.status.name)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("分片大小: ${HumanReadable.fileSize(rec.chunkSize)}")
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadRecordPage(mediaTarget: ObjectTuple) {
    val viewModel = createUploadViewModel(mediaTarget.objectId)
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
    val items = vm.flow.collectAsLazyPagingItems()
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

class DownloadRecordItemPreviewProvider : PreviewParameterProvider<DownloadInfo?> {
    override val values: Sequence<DownloadInfo?> =
        sequenceOf(
            DownloadInfo(
                id = 1000L,
                fileInfo =
                FileInfo(
                    id = 1L,
                    url = "https://example.com/files/sample1.png",
                    fullName = "images/sample1.png",
                    contentType = "image/png",
                    size = 5_000_000,
                    name = "sample1.png",
                    owner = 1001L,
                    ownerType = ObjectType.USER,
                    lastModified = now(),
                    dimension = null,
                ),
                status = DownloadStatus.DOWNLOADING,
                message = "正在下载…",
                path = "/downloads/sample1.png",
                progress = 2_500_000,
                total = 5_000_000,
            ),
            DownloadInfo(
                id = 2000L,
                fileInfo =
                FileInfo(
                    id = 2L,
                    url = "https://example.com/files/sample2.mp4",
                    fullName = "videos/sample2.mp4",
                    contentType = "video/mp4",
                    size = 120_000_000,
                    name = "sample2.mp4",
                    owner = 1002L,
                    ownerType = ObjectType.USER,
                    lastModified = now(),
                    dimension = null,
                ),
                status = DownloadStatus.DOWNLOADED,
                message = "下载完成",
                path = "/downloads/sample2.mp4",
                progress = 120_000_000,
                total = 120_000_000,
            ),
            DownloadInfo(
                id = 3000L,
                fileInfo =
                FileInfo(
                    id = 3L,
                    url = "https://example.com/files/sample3.pdf",
                    fullName = "docs/sample3.pdf",
                    contentType = FileInfo.PDF_CONTENT_TYPE,
                    size = 2_000_000,
                    name = "sample3.pdf",
                    owner = 1003L,
                    ownerType = ObjectType.USER,
                    lastModified = now(),
                    dimension = null,
                ),
                status = DownloadStatus.DOWNLOAD_FAILED,
                message = "网络错误，稍后重试",
                path = "/downloads/sample3.pdf",
                progress = 500_000,
                total = 2_000_000,
            ),
            DownloadInfo(
                id = 5000L,
                fileInfo =
                FileInfo(
                    id = 5L,
                    url = "https://example.com/files/sample5.jpg",
                    fullName = "images/sample5.jpg",
                    contentType = "image/jpeg",
                    size = 3_500_000,
                    name = "sample5.jpg",
                    owner = 1005L,
                    ownerType = ObjectType.USER,
                    lastModified = now(),
                    dimension = null,
                ),
                status = DownloadStatus.PROCESS_FAILED,
                message = "处理失败：文件损坏",
                path = "/downloads/sample5.jpg",
                progress = 3_500_000,
                total = 3_500_000,
            ),
        )
}

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadRecordItem(@PreviewParameter(DownloadRecordItemPreviewProvider::class) d: DownloadInfo?) {
    d ?: return
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable {
                showSheet = true
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FileIcon(d.fileInfo)
        Column(modifier = Modifier.weight(1f)) {
            Text(d.fileInfo.name)
            Row {
                Text(HumanReadable.fileSize(d.fileInfo.size))
                Spacer(modifier = Modifier.weight(1f))
                Text(d.status.name)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = {
                    getDownloadProgress(d)
                }, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                FixedProgress(d.getPercent())
            }
        }
        DownloadStatusButton(d)
    }
    DownloadInfoPage(showSheet, sheetState, d.fileInfo.id) {
        showSheet = false
    }
}

private fun getDownloadProgress(d: DownloadInfo): Float =
    (d.progress.toFloat() / if (d.total == 0L) 1 else d.total).coerceIn(
        0f,
        1f,
    )

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
                Icon(Icons.Default.Pause, contentDescription = "pause")
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
                Icon(Icons.Default.PlayCircle, contentDescription = "resume")
            }
        }

        DownloadStatus.PROCESSED -> {
            Icon(Icons.Default.Done, contentDescription = "done")
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
                Icon(Icons.Default.NotStarted, "start")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadInfoPage(showSheet: Boolean, sheetState: SheetState, id: PrimaryKey, hideSheet: () -> Unit) {
    if (LocalInspectionMode.current) return
    val downloadViewModel = getDownloadViewModel(id)
    BaseSheet(showSheet, sheetState, hideSheet) {
        SheetContainer {
            Column(
                modifier = Modifier.heightIn(200.dp, 600.dp).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DownloadInfoPageInternal(downloadViewModel)
            }
        }
    }
}

@Composable
private fun DownloadInfoPageInternal(downloadViewModel: DownloadViewModel) {
    val downloadInfo by downloadViewModel.data.collectAsState(null)
    DownloadInfoTitle(downloadInfo)
    downloadInfo?.let { info ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinearProgressIndicator(progress = {
                info.progress.toFloat() / info.total
            })
            FixedProgress(info.getPercent())
        }
    }
    DownloadInfoTable(downloadInfo)
}

fun DownloadInfo.getPercent(): String = "${(progress.toFloat() * 100 / total).roundToDecimalPlaces(2)} %"

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
        DownloadStatusButton(data)
    }
}

@Composable
fun DownloadInfoTable(downloadInfo: DownloadInfo?) {
    val fileInfo = downloadInfo?.fileInfo
    val tableData =
        remember(downloadInfo, fileInfo) {
            buildList {
                downloadInfo?.path?.let { add("Path" to it) }
                fileInfo?.size?.let { add("Size" to HumanReadable.fileSize(it)) }
                downloadInfo?.status?.name?.let { add("Status" to it) }
                if (downloadInfo?.status == DownloadStatus.DOWNLOAD_FAILED) {
                    add("Error" to downloadInfo.message)
                }
                downloadInfo?.message?.let { add("Message" to it) }
                fileInfo?.url?.let { add("Url" to it) }
            }
        }
    InfoTable(tableData)
}

fun Float.roundToDecimalPlaces(decimals: Int): Float {
    val multiplier = 10.0f.pow(decimals)
    return round(this * multiplier) / multiplier
}
