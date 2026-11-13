/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.FileViewViewModel
import com.storyteller_f.alphaheart.app.common.createFileViewModel
import com.storyteller_f.alphaheart.app.download_file
import com.storyteller_f.alphaheart.app.preview_unavailable
import com.storyteller_f.alphaheart.app.ui.MaterialSymbolsOutlined
import com.storyteller_f.alphaheart.client.compose_core.components.AudioViewFullScreen
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.FileViewData
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.PdfView
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.VideoViewFullScreen
import com.storyteller_f.alphaheart.client.compose_core.components.globalLoader
import com.storyteller_f.alphaheart.client.compose_core.components.rememberIsInPipMode
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.copy
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.FileInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
                if (!rememberIsInPipMode()) {
                    FilePreviewToolbar(info)
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { RegularFileViewInternal(info) }
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
internal fun FilePreviewToolbar(info: FileInfo) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val downloadLabel = stringResource(Res.string.download_file)
    val navigation = LocalAppNavFactory.current
    val back = { navigation.newAppNav().back() }
    val download = { uriHandler.openUri(info.url) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        IconButton(back, Modifier.appiumSemantics(description = "back", onClick = back)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
        }
        Text(
            info.name,
            Modifier.weight(1f).padding(12.dp).appiumSemantics(text = info.name),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
        TextButton(
            download,
            Modifier.appiumSemantics(text = downloadLabel, description = "download file", onClick = download),
        ) {
            Text(stringResource(Res.string.download_file))
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
