/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.file_copy_address
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.media.LocalResourceDownloadAction
import com.storyteller_f.alphaheart.client.compose_core.media.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.media_download
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.copyText
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.shared.model.FileInfo
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Place this immediately below the page toolbar so cached refresh status overlays the content's top edge. */
@Composable
fun ReadingFilePreviewBody(
    handler: LoadingHandler<FileInfo>,
    modifier: Modifier,
    showChrome: Boolean = true,
    content: @Composable (FileInfo) -> Unit,
) {
    StateView(handler, modifier) { file ->
        if (showChrome) {
            val download = LocalResourceDownloadAction.current
            val resource = ResourceDownloadRequest(file.url, file.name, file.size)
            ReadingFilePreview(resource, { download(resource) }) {
                MediaMenuScope(showDownload = false, showHeading = false, showCopy = false) {
                    content(file)
                }
            }
        } else {
            content(file)
        }
    }
}

@Composable
fun ReadingFilePreview(resource: ResourceDownloadRequest, download: () -> Unit, content: @Composable () -> Unit) {
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
            ReadingFilePreviewHeading(resource, download)
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        }
    }
}

@Composable
fun ReadingFilePreviewHeading(resource: ResourceDownloadRequest, download: () -> Unit) {
    val clipboard = LocalClipboard.current
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
    val downloadLabel = stringResource(Res.string.media_download)
    val copyLabel = stringResource(Res.string.file_copy_address)
    val copy: () -> Unit = {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { clipboard.copyText(resource.url, toast) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            resource.fileName,
            Modifier.weight(1f).padding(end = 12.dp).appiumSemantics(text = resource.fileName),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ReadingIconButton(copy, Modifier.appiumSemantics(description = copyLabel, onClick = copy)) {
            ReadingIcon(actionIcon(ActionGlyph.COPY), contentDescription = copyLabel)
        }
        ReadingIconButton(
            download,
            Modifier.appiumSemantics(description = "download file", onClick = download),
        ) {
            ReadingIcon(navigationIcon(NavigationGlyph.DOWNLOAD), contentDescription = downloadLabel)
        }
    }
}
