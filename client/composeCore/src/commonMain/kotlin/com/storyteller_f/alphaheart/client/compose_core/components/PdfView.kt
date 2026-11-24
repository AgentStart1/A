/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ZoomIn
import com.composables.icons.lucide.ZoomOut
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.pdf_next_page
import com.storyteller_f.alphaheart.client.compose_core.pdf_previous_page
import com.storyteller_f.alphaheart.client.compose_core.pdf_zoom_in
import com.storyteller_f.alphaheart.client.compose_core.pdf_zoom_out
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.copyText
import com.storyteller_f.shared.model.FileInfo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@Composable
fun PdfViewBlock(fileInfo: FileInfo, onClick: (FileInfo) -> Unit) {
    val url = fileInfo.url
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier =
        Modifier
            .appiumSemantics(testTag = "uploaded-pdf-attachment")
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer, shape)
            .clickable { onClick(fileInfo) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PictureAsPdf, "pdf")
            Text(
                fileInfo.name,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            IconButton(
                onClick = { onClick(fileInfo) },
                modifier =
                Modifier.appiumSemantics(
                    description = "open file ${fileInfo.name}",
                    onClick = { onClick(fileInfo) },
                ),
            ) {
                Icon(Icons.Default.Fullscreen, "fullscreen")
            }
        }
        val toasterState = LocalToaster.current
        val scope = rememberCoroutineScope()
        FlowRow {
            val clipboardManager = LocalClipboard.current
            IconButton({
                scope.launch {
                    clipboardManager.copyText(url, toasterState)
                }
            }) {
                Icon(Icons.Default.ContentCopy, "copy list")
            }
            val uriHandler = LocalUriHandler.current
            IconButton({
                uriHandler.openUri(url)
            }) {
                Icon(Icons.Default.Download, "download")
            }
        }
    }
}

// Renders a remote PDF using the platform PDF viewer.
@Composable
expect fun PdfView(url: String, modifier: Modifier, showTools: Boolean = false)

@Composable
internal fun PdfPreviewToolbar(
    pageIndex: Int,
    pageCount: Int,
    goToPage: (Int) -> Unit,
    zoom: Float,
    setZoom: (Float) -> Unit,
) {
    Row(Modifier.fillMaxWidth().testTag("pdf-preview-toolbar"), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ goToPage(pageIndex - 1) }, enabled = pageIndex > 0) {
            Icon(Lucide.ChevronLeft, stringResource(Res.string.pdf_previous_page))
        }
        Text("${pageIndex + 1} / $pageCount")
        IconButton({ goToPage(pageIndex + 1) }, enabled = pageIndex + 1 < pageCount) {
            Icon(actionIcon(ActionGlyph.CHEVRON_RIGHT), stringResource(Res.string.pdf_next_page))
        }
        IconButton({ setZoom((zoom - 0.25f).coerceAtLeast(0.75f)) }, enabled = zoom > 0.75f) {
            Icon(Lucide.ZoomOut, stringResource(Res.string.pdf_zoom_out))
        }
        IconButton({ setZoom((zoom + 0.25f).coerceAtMost(2f)) }, enabled = zoom < 2f) {
            Icon(Lucide.ZoomIn, stringResource(Res.string.pdf_zoom_in))
        }
    }
}
