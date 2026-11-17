/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import com.storyteller_f.alphaheart.app.pages.topic.parentReferenceSummary
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParentReferenceSummaryTest {
    @Test
    fun parentReferenceUsesPlainSummaryInsteadOfMarkdownControlCharactersOrLinkDestinations() {
        val summary =
            parentReferenceSummary(
                "# Heading\n\nA **bold** [link](https://example.invalid/private) and *italic*.",
            )
        assertTrue("Heading" in summary)
        assertTrue("bold" in summary)
        assertTrue("link" in summary)
        assertTrue("italic" in summary)
        assertFalse("#" in summary)
        assertFalse("**" in summary)
        assertFalse("https://" in summary)
        assertFalse("](" in summary)
    }

    @Test
    fun parentTextKeepsTheEndingBeyondTheOldHeadlinePreviewLimit() {
        val markdown =
            "# Heading\n\n" + "Paragraph with **formatting**. ".repeat(30) +
                "Ending after two hundred characters"
        val summary = parentReferenceSummary(markdown)
        assertTrue(summary.length > 200)
        assertTrue(summary.endsWith("Ending after two hundred characters"))
        assertFalse("**" in summary)
    }
}
