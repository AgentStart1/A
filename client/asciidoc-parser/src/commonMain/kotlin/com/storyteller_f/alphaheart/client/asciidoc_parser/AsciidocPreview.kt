/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

suspend fun buildAsciidocPreviewHtml(source: String): String {
    // Keep normalization at this independent renderer boundary without a dependency on business/shared modules.
    val renderingSource = source.replace("\r\n", "\n").replace('\r', '\n')
    return buildPlatformAsciidocPreview(renderingSource)
}

internal expect suspend fun buildPlatformAsciidocPreview(source: String): String

internal fun wrapAsciidocPreview(documentHtml: String, head: String = "", scripts: String = ""): String =
    """
    <!doctype html>
    <html lang="en">
    <head>
      <meta charset="utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1">
      $head
      <title>AsciiDoc Preview</title>
      <style>
        :root {
          color-scheme: light dark;
          font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
          line-height: 1.65;
          background: light-dark(#fff, #1d2a23);
          color: light-dark(#22382e, #e2ede5);
        }
        body {
          box-sizing: border-box;
          max-width: 760px;
          margin: 0 auto;
          padding: clamp(20px, 5vw, 48px);
          overflow-wrap: anywhere;
        }
        h1, h2 {
          margin: 0 0 .65em;
        }
        .admonitionblock {
          margin-top: 24px;
          border-inline-start: 3px solid light-dark(#32634d, #aed4b5);
          padding: 8px 14px;
          background: light-dark(#f3f6f2, #142019);
        }
        .admonitionblock td.content {
          padding-inline-start: 12px;
        }
        img {
          max-width: 100%;
          height: auto;
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
      $scripts
    </body>
    </html>
    """.trimIndent()
