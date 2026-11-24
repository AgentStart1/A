/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_reaction
import com.storyteller_f.alphaheart.app.components.Pill
import com.storyteller_f.alphaheart.app.components.TopicActionsRow
import com.storyteller_f.alphaheart.app.topic.TopicActionState
import com.storyteller_f.alphaheart.app.topic.TopicActionsPresentation
import com.storyteller_f.alphaheart.app.topic_action_comments
import com.storyteller_f.alphaheart.app.topic_action_react
import com.storyteller_f.alphaheart.app.topic_action_reply_count
import com.storyteller_f.alphaheart.app.topic_action_view_replies_count
import com.storyteller_f.alphaheart.app.topic_menu
import com.storyteller_f.alphaheart.client.compose_core.utils.readingTypography
import com.storyteller_f.shared.model.ReactionInfo
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TopicActionsPresentationUiTest {
    @Test
    fun actionCopySpacingAndNullableMainActionFollowTheirOwnContextAtBothWidths() =
        runAppComposeUiTest {
            val width = mutableStateOf(390.dp)
            val count = mutableStateOf(0L)
            val mode = mutableStateOf(TopicActionsPresentation.DETAIL_MAIN)
            var comments = 0
            val onComment: () -> Unit = { comments += 1 }
            var reactions = 0
            var menus = 0
            val reaction = getString(Res.string.add_reaction)
            val more = getString(Res.string.topic_menu)
            val reactText = getString(Res.string.topic_action_react)
            setContent {
                ActionFixture(width.value) {
                    TopicActionsRow(
                        TopicActionState(emptyList(), count.value, true, mode.value, reactionCount = 7),
                        onComment.takeUnless { mode.value == TopicActionsPresentation.DETAIL_MAIN },
                        { reactions += 1 },
                        { menus += 1 },
                        {},
                        {},
                    )
                }
            }
            for (commentCount in listOf(0L, 2L)) {
                for (size in listOf(390.dp, 1280.dp)) {
                    for (presentation in TopicActionsPresentation.entries) {
                        runOnIdle {
                            count.value = commentCount
                            width.value = size
                            mode.value = presentation
                        }
                        assertCommentPresentation(presentation, reaction, count.value)
                        paintedActionCaption(reactText, reaction).assertDoesNotExist()
                        paintedActionCaption("7", reaction).assertIsDisplayed()
                        onNodeWithContentDescription(reaction).performClick()
                        onNodeWithContentDescription(more).performClick()
                        val add = onNodeWithContentDescription(reaction).getUnclippedBoundsInRoot()
                        val menu = onNodeWithContentDescription(more).getUnclippedBoundsInRoot()
                        assertEquals(presentation.horizontalGap, menu.left - add.right)
                        assertTrue(menu.right <= size)
                    }
                }
            }
            assertEquals(12, comments)
            assertEquals(16, reactions)
            assertEquals(16, menus)
        }

    @Test
    fun aggregateChipsKeepSelectionCountAndSingleClickableOverflowWithContextActions() =
        runAppComposeUiTest {
            val emojis = listOf("😀", "❤️", "👍", "🎉", "👀", "🔥", "🚀", "👏", "😊", "🙏", "💡", "🤔")
            val data = emojis.mapIndexed { index, emoji -> ReactionInfo(emoji, 1, 1, index == 0, index.toLong()) }
            var overflowClicks = 0
            setContent {
                ActionFixture(390.dp) {
                    TopicActionsRow(
                        TopicActionState(data, 2, true, TopicActionsPresentation.FEED),
                        {},
                        {},
                        {},
                        { overflowClicks += 1 },
                    ) { index -> Pill("1", emoji = data[index].emoji, selected = index == 0) {} }
                }
            }
            onNodeWithContentDescription("😀 1").assertIsSelected().assertHeightIsEqualTo(32.dp)
            val labels =
                onAllNodes(hasClickAction()).fetchSemanticsNodes().flatMap {
                    it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().distinct()
                }
            val visible = emojis.count { "$it 1" in labels }
            assertTrue(visible in 1 until data.size)
            val pane = onNodeWithTag("topic-actions").fetchSemanticsNode().boundsInRoot
            onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
                assertTrue(node.boundsInRoot.left >= pane.left)
                assertTrue(node.boundsInRoot.right <= pane.right)
            }
            val overflow = labels.filter { it.startsWith("+") }
            assertEquals(listOf("+${data.size - visible}"), overflow)
            onNodeWithContentDescription(overflow.single()).performClick()
            assertEquals(1, overflowClicks)
        }
}

private fun commentLabel(mode: TopicActionsPresentation, count: Long): String? =
    if (mode == TopicActionsPresentation.DETAIL_MAIN) null else count.toString()

private suspend fun commentDescription(mode: TopicActionsPresentation, count: Long): String? =
    when (mode) {
        TopicActionsPresentation.CHAT_LIST -> getString(Res.string.topic_action_reply_count, count.toString())
        TopicActionsPresentation.DETAIL_REPLY -> getString(Res.string.topic_action_view_replies_count, count.toString())
        TopicActionsPresentation.FEED -> getString(Res.string.topic_action_comments, count.toString())
        TopicActionsPresentation.DETAIL_MAIN -> null
    }

@Composable
private fun ActionFixture(width: Dp, content: @Composable () -> Unit) {
    MaterialTheme(typography = readingTypography) {
        CompositionLocalProvider(LocalDensity provides Density(1f)) {
            Box(Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, 320.dp)) {
                Column {
                    Box(Modifier.fillMaxWidth().height(24.dp).testTag("action-body"))
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private suspend fun androidx.compose.ui.test.ComposeUiTest.assertCommentPresentation(
    presentation: TopicActionsPresentation,
    reaction: String,
    count: Long,
) {
    val commentDescription = commentDescription(presentation, count)
    if (commentDescription != null) {
        val commentText = assertNotNull(commentLabel(presentation, count))
        onNodeWithContentDescription(commentDescription).assertIsSelected().performClick()
        val caption = paintedActionCaption(commentText, commentDescription)
        caption.assertTextEquals(commentText)
        val result = mutableListOf<TextLayoutResult>()
        caption.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(result) }
        assertEquals(12.sp, result.single().layoutInput.style.fontSize)
        val comment = onNodeWithContentDescription(commentDescription).getUnclippedBoundsInRoot()
        val body = onNodeWithTag("action-body").getUnclippedBoundsInRoot()
        val next = onNodeWithContentDescription(reaction).getUnclippedBoundsInRoot()
        assertEquals(presentation.topGap, comment.top - body.bottom)
        assertEquals(presentation.horizontalGap, next.left - comment.right)
    } else {
        onNodeWithText(count.toString()).assertDoesNotExist()
        assertEquals(2, onAllNodes(hasClickAction()).fetchSemanticsNodes().size)
    }
}

@OptIn(ExperimentalTestApi::class)
private fun androidx.compose.ui.test.ComposeUiTest.paintedActionCaption(text: String, actionDescription: String) =
    onNode(
        hasText(text) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult) and
            hasAnyAncestor(hasContentDescription(actionDescription) and hasClickAction()),
        useUnmergedTree = true,
    )
