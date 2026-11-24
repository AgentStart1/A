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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_attachment
import com.storyteller_f.alphaheart.app.audio_recorder
import com.storyteller_f.alphaheart.app.common.createFileSearchViewModel
import com.storyteller_f.alphaheart.app.common.createMediaListViewModel
import com.storyteller_f.alphaheart.app.components.ReadingSheet
import com.storyteller_f.alphaheart.app.error.UploadError
import com.storyteller_f.alphaheart.app.files
import com.storyteller_f.alphaheart.app.media.selectFileAndUpload
import com.storyteller_f.alphaheart.app.media.uploadRecording
import com.storyteller_f.alphaheart.app.pages.file.FileCell
import com.storyteller_f.alphaheart.app.provide_permission
import com.storyteller_f.alphaheart.app.search_files
import com.storyteller_f.alphaheart.app.search_images
import com.storyteller_f.alphaheart.app.select_image
import com.storyteller_f.alphaheart.app.start_record
import com.storyteller_f.alphaheart.app.stop_record
import com.storyteller_f.alphaheart.app.upload_file
import com.storyteller_f.alphaheart.app.utils.Recorder
import com.storyteller_f.alphaheart.app.utils.RecordingState
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.isPermissionGranted
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.permissions.Permission
import com.storyteller_f.alphaheart.client.compose_core.permissions.requestPermission
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.mapPresent
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedButton as OutlinedButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow as PrimaryTabRow

@Composable
fun FilePicker(
    showSheet: Boolean,
    mediaTarget: ObjectTuple,
    support: List<String> = listOf("files", "audio recorder"),
    requiredDimension: Dimension? = null,
    imageOnly: Boolean = false,
    title: String? = null,
    allowUpload: Boolean = true,
    onClickItems: (List<FileInfo>) -> Unit,
    hideSheet: () -> Unit,
) {
    if (!showSheet) return
    val heading = title ?: stringResource(if (imageOnly) Res.string.select_image else Res.string.add_attachment)
    ReadingSheet(heading, hideSheet, ReadingOverlaySize.FORM) {
        val pagerState = rememberPagerState { support.size }
        val multipleTabs = support.size > 1
        if (multipleTabs) {
            FilePickerTabs(pagerState, support) {
                if (allowUpload && support[pagerState.currentPage] == "files") {
                    FilePickerUploadButton(mediaTarget, onClickItems, imageOnly)
                }
            }
        }
        FilePickerPager(pagerState, modifier = Modifier.height(300.dp)) {
            if (support[it] == "files") {
                FileListView(mediaTarget, requiredDimension, onClickItems, imageOnly, allowUpload && !multipleTabs)
            } else {
                AudioRecorder(mediaTarget, onClickItems)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilePickerTabs(pagerState: PagerState, support: List<String>, actions: @Composable () -> Unit) {
    val tabs =
        listOf(
            Icons.Default.Cloud to stringResource(Res.string.files),
            Icons.Default.Mic to stringResource(Res.string.audio_recorder),
        )
    val scope = rememberCoroutineScope()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            PrimaryTabRow(contentPadding = PaddingValues(top = 8.dp)) {
                support.forEachIndexed { index, page ->
                    val (icon, label) = tabs[if (page == "files") 0 else 1]
                    val selectTab: () -> Unit = { scope.launch { pagerState.scrollToPage(index) } }
                    Tab(
                        pagerState.currentPage == index,
                        selectTab,
                        modifier = Modifier.appiumSemantics(text = label, onClick = selectTab),
                    ) {
                        Row(
                            Modifier.padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(label)
                        }
                    }
                }
            }
        }
        actions()
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
    allowUpload: Boolean,
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
                if (allowUpload) FilePickerUploadButton(mediaTarget, onClickItem, imageOnly)
            }
        },
    ) { pagingItems ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 12.dp)) {
            topPrepend(pagingItems.loadState, pagingItems::retry)
            pagingItems(pagingItems, { it.id }) { index ->
                val item = pagingItems[index]
                FileCell(item, requiredDimension, previewFromMenu = true, clickItem = onClickItem)
                if (index < pagingItems.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
            }
            bottomAppending(pagingItems.loadState, pagingItems::retry)
        }
    }
}

@Composable
private fun FilePickerUploadButton(
    mediaTarget: ObjectTuple,
    onClickItem: (List<FileInfo>) -> Unit,
    imageOnly: Boolean,
) {
    val scope = rememberCoroutineScope()
    val controller = LocalGlobalDialog.current
    OutlinedButton({
        scope.launch { controller.selectFileAndUpload(mediaTarget, onClickItem, imageOnly) }
    }) { Icon(Icons.Default.CloudUpload, stringResource(Res.string.upload_file)) }
}
