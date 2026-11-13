/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.zt64.compose.pdf.component.PdfHorizontalPager
import dev.zt64.compose.pdf.rememberPdfState
import java.net.URI

@Composable
actual fun PdfView(url: String, modifier: Modifier) {
    PdfHorizontalPager(
        state = rememberPdfState(URI.create(url)),
        modifier = modifier,
    )
}
