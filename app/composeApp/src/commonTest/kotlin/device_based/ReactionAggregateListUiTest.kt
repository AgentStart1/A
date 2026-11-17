/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.storyteller_f.alphaheart.app.pages.topic.ReactionAggregateList
import com.storyteller_f.alphaheart.app.pages.topic.ReactionAggregatePagingList
import com.storyteller_f.alphaheart.client.compose_core.utils.readingTypography
import com.storyteller_f.shared.model.ReactionInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReactionAggregateListUiTest {
    @Test
    fun twelveEmojiWithEighteenReactionsStayCompactAndBoundedAtBothWidths() =
        runAppComposeUiTest {
            val emojis = listOf("😀", "❤️", "👍", "🎉", "👀", "🔥", "🚀", "👏", "😊", "🙏", "💡", "🤔")
            val rows =
                emojis.mapIndexed { index, emoji ->
                    ReactionInfo(emoji, 1, if (index < 6) 2 else 1, false, index.toLong())
                }
            assertEquals(18L, rows.sumOf { it.count })
            val width = mutableStateOf(390.dp)
            setContent {
                MaterialTheme(typography = readingTypography) {
                    Box(
                        Modifier.wrapContentSize(
                            Alignment.TopStart,
                            unbounded = true,
                        ).requiredSize(width.value, 760.dp),
                    ) {
                        ReactionAggregateList(rows = rows)
                    }
                }
            }
            for (size in listOf(390.dp, 1280.dp)) {
                runOnIdle { width.value = size }
                val pane = onNodeWithTag("reaction-aggregate-list").getUnclippedBoundsInRoot()
                assertEquals(if (size > 740.dp) 740.dp else size, pane.right - pane.left)
                assertEquals((size - (pane.right - pane.left)) / 2, pane.left)
                rows.forEachIndexed { index, info ->
                    onNodeWithTag("reaction-emoji-$index").assertTextEquals(info.emoji)
                    onNodeWithTag("reaction-count-$index").assertTextEquals(info.count.toString())
                    val emoji = onNodeWithTag("reaction-emoji-$index").getUnclippedBoundsInRoot()
                    val count = onNodeWithTag("reaction-count-$index").getUnclippedBoundsInRoot()
                    val row = onNodeWithTag("reaction-aggregate-row-$index").getUnclippedBoundsInRoot()
                    assertEquals(20.dp, emoji.left - pane.left)
                    assertEquals(12.dp, count.left - emoji.right)
                    assertTrue(count.right < row.left + 110.dp)
                    assertTrue(row.top >= pane.top && row.bottom <= pane.bottom)
                    if (index < rows.lastIndex) {
                        onAllNodesWithTag("reaction-divider-$index").assertCountEquals(1)
                        val divider = onNodeWithTag("reaction-divider-$index").getUnclippedBoundsInRoot()
                        val next = onNodeWithTag("reaction-aggregate-row-${index + 1}").getUnclippedBoundsInRoot()
                        assertEquals(12.dp, divider.top - row.bottom)
                        assertEquals(12.dp, next.top - divider.bottom)
                    }
                }
                onNodeWithTag("reaction-divider-${rows.lastIndex}").assertDoesNotExist()
            }
        }

    @Test
    fun pagingShrinkAndRouteDisposalDoNotReadKeysFromTheReplacementGeneration() =
        runAppComposeUiTest {
            val rows =
                listOf("😀", "❤️", "👍", "🎉", "👀", "🔥", "🚀", "👏", "😊", "🙏", "💡", "🤔")
                    .mapIndexed { index, emoji -> ReactionInfo(emoji, 1, 1, false, index.toLong()) }
            val paging = MutableStateFlow(PagingData.from(rows))
            val shown = mutableStateOf(true)
            val presenter = CompletableDeferred<LazyPagingItems<ReactionInfo>>()
            setContent {
                MaterialTheme {
                    val items = paging.collectAsLazyPagingItems()
                    SideEffect { presenter.complete(items) }
                    if (shown.value) {
                        Box(Modifier.requiredSize(390.dp, 760.dp)) {
                            ReactionAggregatePagingList(items)
                        }
                    }
                }
            }
            val items = withContext(Dispatchers.Default) { withTimeout(1000) { presenter.await() } }
            awaitPagingCount(items, 12)
            awaitIdle()
            onAllNodesWithTag("reaction-aggregate-row-11").assertCountEquals(1)
            runOnIdle { paging.value = PagingData.from(rows.take(2)) }
            awaitPagingCount(items, 2)
            awaitIdle()
            onAllNodesWithTag("reaction-aggregate-row-2").assertCountEquals(0)
            onNodeWithTag("reaction-emoji-1").assertTextEquals("❤️")
            // Navigation can clear the pager before its old LazyColumn has finished disposal/measurement.
            runOnIdle {
                paging.value = PagingData.empty()
                shown.value = false
            }
            onNodeWithTag("reaction-aggregate-list").assertDoesNotExist()
            awaitPagingCount(items, 0)
            runOnIdle { shown.value = true }
            onAllNodesWithTag("reaction-aggregate-row-0").assertCountEquals(0)
            runOnIdle { paging.value = PagingData.from(rows.takeLast(1)) }
            awaitPagingCount(items, 1)
            awaitIdle()
            onAllNodesWithTag("reaction-aggregate-row-0").assertCountEquals(1)
            onNodeWithTag("reaction-emoji-0").assertTextEquals("🤔")
            onNodeWithTag("reaction-divider-0").assertDoesNotExist()
        }
}

@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.awaitPagingCount(items: LazyPagingItems<ReactionInfo>, count: Int) {
    // LazyPagingItems publishes Compose snapshot state on Main. Drain pending frames/notifications while
    // awaiting that presentation, not only the external Flow; the shared boundary preserves the 1000 ms budget.
    awaitExternalCondition { items.itemCount == count }
}
