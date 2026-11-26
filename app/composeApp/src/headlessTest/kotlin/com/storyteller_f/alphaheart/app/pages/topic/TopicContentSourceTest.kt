/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import com.storyteller_f.alphaheart.app.topic.asFeedPreview
import com.storyteller_f.alphaheart.app.topic.originalPlainContent
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class TopicContentSourceTest {
    @Test
    fun feedPreviewKeepsFullTopicAvailableForDetails() {
        val raw = "First paragraph.\n\nThe rest of the topic belongs in the details."
        val full = TopicContent.Plain(raw)
        val topic = TopicDetail.EMPTY.copy(topicInfo = TopicDetail.EMPTY.topicInfo.copy(content = full))

        val preview = topic.asFeedPreview()
        val excerpt = assertIs<TopicContent.Extracted>(preview.content)
        assertEquals("First paragraph.", excerpt.plain)
        assertEquals(raw, excerpt.originalPlainContent()?.plain)
        assertSame(full, topic.content)
        assertSame(preview, preview.asFeedPreview())
    }

    @Test
    fun extractedFeedActionsUseFullOriginalRatherThanDisplayedSummary() {
        val original = "# Full topic\n" + "A paragraph that is not present in the feed summary. ".repeat(20)
        val preview = TopicContent.Extracted("Short summary…", origin = original)
        val source = preview.originalPlainContent()
        assertEquals(original, source?.plain)
        assertEquals(preview.fileInfos, source?.fileInfos)
    }

    @Test
    fun unextractedContentKeepsItsOriginalObject() {
        val source = TopicContent.Plain("**Original text**")
        assertSame(source, source.originalPlainContent())
    }

    @Test
    fun unavailableOrEncryptedContentIsNeverTreatedAsPlaintext() {
        assertNull(TopicContent.Nil.originalPlainContent())
        assertNull(TopicContent.Encrypted("ciphertext", emptyMap()).originalPlainContent())
        assertNull(TopicContent.DecryptFailed("Unavailable").originalPlainContent())
    }

    @Test
    fun renderingNormalizationNeverRewritesTheFullOriginalTopicCopy() {
        val raw = listOf("# Original", "", "```kotlin", "val first = 1", "```", "").joinToString("\r\n")
        val content = TopicContent.Plain(raw)
        com.storyteller_f.shared.utils.extractMarkdownHeadline(raw)
        assertSame(content, content.originalPlainContent())
        assertEquals(raw, content.originalPlainContent()?.plain)
        assertEquals(raw, TopicContent.Extracted("summary", origin = raw).originalPlainContent()?.plain)
    }
}
