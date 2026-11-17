/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.storyteller_f.alphaheart.app.pages.topic.CommonTopicList
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.shared.model.TopicDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TopicDataRegionUiTest {
    @Test
    fun cachedSourceRefreshUsesPaneWidthAndPreservesReadingBounds() =
        runAppComposeUiTest {
            val model = TopicRegionModel()
            val width = mutableStateOf(1000.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 600.dp)) {
                        CommonTopicList(model) { _, item, _ ->
                            Text(
                                "Item ${item?.id ?: "null"}",
                                Modifier.fillMaxWidth().height(40.dp).testTag("row-${item?.id ?: "null"}"),
                            )
                        }
                    }
                }
            }
            awaitExternalCondition { onAllNodesWithTag("row-1").fetchSemanticsNodes().isNotEmpty() }
            for (viewport in listOf(1000.dp, 390.dp)) {
                runOnIdle { width.value = viewport }
                val pane = onNodeWithTag("topic-list-data-region").getUnclippedBoundsInRoot()
                val reading = onNodeWithTag("topic-list-reading-content").getUnclippedBoundsInRoot()
                assertEquals(viewport, pane.right - pane.left)
                assertEquals(minOf(viewport, 740.dp), reading.right - reading.left)
                val initialRow = onNodeWithTag("row-1").getUnclippedBoundsInRoot()
                val states =
                    listOf(LoadState.Loading, LoadState.Error(Exception("Offline")), LoadState.NotLoading(false))
                for (state in states) {
                    runOnIdle { model.refresh(state) }
                    awaitExternalCondition {
                        val loading =
                            onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
                                .fetchSemanticsNodes().isNotEmpty()
                        // Fixture rows are plain Text; the only click action is the real refresh-error retry.
                        val error = onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
                        when (state) {
                            is LoadState.Loading -> loading
                            is LoadState.Error -> error && !loading
                            is LoadState.NotLoading -> !error && !loading
                        }
                    }
                    assertEquals(pane, onNodeWithTag("topic-list-data-region").getUnclippedBoundsInRoot())
                    assertEquals(reading, onNodeWithTag("topic-list-reading-content").getUnclippedBoundsInRoot())
                    assertEquals(initialRow, onNodeWithTag("row-1").getUnclippedBoundsInRoot())
                    if (state is LoadState.Loading) {
                        val line =
                            onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
                                .getUnclippedBoundsInRoot()
                        assertEquals(pane.right - pane.left, line.right - line.left)
                    }
                }
            }
        }

    @Test
    fun switchingSourcesDoesNotShowAnotherSourcesRefreshAndKeepsScrollAnchor() =
        runAppComposeUiTest {
            val first = TopicRegionModel()
            val second = TopicRegionModel()
            val selected = mutableStateOf(first)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(1000.dp, 600.dp)) {
                        CommonTopicList(selected.value) { _, item, _ ->
                            Text(
                                "Item ${item?.id ?: "null"}",
                                Modifier.fillMaxWidth().height(40.dp).testTag("row-${item?.id ?: "null"}"),
                            )
                        }
                    }
                }
            }
            awaitExternalCondition { onAllNodesWithTag("row-1").fetchSemanticsNodes().isNotEmpty() }
            runOnIdle { first.refresh(LoadState.Loading) }
            awaitExternalCondition {
                onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            runOnIdle { selected.value = second }
            awaitExternalCondition {
                onAllNodesWithTag("row-1").fetchSemanticsNodes().isNotEmpty() &&
                    onAllNodes(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
                        .fetchSemanticsNodes()
                        .isEmpty()
            }
            onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertDoesNotExist()
            onNodeWithTag("topic-list-reading-content").performScrollToIndex(20)
            val anchor = onNodeWithTag("row-21").getUnclippedBoundsInRoot()
            runOnIdle { second.refresh(LoadState.Error(Exception("Offline"))) }
            awaitExternalCondition {
                onAllNodesWithTag("row-21").fetchSemanticsNodes().isNotEmpty() &&
                    onAllNodes(hasClickAction()).fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(anchor, onNodeWithTag("row-21").getUnclippedBoundsInRoot())
        }
}

private class TopicRegionModel : PagingViewModel<TopicDetail>() {
    private val rows = (1L..40L).map { TopicDetail.EMPTY.copy(topicInfo = TopicDetail.EMPTY.topicInfo.copy(id = it)) }
    override val flow = MutableStateFlow(page(LoadState.NotLoading(false)))

    fun refresh(state: LoadState) {
        flow.value = page(state)
    }

    private fun page(state: LoadState): PagingData<TopicDetail> =
        PagingData.from(
            rows,
            sourceLoadStates = LoadStates(state, LoadState.NotLoading(true), LoadState.NotLoading(true)),
        )
}
