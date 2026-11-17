/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.media_retry
import com.storyteller_f.alphaheart.client.compose_core.pdf_preview_failed
import io.github.aakira.napier.Napier
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.w3c.dom.HTMLElement
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsException
import kotlin.js.JsString
import kotlin.js.Promise

private enum class BrowserPdfState { LOADING, READY, FAILED }

/** Same-origin PDF.js pages replace opaque native plug-ins; readiness requires actual raster and text layers. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun PdfView(url: String, modifier: Modifier) {
    val element = remember(url) { document.createElement("div") as HTMLElement }
    var attempt by remember(url) { mutableStateOf(0) }
    var state by remember(url) { mutableStateOf(BrowserPdfState.LOADING) }
    LaunchedEffect(element, attempt) { renderPdfPreview(element, url) { state = it } }
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
                        ReadingTextButton({ attempt++ }) { Text(stringResource(Res.string.media_retry)) }
                    }
                }

            BrowserPdfState.READY -> Unit
        }
    }
}

private suspend fun renderPdfPreview(element: HTMLElement, url: String, onState: (BrowserPdfState) -> Unit) {
    onState(BrowserPdfState.LOADING)
    val failure = CompletableDeferred<Exception>()
    var renderer: BrowserPdfSurface? = null
    try {
        renderer = createPdfPreview(element, url) { failure.complete(IllegalStateException(it)) }
        renderer.ready.await()
        onState(BrowserPdfState.READY)
        throw failure.await()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: JsException) {
        Napier.w("PDF preview failed (JavaScript error)")
        onState(BrowserPdfState.FAILED)
    } catch (error: Exception) {
        Napier.w("PDF preview failed (${error::class.simpleName})")
        onState(BrowserPdfState.FAILED)
    } finally {
        val owned = renderer
        if (owned != null) releasePdfPreview(owned)
    }
}

private suspend fun releasePdfPreview(renderer: BrowserPdfSurface) {
    withContext(NonCancellable) {
        try {
            renderer.close().await()
        } catch (_: JsException) {
            Napier.w("PDF preview cleanup failed (JavaScript error)")
        } catch (error: Exception) {
            Napier.w("PDF preview cleanup failed (${error::class.simpleName})")
        }
    }
}

@JsModule("pdf-preview-host")
internal external fun createPdfPreview(
    element: HTMLElement,
    url: String,
    onFailure: (String) -> Unit,
): BrowserPdfSurface

internal external interface BrowserPdfSurface : JsAny {
    val ready: Promise<JsString>
    fun close(): Promise<JsAny?>
}
