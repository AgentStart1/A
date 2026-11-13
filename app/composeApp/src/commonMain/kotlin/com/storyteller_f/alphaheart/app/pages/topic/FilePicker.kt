/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.audio_recorder
import com.storyteller_f.alphaheart.app.common.OnMediaUploaded
import com.storyteller_f.alphaheart.app.common.createMediaListViewModel
import com.storyteller_f.alphaheart.app.error.UploadError
import com.storyteller_f.alphaheart.app.files
import com.storyteller_f.alphaheart.app.pages.file.FileCell
import com.storyteller_f.alphaheart.app.provide_permission
import com.storyteller_f.alphaheart.app.start_record
import com.storyteller_f.alphaheart.app.stop_record
import com.storyteller_f.alphaheart.app.upload_file
import com.storyteller_f.alphaheart.app.utils.ClientFile
import com.storyteller_f.alphaheart.app.utils.Recorder
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialogState
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialogStateProgress
import com.storyteller_f.alphaheart.client.compose_core.components.Permission
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.isPermissionGranted
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.components.requestPermission
import com.storyteller_f.alphaheart.client.compose_core.components.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.utils.getUploadDataFromPath
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.utils.generateImageMarkdownContent
import com.storyteller_f.shared.utils.generateObjectMarkdownContent
import com.storyteller_f.shared.utils.sha256
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePicker(
    showSheet: Boolean,
    sheetState: SheetState,
    mediaTarget: ObjectTuple,
    support: List<String> = listOf("files", "audio recorder"),
    requiredDimension: Dimension? = null,
    onClickItems: (List<FileInfo>) -> Unit,
    hideSheet: () -> Unit,
) {
    BaseSheet(showSheet, sheetState, hideSheet) {
        val pagerState =
            rememberPagerState {
                support.size
            }
        val tabs =
            listOf(
                Icons.Default.Cloud to stringResource(Res.string.files),
                Icons.Default.Mic to stringResource(Res.string.audio_recorder),
            ).filter {
                support.contains(it.second)
            }
        if (support.size != 1) {
            val currentPage = pagerState.currentPage
            val scope = rememberCoroutineScope()
            PrimaryTabRow(
                currentPage,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                tabs.forEachIndexed { index, pair ->
                    Tab(currentPage == index, {
                        scope.launch {
                            pagerState.scrollToPage(index)
                        }
                    }) {
                        Icon(pair.first, pair.second, modifier = Modifier.padding(vertical = 10.dp).size(30.dp))
                    }
                }
            }
        }
        HorizontalPager(pagerState, modifier = Modifier.height(300.dp)) {
            if (tabs[it].second == "files") {
                FileListView(mediaTarget, requiredDimension, onClickItems)
            } else {
                AudioRecorder(mediaTarget, onClickItems)
            }
        }
    }
}

@Composable
fun AudioRecorder(mediaTarget: ObjectTuple, uploadSuccess: (List<FileInfo>) -> Unit) {
    val isRecording by Recorder.isRecording
    val isGranted by isPermissionGranted(Permission.Audio)
    Box(modifier = Modifier.fillMaxSize()) {
        RecorderButton(isGranted, isRecording, uploadSuccess, mediaTarget)
        if (!isGranted) {
            Box(
                modifier =
                Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Button({
                    requestPermission(Permission.Audio)
                }, modifier = Modifier.align(Alignment.Center)) {
                    Text(stringResource(Res.string.provide_permission))
                }
            }
        }
    }
}

