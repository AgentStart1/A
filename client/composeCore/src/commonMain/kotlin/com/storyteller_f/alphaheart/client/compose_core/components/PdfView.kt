/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ZoomIn
import com.composables.icons.lucide.ZoomOut
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.pdf_next_page
import com.storyteller_f.alphaheart.client.compose_core.pdf_previous_page
import com.storyteller_f.alphaheart.client.compose_core.pdf_zoom_in
import com.storyteller_f.alphaheart.client.compose_core.pdf_zoom_out
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

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
