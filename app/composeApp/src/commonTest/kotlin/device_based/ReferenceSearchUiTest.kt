/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.search.SearchPageLayout
import com.storyteller_f.alphaheart.app.pages.search.searchScopeLabel
import com.storyteller_f.alphaheart.app.pages.topic.ParentTopicReferenceContent
import com.storyteller_f.alphaheart.app.search.SearchScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ReferenceSearchUiTest {
    @Test
    fun parentQuoteKeepsTheConfirmedThreeRowsSpacingAndNavigationAtBothWidths() =
        runAppComposeUiTest {
            val width = mutableStateOf(390.dp)
            var visits = 0
            setContent {
                MaterialTheme {
                    Box(
                        Modifier.wrapContentSize(
                            Alignment.TopStart,
                            unbounded = true,
                        ).requiredSize(width.value, 400.dp),
                    ) {
                        ParentTopicReferenceContent("Writer", "A plain parent summary") { visits++ }
                    }
                }
            }
            for (size in listOf(390.dp, 1280.dp)) {
                runOnIdle { width.value = size }
                val card = onNodeWithContentDescription("View parent topic").getUnclippedBoundsInRoot()
                val author =
                    onNode(hasText("Reply to Writer") and !hasClickAction(), useUnmergedTree = true)
                        .getUnclippedBoundsInRoot()
                val summary =
                    onNode(hasText("A plain parent summary") and !hasClickAction(), useUnmergedTree = true)
                        .getUnclippedBoundsInRoot()
                val action =
                    onNode(hasText("View parent topic") and !hasClickAction(), useUnmergedTree = true)
                        .getUnclippedBoundsInRoot()
                assertEquals(if (size <= 600.dp) 15.dp else 21.dp, author.left - card.left)
                assertEquals(if (size <= 600.dp) 12.dp else 14.dp, author.top - card.top)
                assertEquals(8.dp, summary.top - author.bottom)
                assertEquals(8.dp, action.top - summary.bottom)
                assertTrue(action.bottom < card.bottom)
                onNodeWithContentDescription("View parent topic").performClick()
            }
            assertEquals(2, visits)
        }

    @Test
    fun entitySearchScopeNamesTheSearchedContentRatherThanOnlyTheOwnerKind() =
        runAppComposeUiTest {
            val scope = mutableStateOf<SearchScope>(SearchScope.CommunityTopic(1))
            setContent { Text(searchScopeLabel(scope.value)) }
            val cases =
                listOf(
                    SearchScope.CommunityTopic(1) to "Topics",
                    SearchScope.RoomTopic(1) to "Topics",
                    SearchScope.UserTopic(1) to "Topics",
                    SearchScope.CommunityRoom(1) to "Rooms",
                    SearchScope.CommunityMember(1) to "Members",
                    SearchScope.RoomMember(1) to "Members",
                    SearchScope.UserCommunities(1) to "Communities",
                    SearchScope.UserCreatedTitle(1) to "Titles",
                    SearchScope.UserReceivedTitle(1) to "Titles",
                )
            for ((target, content) in cases) {
                runOnIdle { scope.value = target }
                onNodeWithText("Search scope · $content").assertExists()
            }
        }

    @Test
    fun searchFormHasALabeledPrimaryButtonAndPreservesKeyboardSubmission() =
        runAppComposeUiTest {
            val query = mutableStateOf("")
            val submitted = mutableStateOf("")
            var submissions = 0
            val submit = {
                submissions++
                submitted.value = query.value
            }
            setContent {
                SearchPageLayout(query.value, submitted.value, { query.value = it }, submit, {}, {}, {
                    Text("Results: ${submitted.value}")
                })
            }
            val field = onNode(hasSetTextAction())
            field.performTextReplacement("needle")
            field.performImeAction()
            onNodeWithText("Results: needle").assertExists()
            assertEquals(1, submissions)
            onNode(hasText("Search") and hasClickAction()).performClick()
            assertEquals(2, submissions)
            assertEquals("needle", submitted.value)
        }
}
