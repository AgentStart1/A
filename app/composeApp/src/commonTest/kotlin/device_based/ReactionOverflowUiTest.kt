/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.components.EmojiRow
import com.storyteller_f.alphaheart.client.compose_core.components.Pill
import com.storyteller_f.alphaheart.client.compose_core.utils.readingTypography
import com.storyteller_f.shared.model.ReactionInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReactionOverflowUiTest {
    @Test
    fun resizingExposesOnlyPlacedEmojiAndOneOverflowAction() =
        runAppComposeUiTest {
            val emojis = listOf("😀", "❤️", "👍", "🎉", "👀", "🔥", "🚀", "👏", "😊", "🙏", "💡", "🤔")
            val data = emojis.mapIndexed { index, emoji -> ReactionInfo(emoji, 1, 1, false, index.toLong()) }
            val width = mutableStateOf(1280.dp)
            var overflowClicks = 0
            var added = 0
            var more = 0
            setContent {
                ReactionOverflowFixture(width.value, data, { overflowClicks++ }, { added++ }, { more++ })
            }

            val measuredLayouts = mutableListOf<String>()
            for (size in listOf(1280.dp, 390.dp, 1280.dp)) {
                runOnIdle { width.value = size }
                val bounds = onNodeWithTag("reaction-content-bounds").getUnclippedBoundsInRoot()
                assertEquals((if (size > 740.dp) 740.dp else size) - 36.dp, bounds.right - bounds.left)
                val actions = onAllNodes(hasClickAction()).fetchSemanticsNodes()
                val labels =
                    actions.flatMap { node ->
                        // A decorative icon can merge the same description into its one parent action.
                        // Distinct labels per action retain duplicate-action detection across separate nodes.
                        node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().distinct()
                    }
                val visible = emojis.count { "$it 1" in labels }
                val overflow = labels.filter { it.startsWith("+") }
                measuredLayouts +=
                    "viewport=$size; content=${bounds.right - bounds.left}; visible=$visible; overflow=$overflow"
                assertEquals(1, labels.count { it == "Add reaction" })
                assertEquals(1, labels.count { it == "More" })
                assertEquals(1, labels.count { it == "Comments" })
                if (visible < data.size) {
                    assertEquals(listOf("+${data.size - visible}"), overflow)
                    onNodeWithContentDescription(overflow.single()).performClick()
                } else {
                    assertTrue(overflow.isEmpty(), "No overflow is expected when all emoji are placed: $labels")
                }
                assertEquals(visible + 3 + overflow.size, actions.size)
                onNodeWithContentDescription("Add reaction").performClick()
                onNodeWithContentDescription("More").performClick()
            }
            assertTrue(overflowClicks > 0, "Actual topic controls must overflow and remain clickable: $measuredLayouts")
            assertEquals(3, added)
            assertEquals(3, more)
        }
}

@androidx.compose.runtime.Composable
private fun ReactionOverflowFixture(
    width: androidx.compose.ui.unit.Dp,
    data: List<ReactionInfo>,
    onOverflow: () -> Unit,
    onAdd: () -> Unit,
    onMore: () -> Unit,
) {
    MaterialTheme(typography = readingTypography) {
        Box(
            Modifier.wrapContentSize(Alignment.TopStart, unbounded = true).requiredSize(width, 300.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Match CommonTopicList (740dp pane / 10dp list inset) and TopicContentAndInteraction (8dp).
            Box(Modifier.widthIn(max = 740.dp).fillMaxWidth().padding(horizontal = 10.dp)) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    Box(Modifier.fillMaxWidth().testTag("reaction-content-bounds")) {
                        EmojiRow(
                            data,
                            false,
                            overflow = { shown ->
                                if (shown < data.size) Pill("+${data.size - shown}") { onOverflow() }
                                Pill(
                                    icon = Icons.Outlined.AddReaction,
                                    description = "Add reaction",
                                ) { onAdd() }
                                Pill(icon = Icons.Outlined.MoreHoriz, description = "More") { onMore() }
                            },
                            content = { index, showEmoji ->
                                if (index == 0) {
                                    Pill(
                                        "0",
                                        icon = Icons.AutoMirrored.Outlined.Comment,
                                        description = "Comments",
                                    ) {}
                                }
                                if (showEmoji) Pill("1 ", emoji = data[index].emoji) {}
                            },
                        )
                    }
                }
            }
        }
    }
}
