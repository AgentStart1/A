/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsName

@OptIn(ExperimentalWasmJsInterop::class)
@JsModule("@asciidoctor/core")
private external object AsciidoctorModule {
    @JsName("default")
    fun create(): Asciidoctor
}

private external interface Asciidoctor {
    fun convert(source: String, options: AsciidoctorOptions): String
}

private external interface AsciidoctorOptions

actual suspend fun convertAsciidoc(source: String): String =
    AsciidoctorModule.create().convert(source, asciidoctorOptions())

@OptIn(ExperimentalWasmJsInterop::class)
private fun asciidoctorOptions(): AsciidoctorOptions =
    js(
        """
    ({ safe: 'safe', attributes: { showtitle: true } })
    """,
    )
