/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.markdown

import kotlin.collections.get

internal enum class CodeFenceKind {
    REF,
    MATH,
    MERMAID,
    ASCIIDOC,
    OBJECT,
    CODE,
}

internal fun codeFenceKind(language: String): CodeFenceKind {
    val kind =
        when (language) {
            "com.storyteller_f.alphaheart", "c.s.a", "csa" -> CodeFenceKind.REF
            "math" -> CodeFenceKind.MATH
            "mermaid" -> CodeFenceKind.MERMAID
            "asciidoc" -> CodeFenceKind.ASCIIDOC
            "object" -> CodeFenceKind.OBJECT
            else -> CodeFenceKind.CODE
        }
    return kind
}
