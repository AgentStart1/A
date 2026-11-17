/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.filter
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_attachment
import com.storyteller_f.alphaheart.app.audio_recorder
import com.storyteller_f.alphaheart.app.common.OnMediaUploaded
import com.storyteller_f.alphaheart.app.common.createFileSearchViewModel
import com.storyteller_f.alphaheart.app.common.createMediaListViewModel
import com.storyteller_f.alphaheart.app.error.UploadError
import com.storyteller_f.alphaheart.app.files
import com.storyteller_f.alphaheart.app.invalid_image_file
import com.storyteller_f.alphaheart.app.pages.file.FileCell
import com.storyteller_f.alphaheart.app.provide_permission
import com.storyteller_f.alphaheart.app.search_files
import com.storyteller_f.alphaheart.app.search_images
import com.storyteller_f.alphaheart.app.select_image
import com.storyteller_f.alphaheart.app.start_record
import com.storyteller_f.alphaheart.app.stop_record
import com.storyteller_f.alphaheart.app.upload_file
import com.storyteller_f.alphaheart.app.utils.ClientFile
import com.storyteller_f.alphaheart.app.utils.Recorder
import com.storyteller_f.alphaheart.app.utils.RecordingState
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
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
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
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
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import io.ktor.http.ContentType
import io.ktor.http.defaultForFileExtension
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedButton as OutlinedButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow as PrimaryTabRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePicker(
    showSheet: Boolean,
    sheetState: SheetState,
    mediaTarget: ObjectTuple,
    support: List<String> = listOf("files", "audio recorder"),
    requiredDimension: Dimension? = null,
    imageOnly: Boolean = false,
    title: String? = null,
    onClickItems: (List<FileInfo>) -> Unit,
    hideSheet: () -> Unit,
) {
    BaseSheet(showSheet, sheetState, hideSheet) {
        Text(
            title ?: stringResource(if (imageOnly) Res.string.select_image else Res.string.add_attachment),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 40.dp, bottom = 20.dp),
        )
        val pagerState =
            rememberPagerState {
                support.size
            }
        val tabs =
            listOf(
                Icons.Default.Cloud to stringResource(Res.string.files),
                Icons.Default.Mic to stringResource(Res.string.audio_recorder),
            ).filterIndexed { index, _ -> support.contains(if (index == 0) "files" else "audio recorder") }
        if (support.size != 1) {
            val currentPage = pagerState.currentPage
            val scope = rememberCoroutineScope()
            PrimaryTabRow(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentPadding = PaddingValues(top = 8.dp),
            ) {
                tabs.forEachIndexed { index, pair ->
                    val selectTab = {
                        scope.launch {
                            pagerState.scrollToPage(index)
                        }
                        Unit
                    }
                    Tab(
                        currentPage == index,
                        selectTab,
                        modifier = Modifier.appiumSemantics(text = pair.second, onClick = selectTab),
                    ) {
                        Row(
                            Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(pair.first, null, modifier = Modifier.size(20.dp))
                            Text(pair.second)
                        }
                    }
                }
            }
        }
        FilePickerPager(pagerState, modifier = Modifier.height(300.dp)) {
            if (support[it] == "files") {
                FileListView(mediaTarget, requiredDimension, onClickItems, imageOnly)
            } else {
                AudioRecorder(mediaTarget, onClickItems)
            }
        }
    }
}

