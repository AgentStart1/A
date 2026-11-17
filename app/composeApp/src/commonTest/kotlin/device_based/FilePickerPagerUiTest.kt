/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.topic.FilePickerPager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FilePickerPagerUiTest {
    @Test
    fun accessibilityScrollKeepsAttachmentPageAligned() =
        runAppComposeUiTest {
            setContent {
                val pager = rememberPagerState { 2 }
                FilePickerPager(pager, Modifier.requiredSize(390.dp, 300.dp).testTag("picker")) { page ->
                    Box(Modifier.fillMaxSize().testTag("page-$page")) { Text("Page $page") }
                }
            }
            onNodeWithTag("picker").performSemanticsAction(SemanticsActions.ScrollBy) { scroll ->
                assertTrue(scroll(133.2f, 0f))
            }
            val viewport = onNodeWithTag("picker").getUnclippedBoundsInRoot()
            val files = onNodeWithTag("page-0").getUnclippedBoundsInRoot()
            assertEquals(viewport.left, files.left)
            assertEquals(viewport.right, files.right)

            onNodeWithTag("picker").performTouchInput { swipeLeft() }
            val recorder = onNodeWithTag("page-1").getUnclippedBoundsInRoot()
            assertEquals(viewport.left, recorder.left)
            assertEquals(viewport.right, recorder.right)
            onNodeWithTag("picker").performSemanticsAction(SemanticsActions.ScrollBy) { scroll ->
                assertTrue(scroll(-133.2f, 0f))
            }
            val alignedRecorder = onNodeWithTag("page-1").getUnclippedBoundsInRoot()
            assertEquals(viewport.left, alignedRecorder.left)
            assertEquals(viewport.right, alignedRecorder.right)

            onNodeWithTag("picker").performTouchInput { swipeRight() }
            val returnedFiles = onNodeWithTag("page-0").getUnclippedBoundsInRoot()
            assertEquals(viewport.left, returnedFiles.left)
            assertEquals(viewport.right, returnedFiles.right)
        }
}
