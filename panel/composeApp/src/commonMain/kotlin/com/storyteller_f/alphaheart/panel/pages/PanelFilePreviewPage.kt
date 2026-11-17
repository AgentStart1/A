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
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.AudioViewFullScreen
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.PdfView
import com.storyteller_f.alphaheart.client.compose_core.components.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.VideoViewFullScreen
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.common.createPanelFileViewModel
import com.storyteller_f.alphaheart.panel.file_preview_title
import com.storyteller_f.alphaheart.panel.unsupported_content_type
import com.storyteller_f.alphaheart.panel.view_image
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
        StateView(fileInfoViewModel.handler, modifier = Modifier.padding(padding).fillMaxSize()) { fileInfo ->
            val contentType = fileInfo.contentType
            val name = fileInfo.name
            val url = fileInfo.url
            when {
                contentType.startsWith("video") -> {
                    val item =
                        RemoteMediaItem(
                            id = id.toString(),
                            url = url,
                            contentType = contentType,
                            name = name,
                        )
                    VideoViewFullScreen(item)
                }

                contentType.startsWith("audio") -> {
                    val item =
                        RemoteMediaItem(
                            id = id.toString(),
                            url = url,
                            contentType = contentType,
                            name = name,
                        )
                    AudioViewFullScreen(item)
                }

                contentType.startsWith("image") -> {
                    com.github.panpf.zoomimage.CoilZoomAsyncImage(
                        model = com.storyteller_f.alphaheart.client.compose_core.components.globalLoader(url),
                        contentDescription = stringResource(Res.string.view_image),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                contentType == com.storyteller_f.shared.model.FileInfo.PDF_CONTENT_TYPE -> {
                    PdfView(url, Modifier.fillMaxSize())
                }

                else -> {
                    CenterBox {
                        Text(stringResource(Res.string.unsupported_content_type, contentType))
                    }
                }
            }
        }
    }
}
