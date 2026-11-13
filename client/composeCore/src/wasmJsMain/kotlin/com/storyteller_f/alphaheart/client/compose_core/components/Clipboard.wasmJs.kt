/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsException
import kotlin.js.Promise

actual suspend fun Clipboard.setText(string: String) {
    writeBrowserClipboardText(string)
}

@OptIn(ExperimentalWasmJsInterop::class)
internal suspend fun writeBrowserClipboardText(text: String) {
    try {
        writeClipboardText(text).await()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: JsException) {
        currentCoroutineContext().ensureActive()
        // DOM errors may lack a stack; do not retain an unsafe JavaScript cause.
        throw BrowserClipboardFailure(failure)
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
private class BrowserClipboardFailure(failure: JsException) :
    IllegalStateException(failure.message ?: "Unable to write browser clipboard")

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(text) => navigator.clipboard.writeText(text)")
private external fun writeClipboardText(text: String): Promise<JsAny?>
