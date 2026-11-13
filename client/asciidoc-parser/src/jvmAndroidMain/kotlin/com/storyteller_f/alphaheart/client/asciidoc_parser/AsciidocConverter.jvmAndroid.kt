/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.use

private const val ASCIIDOCTOR_SCRIPT_PATH = "files/asciidoctor.min.js"

actual suspend fun convertAsciidoc(source: String): String =
    withContext(Dispatchers.Default) {
        val asciidoctorScript = Res.readBytes(ASCIIDOCTOR_SCRIPT_PATH).decodeToString()
        (V8Host.getNodeInstance().createV8Runtime() as NodeRuntime).use { runtime ->
            runtime.getExecutor(asciidoctorScript).executeVoid()
            runtime.getExecutor(
                "module\$build\$asciidoctor_browser.default().convert(" +
                    "${source.toJsStringLiteral()}, { safe: 'safe', attributes: { showtitle: true } })",
            ).executeString()
        }
    }
