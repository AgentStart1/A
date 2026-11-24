/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.media

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLElement
import kotlin.js.JsAny
import kotlin.js.JsException
import kotlin.js.JsString
import kotlin.js.Promise

internal enum class BrowserPdfState { LOADING, READY, FAILED }

internal data class BrowserPdfPage(val index: Int, val count: Int)

internal suspend fun renderPdfPreview(
    element: HTMLElement,
    url: String,
    onState: (BrowserPdfState) -> Unit,
    onPage: (Int, Int) -> Unit,
    onReady: (BrowserPdfSurface) -> Unit,
) {
    onState(BrowserPdfState.LOADING)
    val failure = CompletableDeferred<Exception>()
    var renderer: BrowserPdfSurface? = null
    try {
        renderer = createPdfPreview(element, url, { failure.complete(IllegalStateException(it)) }, onPage)
        renderer.ready.await()
        onReady(renderer)
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
    onPageChange: ((Int, Int) -> Unit)?,
): BrowserPdfSurface

internal external interface BrowserPdfSurface : JsAny {
    val ready: Promise<JsString>
    fun goToPage(index: Int)
    fun setZoom(level: Double)
    fun close(): Promise<JsAny?>
}
