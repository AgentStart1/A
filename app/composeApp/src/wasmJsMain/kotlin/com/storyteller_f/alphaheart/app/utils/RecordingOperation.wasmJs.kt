/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

internal actual suspend fun <T> runRecordingOperation(block: suspend () -> T): T =
    try {
        block()
    } catch (failure: JsException) {
        // DOMException may have no JS stack; never retain it as a Kotlin exception cause.
        throw BrowserRecordingFailure(failure)
    }

private class BrowserRecordingFailure(failure: JsException) : RuntimeException(failure.message)