@Composable
private fun BoxScope.RecorderButton(
    isGranted: Boolean,
    isRecording: Boolean,
    uploadSuccess: (List<FileInfo>) -> Unit,
    mediaTarget: ObjectTuple,
) {
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    Box(
        modifier =
        Modifier.size(150.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .clip(CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape).align(Alignment.Center)
            .clickable {
                scope.launch {
                    if (isGranted) {
                        if (isRecording) {
                            val path = Recorder.stopRecord()
                            Napier.i {
                                "save to ${path ?: "<none>"}"
                            }
                            if (path != null) {
                                globalDialogController.uploadPath(path, mediaTarget)
                                    .mapPresent {
                                        uploadSuccess(it)
                                    }
                            }
                        } else {
                            globalDialogController.useResult {
                                captureFailure(UploadError::from) {
                                    Recorder.startRecord()
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (isRecording) {
            Icon(Icons.Default.Stop, stringResource(Res.string.stop_record), modifier = Modifier.size(50.dp))
        } else {
            Icon(Icons.Default.PlayArrow, stringResource(Res.string.start_record), modifier = Modifier.size(50.dp))
        }
    }
}

@Composable
private fun FileListView(
    mediaTarget: ObjectTuple,
    requiredDimension: Dimension?,
    onClickItem: (List<FileInfo>) -> Unit,
) {
    val viewModel = createMediaListViewModel(mediaTarget)
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    Column(modifier = Modifier.padding(top = 10.dp)) {
        Text(mediaTarget.toString())
        Row {
            IconButton({
                scope.launch {
                    globalDialogController.selectFileAndUpload(mediaTarget) {
                        onClickItem(it)
                    }
                }
            }) {
                Icon(Icons.Default.CloudUpload, stringResource(Res.string.upload_file))
            }
        }
        StateView(viewModel, modifier = Modifier.weight(1f)) { pagingItems ->
            LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                topPrepend(pagingItems.loadState)
                pagingItems(pagingItems, {
                    it.id
                }) {
                    val item = pagingItems[it]
                    FileCell(item, requiredDimension, onClickItem)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                }
                bottomAppending(pagingItems.loadState)
            }
        }
    }
}

suspend fun AppGlobalDialogController.selectFileAndUpload(
    mediaTarget: ObjectTuple,
    uploadSuccess: (List<FileInfo>) -> Unit,
) {
    useResult {
        val f = FileKit.openFilePicker()
        if (f != null) {
            val bytes = f.readBytes()
            val fileSha256 = Buffer().also { it.write(bytes) }.use { sha256(it) }
            val uploadData = getUploadDataFromPlatformFile(f, fileSha256, bytes)
            upload(
                mediaTarget,
                uploadData,
            ) { p, t ->
                emitProgress {
                    GlobalDialogState.Loading(progress = GlobalDialogStateProgress(p, t))
                }
            }.map {
                it as List<FileInfo>?
            }
        } else {
            CustomResult.success(null)
        }
    }.getOrNull()?.let {
        uploadSuccess(it)
    }
}

private fun getUploadDataFromPlatformFile(f: PlatformFile, sha256: String, bytes: ByteArray) =
    UploadData(
        f.size(),
        f.name,
        ContentType.defaultForFileExtension(f.extension),
        sha256,
    ) {
        Buffer().also { it.write(bytes) }
    }

class PlatformClientFile private constructor(val platformFile: PlatformFile, private val bytes: ByteArray) :
    ClientFile {
    override val name: String get() = platformFile.name
    override val contentType: ContentType get() = ContentType.defaultForFileExtension(platformFile.extension)
    override val size: Long get() = platformFile.size()
    override val path: String get() = platformFile.path

    override fun source(): Source = ByteArrayRawSource(bytes).buffered()

    companion object {
        suspend operator fun invoke(platformFile: PlatformFile) =
            PlatformClientFile(platformFile, platformFile.readBytes())
    }
}

private class ByteArrayRawSource(private val bytes: ByteArray) : RawSource {
    private var offset = 0
    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        if (offset >= bytes.size) return -1L
        val count = minOf(byteCount, (bytes.size - offset).toLong()).toInt()
        sink.write(bytes, offset, offset + count)
        offset += count
        return count.toLong()
    }
    override fun close() = Unit
}

suspend fun AppGlobalDialogController.uploadPath(
    path: Path,
    mediaTarget: ObjectTuple,
): CustomResult<List<FileInfo>?, UploadError> {
    val meta = SystemFileSystem.metadataOrNull(path) ?: return CustomResult.success(null)
    val fileSha256 =
        SystemFileSystem.source(path).buffered().use {
            sha256(it)
        }
    return useResult {
        upload(
            mediaTarget,
            getUploadDataFromPath(meta, path, fileSha256),
        ) { p, t ->
            emitProgress {
                GlobalDialogState.Loading(progress = GlobalDialogStateProgress(p, t))
            }
        }
    }.mapError(UploadError::fromError)
}

suspend fun AppGlobalDialogController.upload(
    mediaTarget: ObjectTuple,
    uploadData: UploadData,
    onUpload: suspend (Long, Long?) -> Unit = { _, _ -> },
): CustomResult<List<FileInfo>, UploadError> {
    if (uploadData.size > 100 * 1024 * 1024) {
        return CustomResult.Failure(
            UploadError.from(Exception("file size is too large")),
        ).mapError(UploadError::fromError)
    }

    return request { this.upload(mediaTarget, uploadData, onUpload) }.map {
        it.data
    }.onSuccess {
        emitEvent(OnMediaUploaded(it))
    }.mapError(UploadError::fromError)
}

fun insertContent(it: FileInfo, input: String, updateInput: (String) -> Unit) {
    val text =
        if (it.contentType.startsWith("image/")) {
            "\n${generateImageMarkdownContent(it)}"
        } else {
            "\n${generateObjectMarkdownContent(it)}"
        }
    updateInput(input + text)
}
