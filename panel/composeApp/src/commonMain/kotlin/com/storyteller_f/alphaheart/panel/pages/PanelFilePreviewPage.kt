/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.storyteller_f.alphaheart.client.compose_core.components.AudioViewFilled
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.PdfView
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingFilePreviewBody
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingImageFilePreview
import com.storyteller_f.alphaheart.client.compose_core.components.VideoViewFullScreen
import com.storyteller_f.alphaheart.client.compose_core.components.globalLoader
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.createPanelFileViewModel
import com.storyteller_f.alphaheart.panel.file_preview_title
import com.storyteller_f.alphaheart.panel.unsupported_content_type
import com.storyteller_f.alphaheart.panel.view_image
import com.storyteller_f.shared.model.FileInfo
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelFilePreviewPage(id: Long) {
    val fileInfoViewModel = createPanelFileViewModel(id)
    val nav = LocalPanelNav.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(Res.string.file_preview_title)) },
            navigationIcon = { IconButton(nav::back) { Icon(actionIcon(ActionGlyph.ARROW_LEFT), "back") } },
        )
    }) { padding ->
        ReadingFilePreviewBody(
            fileInfoViewModel.handler,
            Modifier.padding(padding).fillMaxSize(),
            content = ::PanelFilePreviewContent,
        )
    }
}

@Composable
private fun PanelFilePreviewContent(file: FileInfo) {
    when {
        file.contentType.startsWith("video/") ->
            VideoViewFullScreen(RemoteMediaItem(file.id.toString(), file.url, file.contentType, file.name))

        file.contentType.startsWith("audio/") ->
            AudioViewFilled(RemoteMediaItem(file.id.toString(), file.url, file.contentType, file.name))

        file.contentType.startsWith("image/") -> {
            ReadingImageFilePreview(globalLoader(file.url), stringResource(Res.string.view_image))
        }

        file.contentType == FileInfo.PDF_CONTENT_TYPE -> PdfView(file.url, Modifier.fillMaxSize(), showTools = true)

        else -> CenterBox { Text(stringResource(Res.string.unsupported_content_type, file.contentType)) }
    }
}
