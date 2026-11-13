/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.URI
import kotlin.test.assertContains
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class DesktopBrowserCapture private constructor(val command: File, private val capturedUriFile: File) {
    suspend fun assertOpenedAsciidocPreview(expectedSource: String) {
        withTimeout(15.seconds) {
            while (!capturedUriFile.isFile || capturedUriFile.readText().isBlank()) {
                delay(200)
            }
        }
        val uri = capturedUriFile.readText().trim()
        assertTrue(uri.startsWith("file:"), "Expected a file URI, but got $uri")
        val previewFile = File(URI(uri))
        assertTrue(previewFile.isFile, "Generated AsciiDoc HTML file does not exist: $uri")
        val previewHtml = previewFile.readText()
        expectedSource.lineSequence().filter(String::isNotBlank).forEach { sourceLine ->
            assertContains(previewHtml, sourceLine)
        }
    }

    companion object {
        fun create(directory: File): DesktopBrowserCapture {
            check(directory.mkdirs()) { "Failed to create browser capture directory" }
            val capturedUriFile = File(directory, "opened-uri.txt")
            val command = File(directory, "xdg-open")
            command.writeText(
                """
                #!/bin/sh
                printf '%s' "${'$'}1" > "${capturedUriFile.canonicalPath}"
                """.trimIndent(),
            )
            check(command.setExecutable(true)) { "Failed to make browser capture command executable" }
            return DesktopBrowserCapture(command, capturedUriFile)
        }
    }
}
