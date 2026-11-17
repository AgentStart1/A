/*
 * This is a private project. All rights reserved.
 */

package com.mohamedrejeb.richeditor.parser.markdown

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalRichTextApi::class)
class RichEditorMarkdownRoundTripTest {
    @Test
    fun equalsAndInlineMathKeepTheirOriginalSource() {
        val sources =
            listOf(
            "a=b",
            "${'$'}x^2+y^2=z^2${'$'}",
            "before ${'$'}x=y${'$'} after",
            "行内公式： ${'$'}x^2+y^2=z^2${'$'} ，数学与中文在同一段落中排版。",
        )
        sources.forEach { source ->
            val state = RichTextState().setMarkdown(source)
            assertEquals(source, state.annotatedString.text, "Visible editing text: $source")
            assertEquals(source, state.toMarkdown(), "Markdown round trip: $source")
        }
    }

    @Test
    fun codeAndInlineFormattingStillRoundTrip() {
        listOf("`x^2+y^2=z^2`", "**bold** and *italic*", "**bold** ${'$'}x=y${'$'} and `a=b`")
            .forEach { source -> assertEquals(source, RichTextState().setMarkdown(source).toMarkdown()) }
    }

    @Test
    fun selectionDoesNotChangeTheDocumentAndFormattingPreservesMath() {
        val state = RichTextState().setMarkdown("Plain ${'$'}x^2+y^2=z^2${'$'} and `a=b`")
        val original = state.toRichTextDocument()
        state.selection = TextRange(0, 5)
        assertEquals(original, state.toRichTextDocument(), "Selection is not a document mutation")
        state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        val markdown = state.toMarkdown()
        assertTrue(markdown.contains("***Plain***"), markdown)
        assertTrue(markdown.contains("${'$'}x^2+y^2=z^2${'$'}"), markdown)
        assertTrue(markdown.contains("`a=b`"), markdown)
    }
}
