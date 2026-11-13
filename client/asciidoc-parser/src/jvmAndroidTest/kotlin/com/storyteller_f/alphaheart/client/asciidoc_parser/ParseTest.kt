/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.node.modules.NodeModuleModule
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.use

class ParseTest : PlatformHeadlessTest() {
    @Test
    fun testParseAsciiDoc() {
        val scriptDir =
            File(
                requireNotNull(System.getProperty("asciidoc.test.fixture")) { "Run this test through Gradle" },
            )
        assertTrue(File(scriptDir, "node_modules").isDirectory, "Gradle must prepare the AsciiDoc test dependencies")
        val scripts =
            File(scriptDir, "parse-asciidoc.js").bufferedReader().use {
                it.readText()
            }
        (V8Host.getNodeInstance().createV8Runtime() as NodeRuntime).use { runtime ->
            runtime.getNodeModule(NodeModuleModule::class.java)
                .setRequireRootDirectory(scriptDir)

            val result =
                runtime.getExecutor(scripts)
                    .executeString()
            assertEquals(
                """
                <div class="paragraph">
                <p>Hello, <em>Asciidoctor</em></p>
                </div>
                """.trimIndent(),
                result,
            )
        }
    }
}
