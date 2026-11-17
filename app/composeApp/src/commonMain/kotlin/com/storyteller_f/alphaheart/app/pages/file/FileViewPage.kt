/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.FileViewViewModel
import com.storyteller_f.alphaheart.app.common.createFileViewModel
import com.storyteller_f.alphaheart.app.download_file
import com.storyteller_f.alphaheart.app.file_preview_title
import com.storyteller_f.alphaheart.app.preview_unavailable
import com.storyteller_f.alphaheart.app.ui.MaterialSymbolsOutlined
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.AudioViewFullScreen
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.FileViewData
import com.storyteller_f.alphaheart.client.compose_core.components.LocalResourceDownloadAction
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.MediaMenuScope
import com.storyteller_f.alphaheart.client.compose_core.components.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.PdfView
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.components.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.VideoViewFullScreen
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.components.globalLoader
import com.storyteller_f.alphaheart.client.compose_core.components.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.components.rememberIsInPipMode
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.copy
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.FileInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileViewPage(session: FileViewData) {
    when (session) {
        is FileViewData.Regular -> {
            val fileViewModel = createFileViewModel(session.fileId)
            RegularFileView(fileViewModel)
        }

        is FileViewData.Player -> {
            CenterBox {
                val remoteMediaItem = session.remoteMediaItem
                if (remoteMediaItem.contentType.startsWith("video")) {
                    VideoViewFullScreen(remoteMediaItem)
                } else {
                    AudioViewFullScreen(remoteMediaItem)
                }
            }
        }

        is FileViewData.LocalImage -> {
            Box {
                CoilZoomAsyncImage(
                    model = session.url,
                    contentDescription = "view image",
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun RegularFileView(fileViewModel: FileViewViewModel) {
    Surface(Modifier.fillMaxSize()) {
        StateView(fileViewModel.handler, modifier = Modifier.fillMaxSize()) { info ->
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                val pip = rememberIsInPipMode()
                if (!pip) FilePreviewToolbar()
                val downloadResource = LocalResourceDownloadAction.current
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (pip) {
                        RegularFileViewInternal(info)
                    } else {
                        FilePreviewContent(
                            info,
                            { downloadResource(ResourceDownloadRequest(info.url, info.name, info.size)) },
                        ) {
                            MediaMenuScope(showDownload = false, showHeading = false) {
                                RegularFileViewInternal(info)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegularFileViewInternal(info: FileInfo) {
    when {
        info.contentType.startsWith("video/") -> VideoViewFullScreen(info.mediaItem())

        info.contentType.startsWith("audio/") -> AudioViewFullScreen(info.mediaItem())

        info.contentType.startsWith("image") -> {
            ZoomImage(info)
        }

        info.contentType == FileInfo.PDF_CONTENT_TYPE -> {
            Column {
                PdfView(info.url, Modifier.weight(1f))
            }
        }

        else ->
            CenterBox {
                val message = stringResource(Res.string.preview_unavailable)
                Text(message, Modifier.appiumSemantics(text = message))
            }
    }
}

private fun FileInfo.mediaItem() = RemoteMediaItem(id.toString(), url, contentType, name)

@Composable
internal fun FilePreviewToolbar() {
    val navigation = LocalAppNavFactory.current
    val back = { navigation.newAppNav().back() }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(back, Modifier.appiumSemantics(description = "back", onClick = back)) {
            Icon(actionIcon(ActionGlyph.ARROW_LEFT), contentDescription = "back")
        }
        Text(
            stringResource(Res.string.file_preview_title),
            Modifier.weight(1f).padding(12.dp),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
internal fun FilePreviewContent(info: FileInfo, download: () -> Unit, content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("file-preview-main")) {
        val horizontal =
            when {
                maxWidth <= 600.dp -> 14.dp
                maxWidth <= 820.dp -> 22.dp
                else -> 30.dp
            }
        val vertical = if (maxWidth <= 600.dp) 20.dp else horizontal
        Column(
            Modifier.fillMaxSize().padding(horizontal = horizontal, vertical = vertical)
                .testTag("file-preview-content"),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            FilePreviewHeading(info, download)
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

@Composable
internal fun FilePreviewHeading(info: FileInfo, download: () -> Unit) {
    val downloadLabel = stringResource(Res.string.download_file)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            info.name,
            Modifier.weight(1f).padding(end = 12.dp).appiumSemantics(text = info.name),
            style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            download,
            Modifier.appiumSemantics(text = downloadLabel, description = "download file", onClick = download),
        ) {
            Icon(navigationIcon(NavigationGlyph.DOWNLOAD), contentDescription = null)
            Text(downloadLabel)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ZoomImage(fileInfo: FileInfo) {
    Box {
        var showSheet by remember {
            mutableStateOf(false)
        }
        val sheetState = rememberModalBottomSheetState()
        var imageLoaded by remember(fileInfo.url) { mutableStateOf(false) }
        val imageDescription = if (imageLoaded) "view image" else "image loading"
        CoilZoomAsyncImage(
            model = globalLoader(fileInfo.url),
            contentDescription = imageDescription,
            modifier = Modifier.fillMaxSize().appiumSemantics(description = imageDescription),
            onSuccess = { imageLoaded = true },
            onError = {
                imageLoaded = false
                Napier.e("Uploaded image preview failed: ${it.result.throwable.message.orEmpty()}")
            },
            onLongPress = {
                showSheet = true
            },
        )
        ImageSheet(fileInfo, showSheet, sheetState) {
            showSheet = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSheet(fileInfo: FileInfo, showSheet: Boolean, sheetState: SheetState, hideSheet: () -> Unit) {
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val globalDialogController = LocalGlobalDialog.current
    BaseSheet(
        showSheet,
        sheetState,
        hideSheet,
    ) {
        Column(modifier = Modifier.height(200.dp).padding(20.dp)) {
            ButtonNav(MaterialSymbolsOutlined.FileCopy, "copy") {
                scope.launch {
                    globalDialogController.useResult {
                        request {
                            copy(fileInfo.id)
                        }
                    }.onSuccess {
                        toaster.showMessage("done")
                    }
                }
            }
        }
    }
}
