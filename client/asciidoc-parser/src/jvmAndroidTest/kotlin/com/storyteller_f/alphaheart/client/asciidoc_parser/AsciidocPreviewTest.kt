/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AsciidocPreviewTest : PlatformHeadlessTest() {
    @Test
    fun embedsSourceAsInertTextAndLoadsTheBundledConverter() =
        runTest {
            val html = buildAsciidocPreviewHtml("= Title\r\n\r\n</template><script>bad()</script>&")
            assertContains(html, """<template id="source">= Title""")
            assertContains(html, "\n\n&lt;/template>&lt;script>bad()&lt;/script>&amp;")
            assertContains(html, "Content-Security-Policy")
            assertContains(html, "asciidoctor.convert(source")
            assertFalse(html.contains("<script>bad()"))
        }
}
