/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.media.BrowserPdfPage
import com.storyteller_f.alphaheart.client.compose_core.media.BrowserPdfState
import com.storyteller_f.alphaheart.client.compose_core.media.BrowserPdfSurface
import com.storyteller_f.alphaheart.client.compose_core.media.renderPdfPreview
import com.storyteller_f.alphaheart.client.compose_core.media_retry
import com.storyteller_f.alphaheart.client.compose_core.pdf_preview_failed
import kotlinx.browser.document
import org.jetbrains.compose.resources.stringResource
import org.w3c.dom.HTMLElement
import kotlin.js.ExperimentalWasmJsInterop

/** Same-origin PDF.js pages replace opaque native plug-ins; readiness requires actual raster and text layers. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun PdfView(url: String, modifier: Modifier, showTools: Boolean) {
    val element = remember(url) { document.createElement("div") as HTMLElement }
    var attempt by remember(url) { mutableStateOf(0) }
    var state by remember(url, attempt) { mutableStateOf(BrowserPdfState.LOADING) }
    var page by remember(url, attempt) { mutableStateOf<BrowserPdfPage?>(null) }
    var renderer by remember(url, attempt) { mutableStateOf<BrowserPdfSurface?>(null) }
    var zoom by remember(url, attempt) { mutableFloatStateOf(1f) }
    LaunchedEffect(element, attempt) {
        renderPdfPreview(
            element,
            url,
            onState = { state = it },
            onPage = { index, count -> page = BrowserPdfPage(index, count) },
            onReady = { renderer = it },
        )
    }
    Column(modifier) {
        val currentPage = page
        val currentRenderer = renderer
        if (showTools && state == BrowserPdfState.READY && currentPage != null && currentRenderer != null) {
            PdfPreviewToolbar(currentPage.index, currentPage.count, currentRenderer::goToPage, zoom) { level ->
                zoom = level
                currentRenderer.setZoom(level.toDouble())
            }
        }
        BrowserPdfContent(element, state, Modifier.weight(1f).fillMaxWidth()) { attempt++ }
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun BrowserPdfContent(element: HTMLElement, state: BrowserPdfState, modifier: Modifier, retry: () -> Unit) {
    Box(modifier) {
        key(element) {
            HtmlElementView(
                factory = { element },
                modifier = Modifier.fillMaxSize(),
                update = {
                    it.style.setProperty("pointer-events", if (state == BrowserPdfState.READY) "auto" else "none")
                    it.style.visibility = if (state == BrowserPdfState.READY) "visible" else "hidden"
                },
            )
        }
        when (state) {
            BrowserPdfState.LOADING -> CenterBox { CircularProgressIndicator() }

            BrowserPdfState.FAILED ->
                CenterBox {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(Res.string.pdf_preview_failed))
                        ReadingTextButton(retry) { Text(stringResource(Res.string.media_retry)) }
                    }
                }

            BrowserPdfState.READY -> Unit
        }
    }
}
