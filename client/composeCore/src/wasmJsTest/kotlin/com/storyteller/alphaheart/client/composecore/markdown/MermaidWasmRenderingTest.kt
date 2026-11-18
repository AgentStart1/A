/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.composecore.markdown

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class MermaidWasmRenderingTest {
    @Test
    fun invalidSequenceReturnsErrorAndAllowsNextRender() {
        val error =
            assertFailsWith<IllegalArgumentException> {
                renderMermaidSvg(
                    """
                    sequenceDiagram
                     participant Alice
                     Alice->>Alice: Start
                     Alice->>John: Hello
                    """.trimIndent(),
                )
            }
        assertTrue(error.message.orEmpty().contains("John"), "Expected the renderer's participant diagnostic")
        val svg = renderMermaidSvg("flowchart LR\n A[Still running] --> B[Ready]")
        assertTrue(svg.contains("Still running"))
        assertTrue(svg.contains("<svg"))
    }

    @Test
    fun wasmRendererReturnsTheActualFlowchartSvg() {
        val svg = renderMermaidSvg(MERMAID_SOURCE)

        assertTrue(svg.contains("<svg"), "Expected an SVG root element")
        assertTrue(svg.contains("Community"), "Expected the first graph label")
        assertTrue(svg.contains("Room"), "Expected the second graph label")
        assertTrue(svg.contains("Topic"), "Expected the third graph label")
    }
}

internal const val MERMAID_SOURCE = "graph LR\n  A[Community] --> B[Room]\n  B --> C[Topic]"
