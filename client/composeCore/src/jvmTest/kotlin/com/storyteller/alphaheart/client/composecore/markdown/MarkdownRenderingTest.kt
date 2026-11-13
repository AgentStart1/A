/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.composecore.markdown

import com.hrm.latex.renderer.measure.LatexDimensions
import com.storyteller_f.alphaheart.client.compose_core.components.CodeFenceKind
import com.storyteller_f.alphaheart.client.compose_core.components.calculateMathPlaceholderSize
import com.storyteller_f.alphaheart.client.compose_core.components.codeFenceKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class MarkdownRenderingTest {
    @Test
    fun `render Mermaid flowchart as SVG`() {
        val svg =
            renderMermaidSvg(
                "flowchart LR\n    A[Start] --> B[Finish]",
            )

        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("Start"))
        assertTrue(svg.contains("Finish"))
    }

    @Test
    fun `route Mermaid code fence to Mermaid renderer`() {
        assertEquals(CodeFenceKind.MERMAID, codeFenceKind("mermaid"))
    }

    @Test
    fun `route unknown code fence to highlighted code`() {
        assertEquals(CodeFenceKind.CODE, codeFenceKind("kotlin"))
    }

    @Test
    fun `use measured math height for tall block formula`() {
        val measuredDimensions =
            LatexDimensions(
                widthPx = 180f,
                heightPx = 64f,
                baselinePx = 42f,
                contentWidthPx = 180f,
                contentHeightPx = 64f,
                contentBaselinePx = 42f,
            )

        val placeholder =
            calculateMathPlaceholderSize(
                dimensions = measuredDimensions,
                isInline = false,
                maxWidthPx = 320f,
                fallbackFontSizePx = 20f,
                contentLength = 120,
            )

        assertEquals(320f, placeholder.widthPx)
        assertEquals(64f, placeholder.heightPx)
    }
}
