/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.pdf_preview_failed
import dev.zt64.compose.pdf.PdfState
import dev.zt64.compose.pdf.component.PdfHorizontalPager
import dev.zt64.compose.pdf.rememberPdfState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import java.io.IOException
import java.net.URI

@Composable
actual fun PdfView(url: String, modifier: Modifier, showTools: Boolean) {
    val state = rememberPdfState(URI.create(url))
    val pager = rememberPagerState { state.pageCount }
    val scope = rememberCoroutineScope()
    var zoom by remember(url) { mutableFloatStateOf(1f) }
    Column(modifier) {
        if (showTools && state.pageCount > 0) {
            PdfPreviewToolbar(pager.currentPage, state.pageCount, { page ->
                scope.launch { pager.scrollToPage(page) }
            }, zoom, { zoom = it })
        }
        PdfHorizontalPager(
            state = state,
            pagerState = pager,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top,
            page = { index -> key(state, index) { PreviewPdfPage(state, index, zoom) } },
        )
    }
}

@Composable
private fun PreviewPdfPage(state: PdfState, index: Int, zoom: Float) {
    val loaded by produceState<Result<ImageBitmap>?>(null, state, index) {
        value =
            try {
                Result.success(state.loadPage(index))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                Result.failure(error)
            } catch (error: IllegalStateException) {
                Result.failure(error)
            }
    }
    val bitmap = loaded?.getOrNull()
    if (bitmap != null) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val pageWidth = maxWidth * zoom
            Box(
                Modifier.fillMaxSize()
                    .horizontalScroll(rememberScrollState(), enabled = zoom > 1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Image(
                    bitmap,
                    "Page $index",
                    Modifier.width(pageWidth).aspectRatio(bitmap.width.toFloat() / bitmap.height),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loaded == null) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(Res.string.pdf_preview_failed))
            }
        }
    }
}
