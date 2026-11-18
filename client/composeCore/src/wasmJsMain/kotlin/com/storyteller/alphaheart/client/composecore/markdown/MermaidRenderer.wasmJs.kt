/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.composecore.markdown

import com.storyteller_f.mermaid_kmp.renderMermaid
import kotlin.js.JsException

internal actual fun renderMermaidSvg(input: String): String =
    try {
        renderMermaid(input)
    } catch (failure: JsException) {
        // Normalize the FFI JavaScript error so shared rendering can display its source fallback.
        throw BrowserMermaidFailure(failure)
    }

private class BrowserMermaidFailure(failure: JsException) :
    IllegalArgumentException("Mermaid rendering failed: ${failure.message}")
