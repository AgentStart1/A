/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.w3c.dom.HTMLIFrameElement
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsException
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString

private val pdfCoordination = Dispatchers.Default.limitedParallelism(1)

/** A same-origin document URL preserves cross-origin isolation while using the native PDF viewer. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun PdfView(url: String, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val host =
        remember(url, scope) {
            PdfPreviewHost(scope, pdfCoordination, Dispatchers.Default) { openBrowserPdf(url) }
        }
    DisposableEffect(host) { onDispose { host.close() } }
    val state by host.state.collectAsState()
    Box(modifier) {
        when (val current = state) {
            PdfPreviewState.Loading -> CenterBox { CircularProgressIndicator() }

            is PdfPreviewState.Failed -> CenterBox { ExceptionCell(current.cause, host::refresh) }

            is PdfPreviewState.Ready ->
                HtmlElementView(
                    factory = {
                        (document.createElement("iframe") as HTMLIFrameElement).apply {
                            title = "PDF document"
                            style.width = "100%"
                            style.height = "100%"
                            style.border = "0"
                            addEventListener("load", { setAttribute("data-pdf-loaded", "true") })
                            src = current.url
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = {
                        if (it.src != current.url) {
                            it.removeAttribute("data-pdf-loaded")
                            it.src = current.url
                        }
                    },
                )
        }
    }
}

private external interface BrowserPdfRequest : JsAny {
    val ready: Promise<JsString>
    fun close()
}

private fun openBrowserPdf(url: String): PdfPreviewDocument =
    try {
        BrowserPdfDocument(startPdfRequest(url.toJsString()))
    } catch (failure: JsException) {
        throw BrowserPdfFailure(failure, "Unable to open PDF preview")
    }

// DOM errors can lack a JavaScript stack; preserve their message without retaining an unsafe JS cause.
private class BrowserPdfFailure(failure: JsException, fallback: String) :
    IllegalStateException(failure.message ?: fallback)

private class BrowserPdfDocument(private val request: BrowserPdfRequest) : PdfPreviewDocument {
    override suspend fun awaitUrl(): String =
        try {
            request.ready.await().toString()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: JsException) {
            currentCoroutineContext().ensureActive()
            throw BrowserPdfFailure(failure, "Unable to load PDF preview")
        }

    override fun close() = request.close()
}

@JsFun(
    """(url) => {
    const controller = new AbortController();
    let objectUrl = null;
    const ready = (async () => {
        const response = await fetch(url, {credentials: 'omit', signal: controller.signal});
        if (!response.ok) throw new Error('PDF request failed: ' + response.status);
        const buffer = await response.arrayBuffer();
        if (controller.signal.aborted) throw new DOMException('PDF preview closed', 'AbortError');
        objectUrl = URL.createObjectURL(new Blob([buffer], {type: 'application/pdf'}));
        return objectUrl;
    })();
    return {
        ready,
        close() {
            controller.abort();
            if (objectUrl !== null) {
                URL.revokeObjectURL(objectUrl);
                objectUrl = null;
            }
        }
    };
}""",
)
private external fun startPdfRequest(url: JsString): BrowserPdfRequest