@Composable
fun AudioRecorder(mediaTarget: ObjectTuple, uploadSuccess: (List<FileInfo>) -> Unit) {
    val recorder = remember(mediaTarget) { Recorder() }
    val recordingState by recorder.state.collectAsState()
    DisposableEffect(recorder) {
        onDispose { recorder.cancelRecord() }
    }
    val isGranted by isPermissionGranted(Permission.Audio)
    Box(modifier = Modifier.fillMaxSize()) {
        RecorderButton(isGranted, recordingState is RecordingState.Active, uploadSuccess, mediaTarget, recorder)
        if (!isGranted) {
            Box(modifier = Modifier.fillMaxSize()) {
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
private fun RecorderButton(
    isGranted: Boolean,
    isRecording: Boolean,
    uploadSuccess: (List<FileInfo>) -> Unit,
    mediaTarget: ObjectTuple,
    recorder: Recorder,
) {
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    if (!isGranted) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val recordAction = {
            scope.launch {
                if (isGranted) {
                    if (isRecording) {
                        globalDialogController.uploadRecording(recorder, mediaTarget)
                            .mapPresent { uploadSuccess(it) }
                    } else {
                        globalDialogController.useResult {
                            captureFailure(UploadError::from) {
                                recorder.startRecord()
                            }
                        }
                    }
                }
            }
            Unit
        }
        val label = stringResource(if (isRecording) Res.string.stop_record else Res.string.start_record)
        Button(
            onClick = recordAction,
            modifier = Modifier.appiumSemantics(text = label, onClick = recordAction),
        ) {
            Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null)
            Text(
                label,
                Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun FileListView(
    mediaTarget: ObjectTuple,
    requiredDimension: Dimension?,
    onClickItem: (List<FileInfo>) -> Unit,
    imageOnly: Boolean,
) {
    var query by rememberSaveable(mediaTarget) { mutableStateOf("") }
    val viewModel =
        if (query.isBlank()) {
            createMediaListViewModel(mediaTarget)
        } else {
            createFileSearchViewModel(mediaTarget.objectId, mediaTarget.objectType, query)
        }
    val visibleModel =
        remember(viewModel, imageOnly) {
            object : PagingViewModel<FileInfo>() {
                override val flow =
                    viewModel.flow.map { page ->
                        if (imageOnly) page.filter { it.contentType.startsWith("image/") } else page
                    }
            }
        }
    val scope = rememberCoroutineScope()
    val controller = LocalGlobalDialog.current
    StateView(
        visibleModel,
        modifier = Modifier.fillMaxSize(),
        header = {
            Row(
                Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    query,
                    { query = it },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    label = {
                        Text(
                            stringResource(if (imageOnly) Res.string.search_images else Res.string.search_files),
                        )
                    },
                )
                OutlinedButton({
                    scope.launch { controller.selectFileAndUpload(mediaTarget, onClickItem, imageOnly) }
                }) { Icon(Icons.Default.CloudUpload, stringResource(Res.string.upload_file)) }
            }
        },
    ) { pagingItems ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
            topPrepend(pagingItems.loadState, pagingItems::retry)
            pagingItems(pagingItems, { it.id }) { index ->
                val item = pagingItems[index]
                FileCell(item, requiredDimension, onClickItem)
                if (index < pagingItems.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
            }
            bottomAppending(pagingItems.loadState, pagingItems::retry)
        }
    }
}

suspend fun AppGlobalDialogController.selectFileAndUpload(
    mediaTarget: ObjectTuple,
    uploadSuccess: (List<FileInfo>) -> Unit,
    imageOnly: Boolean = false,
) {
    useResult {
        val f = FileKit.openFilePicker(type = if (imageOnly) FileKitType.Image else FileKitType.File())
        if (f != null) {
            val invalidImage = getString(Res.string.invalid_image_file)
            require(!imageOnly || ContentType.defaultForFileExtension(f.extension).toString().startsWith("image/")) {
                invalidImage
            }
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
                require(
                    !imageOnly || it.all { info -> info.contentType.startsWith("image/") && info.dimension != null },
                ) {
                    invalidImage
                }
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

private suspend fun AppGlobalDialogController.uploadRecording(
    recorder: Recorder,
    mediaTarget: ObjectTuple,
): CustomResult<List<FileInfo>?, UploadError> =
    useResult {
        captureFailure(UploadError::from) {
            recorder.stopRecord()?.let { createRecordingUpload(it) }
        }.andThen(UploadError::fromError) { data ->
            if (data == null) {
                CustomResult.success(null)
            } else {
                upload(mediaTarget, data) { progress, total ->
                    emitProgress {
                        GlobalDialogState.Loading(progress = GlobalDialogStateProgress(progress, total))
                    }
                }.map { it as List<FileInfo>? }
            }
        }
    }.mapError(UploadError::fromError)

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private suspend fun createRecordingUpload(bytes: ByteArray): UploadData {
    val digest = Buffer().also { it.write(bytes) }.use { sha256(it) }
    return UploadData(
        bytes.size.toLong(),
        "${kotlin.uuid.Uuid.random().toHexString()}.wav",
        ContentType.parse("audio/wav"),
        digest,
    ) { Buffer().also { it.write(bytes) } }
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
