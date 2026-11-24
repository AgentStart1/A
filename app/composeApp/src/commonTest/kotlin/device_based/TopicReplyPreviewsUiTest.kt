/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.topic.TopicReplyPreviews
import com.storyteller_f.alphaheart.client.compose_core.utils.readingColorScheme
import com.storyteller_f.alphaheart.client.compose_core.utils.readingTypography
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.TopicReplyPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TopicReplyPreviewsUiTest {
    @Test
    fun excerptsWrapWithinTwoLinesWithoutSqueezingAvatarsAtBothWidths() =
        runAppComposeUiTest {
            val width = mutableStateOf(390.dp)
            val chat = mutableStateOf(false)
            val long = "A long reply with a readable excerpt. ".repeat(35)
            val replies = listOf(reply(1, "Short reply"), reply(2, long))
            setContent {
                MaterialTheme(colorScheme = readingColorScheme(false), typography = readingTypography) {
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Box(
                            Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                                .requiredSize(width.value, 720.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(Modifier.widthIn(max = 740.dp).fillMaxWidth()) {
                                TopicReplyPreviews(replies, chat = chat.value) {
                                    Box(Modifier.size(24.dp).testTag("avatar-${it.id}"))
                                }
                            }
                        }
                    }
                }
            }
            for ((viewport, chatMode) in listOf(390.dp to false, 1280.dp to false, 390.dp to true, 1280.dp to true)) {
                runOnIdle {
                    width.value = viewport
                    chat.value = chatMode
                }
                val panel = onNodeWithTag("topic-reply-previews").getUnclippedBoundsInRoot()
                assertEquals(minOf(viewport, 740.dp), panel.right - panel.left)
                val first = onNodeWithTag("topic-reply-1").getUnclippedBoundsInRoot()
                val second = onNodeWithTag("topic-reply-2").getUnclippedBoundsInRoot()
                assertEquals(first.bottom, second.top)
                for (id in listOf(1, 2)) {
                    val avatar = onNodeWithTag("avatar-$id").getUnclippedBoundsInRoot()
                    val summary = onNodeWithTag("topic-reply-summary-$id").getUnclippedBoundsInRoot()
                    assertEquals(24.dp, avatar.right - avatar.left)
                    assertEquals(24.dp, avatar.bottom - avatar.top)
                    assertEquals(if (chatMode) 22.dp else 10.dp, summary.left - avatar.right)
                    assertEquals(avatar.top + if (chatMode) 8.dp else 0.dp, summary.top)
                    assertTrue(summary.right <= panel.right)
                }
                val layouts = mutableListOf<TextLayoutResult>()
                onNodeWithTag("topic-reply-summary-2")
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertEquals(2, layouts.single().lineCount)
                val layout = layouts.single()
                // Skia reports the cutoff through the measured line end.
                assertTrue(layout.hasVisualOverflow)
                val visibleEnd = layout.getLineEnd(1, visibleEnd = true)
                assertTrue(layout.isLineEllipsized(1) || visibleEnd < layout.layoutInput.text.length)
            }
        }

    @Test
    fun excerptKeepsParentNavigationWhileAvatarKeepsItsOwnAction() =
        runAppComposeUiTest {
            var parentVisits = 0
            var avatarVisits = 0
            setContent {
                MaterialTheme {
                    Box(Modifier.size(390.dp, 300.dp).clickable { parentVisits++ }) {
                        TopicReplyPreviews(listOf(reply(1, "Reply summary"))) {
                            Box(Modifier.size(24.dp).testTag("author").clickable { avatarVisits++ })
                        }
                    }
                }
            }
            onNodeWithTag("author", useUnmergedTree = true).performClick()
            assertEquals(1, avatarVisits)
            assertEquals(0, parentVisits)
            onNodeWithTag("topic-reply-summary-1", useUnmergedTree = true).performTouchInput { click() }
            assertEquals(1, parentVisits)
            assertEquals(1, avatarVisits)
        }
}

private fun reply(id: Long, summary: String): TopicReplyPreview =
    TopicReplyPreview(TopicDetail.EMPTY.topicInfo.copy(id = id, content = TopicContent.Plain(summary)), null)
