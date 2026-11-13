/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

suspend fun buildAsciidocPreviewHtml(source: String): String {
    val documentHtml = convertAsciidoc(source)
    val previewHtml =
        """
        <!doctype html>
        <html lang="en">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>AsciiDoc Preview</title>
          <style>
            :root {
              color-scheme: light dark;
              font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
              line-height: 1.55;
            }
            body {
              box-sizing: border-box;
              max-width: 960px;
              margin: 0 auto;
              padding: 32px 24px;
            }
            pre {
              overflow-x: auto;
              padding: 16px;
              border-radius: 8px;
              background: color-mix(in srgb, CanvasText 8%, Canvas);
            }
          </style>
        </head>
        <body>
          <main id="preview">$documentHtml</main>
        </body>
        </html>
        """.trimIndent()
    return previewHtml
}

expect suspend fun convertAsciidoc(source: String): String

internal fun String.toJsStringLiteral(): String =
    buildString {
        append('"')
        for (char in this@toJsStringLiteral) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\u2028' -> append("\\u2028")
                '\u2029' -> append("\\u2029")
                else -> append(char)
            }
        }
        append('"')
    }
