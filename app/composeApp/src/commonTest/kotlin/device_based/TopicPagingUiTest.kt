/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.storyteller_f.alphaheart.api.TopicInfoListResponse
import com.storyteller_f.alphaheart.app.pages.topic.CommonTopicList
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.buildPager
import com.storyteller_f.alphaheart.client.core.RequestError
import com.storyteller_f.alphaheart.client.room.RemoteKeyRoomStorage
import com.storyteller_f.alphaheart.client.room.RoomTopicInfoStorage
import com.storyteller_f.alphaheart.client.room.getRoomDatabase
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.storage.RemoteKeyStorageWrapper
import com.storyteller_f.storage.TopicCollection
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
internal class TopicPagingUiTest {
    @Test
    fun retryPreservesDeepTopicKeyAndOffset() =
        runAppComposeUiTest {
            val fixture = TopicPagingFixture()
            try {
                fixture.seed()
                setContent { TopicPagingContent(fixture.model) }
                awaitExternalCondition {
                    onAllNodesWithText("Refresh failed · Retry").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithTag("topic-list-reading-content").performScrollToIndex(24)
                val tag = "topic-row-${fixture.rows[23].id}"
                onNodeWithTag(tag).assertIsDisplayed()
                val before = onNodeWithTag(tag).getUnclippedBoundsInRoot().top
                runOnIdle { fixture.revision.value = 1 }
                onNodeWithText("Refresh failed · Retry").performClick()
                awaitExternalCondition { fixture.committed.value == 1 }
                awaitExternalCondition {
                    onAllNodesWithText("Topic 23 revision 1").fetchSemanticsNodes().isNotEmpty()
                }
                onNodeWithTag(tag).assertIsDisplayed()
                assertEquals(before, onNodeWithTag(tag).getUnclippedBoundsInRoot().top)
                assertEquals(listOf(null, null, "opaque/unpinned", "opaque/last"), fixture.requests.value)
            } finally {
                closeTestDatabase(fixture.database, fixture.pagingScope)
            }
        }

    @Test
    fun transientEmptyPageRetainsReadingPositionForSameSource() =
        runAppComposeUiTest {
            val rows = topicPagingRows()
            val pages = MutableStateFlow(PagingData.from(rows))
            val model =
                object : PagingViewModel<TopicDetail>() {
                    override val flow = pages
                }
            setContent { TopicPagingContent(model) }
            awaitTopicPagingPhase("Initial topic page did not appear") {
                onAllNodesWithText("Topic 0 revision 0").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("topic-list-reading-content").performScrollToIndex(24)
            val tag = "topic-row-${rows[23].id}"
            onNodeWithTag(tag).assertIsDisplayed()
            val before = onNodeWithTag(tag).getUnclippedBoundsInRoot().top
            runOnIdle { pages.value = PagingData.empty() }
            awaitTopicPagingPhase("Transient empty page did not replace the reading list") {
                onAllNodesWithTag("topic-list-reading-content").fetchSemanticsNodes().isEmpty()
            }
            runOnIdle { pages.value = PagingData.from(rows.map(::updatedTopic)) }
            awaitTopicPagingPhase("Refilled topic page did not restore topic 23") {
                onAllNodesWithText("Topic 23 revision 1").fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag(tag).assertIsDisplayed()
            assertEquals(before, onNodeWithTag(tag).getUnclippedBoundsInRoot().top)
        }
}

@OptIn(ExperimentalTestApi::class)
private suspend fun ComposeUiTest.awaitTopicPagingPhase(message: String, condition: () -> Boolean) {
    try {
        awaitExternalCondition(condition = condition)
    } catch (failure: TimeoutCancellationException) {
        val renderedTopics =
            onAllNodes(hasText("Topic ", substring = true)).fetchSemanticsNodes().map {
                it.config.getOrNull(SemanticsProperties.Text)
            }
        throw AssertionError("$message. Rendered topics: $renderedTopics", failure)
    }
}

@Composable
private fun TopicPagingContent(model: PagingViewModel<TopicDetail>) {
    MaterialTheme {
        Box(Modifier.requiredSize(390.dp, 400.dp)) {
            CommonTopicList(model) { _, info, _ ->
                Text(
                    (info?.content as? TopicContent.Plain)?.plain.orEmpty(),
                    Modifier.height(64.dp).testTag("topic-row-${info?.id ?: 0L}"),
                )
            }
        }
    }
}

private fun topicPagingRows() =
    (0 until 28).map {
        val info =
            TopicDetail.EMPTY.topicInfo.copy(
                id = (it * 37 % 101 + 1).toLong(),
                content = TopicContent.Plain("Topic $it revision 0"),
            )
        TopicDetail.EMPTY.copy(topicInfo = info)
    }

private fun updatedTopic(topic: TopicDetail): TopicDetail {
    val body = (topic.content as TopicContent.Plain).plain.replace("revision 0", "revision 1")
    return topic.copy(topicInfo = topic.topicInfo.copy(content = TopicContent.Plain(body)))
}

private class TopicPagingFixture {
    val pagingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database = getRoomDatabase("topic-paging-${Uuid.random().toHexString()}")
    private val storage = RoomTopicInfoStorage(database)
    private val collection = TopicCollection.ChildTopicList(1)
    val revision = MutableStateFlow(0)
    val committed = MutableStateFlow(-1)
    val requests = MutableStateFlow<List<String?>>(emptyList())
    val rows = topicPagingRows()
    val model =
        object : PagingViewModel<TopicDetail>() {
            override val flow =
                buildPager(
                    collection,
                    RemoteKeyStorageWrapper(RemoteKeyRoomStorage(database), "topic-paging-fixture"),
                    storage,
                    onUpdated = { committed.value = revision.value },
                ) { token, _ ->
                    requests.update { it + token }
                    if (revision.value == 0) {
                        CustomResult.failure(RequestError.from(IllegalStateException("offline")))
                    } else {
                        val page =
                            when (token) {
                                null -> emptyList<TopicDetail>() to "opaque/unpinned"
                                "opaque/unpinned" -> rows.take(20) to "opaque/last"
                                "opaque/last" -> rows.drop(20) to null
                                else -> error("Unexpected opaque token")
                            }
                        val data = page.first.map(::updatedTopic)
                        CustomResult.success(
                            TopicInfoListResponse(data.toImmutableList(), Pagination(page.second, null, 28)),
                        )
                    }
                }.flow.cachedIn(pagingScope)
        }

    suspend fun seed() = storage.replace(collection, rows)
}
