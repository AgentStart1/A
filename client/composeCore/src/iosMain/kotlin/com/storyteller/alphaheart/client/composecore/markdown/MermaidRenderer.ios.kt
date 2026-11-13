/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.composecore.markdown

internal actual fun renderMermaidSvg(input: String): String = throw UnsupportedMermaidException(input.length)

private class UnsupportedMermaidException(inputLength: Int) :
    UnsupportedOperationException("Mermaid rendering is not supported on iOS ($inputLength input characters)")
