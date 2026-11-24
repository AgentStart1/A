/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.FileViewViewModel
import com.storyteller_f.alphaheart.app.common.createFileViewModel
import com.storyteller_f.alphaheart.app.components.ReadingSheet
import com.storyteller_f.alphaheart.app.copy
import com.storyteller_f.alphaheart.app.file.mediaItem
import com.storyteller_f.alphaheart.app.file_preview_title
import com.storyteller_f.alphaheart.app.image_actions
import com.storyteller_f.alphaheart.app.preview_unavailable
import com.storyteller_f.alphaheart.client.compose_core.components.AudioViewFilled
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.PdfView
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingFilePreviewBody
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingImageFilePreview
import com.storyteller_f.alphaheart.client.compose_core.components.VideoViewFullScreen
import com.storyteller_f.alphaheart.client.compose_core.components.globalLoader
import com.storyteller_f.alphaheart.client.compose_core.components.rememberIsInPipMode
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.media.FileViewData
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.copy
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.FileInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

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
                    AudioViewFilled(remoteMediaItem)
                }
            }
        }

        is FileViewData.LocalImage -> {
            ReadingImageFilePreview(session.url, "view image")
        }
    }
}

@Composable
private fun RegularFileView(fileViewModel: FileViewViewModel) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            val pip = rememberIsInPipMode()
            if (!pip) FilePreviewToolbar()
            ReadingFilePreviewBody(
                fileViewModel.handler,
                Modifier.weight(1f).fillMaxWidth(),
                showChrome = !pip,
                content = ::RegularFileViewInternal,
            )
        }
    }
}

@Composable
private fun RegularFileViewInternal(info: FileInfo) {
    when {
        info.contentType.startsWith("video/") -> VideoViewFullScreen(info.mediaItem())

        info.contentType.startsWith("audio/") -> AudioViewFilled(info.mediaItem())

        info.contentType.startsWith("image") -> {
            ZoomImage(info)
        }

        info.contentType == FileInfo.PDF_CONTENT_TYPE -> {
            PdfView(info.url, Modifier.fillMaxSize(), showTools = true)
        }

        else ->
            CenterBox {
                val message = stringResource(Res.string.preview_unavailable)
                Text(message, Modifier.appiumSemantics(text = message))
            }
    }
}

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
@OptIn(ExperimentalMaterial3Api::class)
private fun ZoomImage(fileInfo: FileInfo) {
    Box {
        var showSheet by remember {
            mutableStateOf(false)
        }
        var imageLoaded by remember(fileInfo.url) { mutableStateOf(false) }
        val imageDescription = if (imageLoaded) "view image" else "image loading"
        ReadingImageFilePreview(
            model = globalLoader(fileInfo.url),
            description = imageDescription,
            onSuccess = { imageLoaded = true },
            onError = {
                imageLoaded = false
                Napier.e("Uploaded image preview failed: ${it.result.throwable.message.orEmpty()}")
            },
            onLongPress = {
                showSheet = true
            },
        )
        if (showSheet) ImageSheet(fileInfo) { showSheet = false }
    }
}

@Composable
private fun ImageSheet(fileInfo: FileInfo, hideSheet: () -> Unit) {
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val globalDialogController = LocalGlobalDialog.current
    ReadingSheet(stringResource(Res.string.image_actions), hideSheet, ReadingOverlaySize.COMPACT) {
        val copyLabel = stringResource(Res.string.copy)
        IconButton({
            hideSheet()
            scope.launch {
                globalDialogController.useResult { request { copy(fileInfo.id) } }.onSuccess {
                    toaster.showSuccess("done")
                }
            }
        }) {
            Icon(actionIcon(ActionGlyph.COPY), copyLabel)
        }
    }
}
