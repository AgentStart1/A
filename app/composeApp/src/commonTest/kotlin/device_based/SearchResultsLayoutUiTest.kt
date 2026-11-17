/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.search.SearchContent
import com.storyteller_f.alphaheart.app.pages.search.SearchResultsLayout
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class SearchResultsLayoutUiTest {
    @Test
    fun unsupportedChildScopeExplainsTheCapabilityWithoutConstructingAnySessionOrRequest() =
        runAppComposeUiTest {
            val scope = mutableStateOf<SearchScope>(SearchScope.TopicTopic(123))
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(390.dp, 650.dp)) {
                        SearchContent(scope.value, "UI验证")
                    }
                }
            }
            // No session, navigation, or database is installed: constructing a search VM here would fail.
            onNodeWithText("Topic search is not available in this scope yet.").assertExists()
            runOnIdle { scope.value = SearchScope.TopicTopic(456) }
            onNodeWithText("Topic search is not available in this scope yet.").assertExists()
        }

    @Test
    fun resultTabsNeverCoverTheFirstAuthorAndLeaveTheRestOfTheViewportForResults() =
        runAppComposeUiTest {
            val width = mutableStateOf(390.dp)
            setContent {
                MaterialTheme {
                    Box(
                        Modifier.wrapContentSize(
                            Alignment.TopStart,
                            unbounded = true,
                        ).requiredSize(width.value, 650.dp),
                    ) {
                        SearchResultsLayout(
                            filters = {
                                ReadingTabRow(Modifier.testTag("filters")) {
                                    ReadingTab(true, {}) { Text("Topics") }
                                    ReadingTab(false, {}) { Text("Users") }
                                }
                            },
                            results = {
                                Column(Modifier.fillMaxSize().testTag("results")) {
                                    Text("First author", Modifier.testTag("first-author"))
                                    Text("Search result body")
                                }
                            },
                        )
                    }
                }
            }
            for (size in listOf(390.dp, 1280.dp)) {
                runOnIdle { width.value = size }
                val filters = onNodeWithTag("filters").fetchSemanticsNode().boundsInRoot
                val author = onNodeWithTag("first-author").fetchSemanticsNode().boundsInRoot
                val results = onNodeWithTag("results").fetchSemanticsNode().boundsInRoot
                assertTrue(author.top >= filters.bottom)
                assertTrue(results.top >= filters.bottom)
                assertTrue(results.height > filters.height * 3)
                assertTrue(author.left >= results.left && author.right <= results.right)
            }
        }
}
