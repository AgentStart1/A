/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AsciidocPreviewTest : PlatformHeadlessTest() {
    @Test
    fun buildsPreviewHtmlWithConvertedDocument() =
        kotlinx.coroutines.test.runTest {
            val html =
                buildAsciidocPreviewHtml(
                    """
                    = Title

                    Hello, _AsciiDoc_ "reader"
                    """.trimIndent(),
                )

            assertContains(html, "<title>AsciiDoc Preview</title>")
            assertContains(html, "<h1>Title</h1>")
            assertContains(html, "Hello, <em>AsciiDoc</em> \"reader\"")
            assertFalse(html.contains("<script"))
        }
}
