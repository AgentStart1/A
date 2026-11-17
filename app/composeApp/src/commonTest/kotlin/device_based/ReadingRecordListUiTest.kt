/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.ReadingRecordList
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ReadingRecordListUiTest {
    @Test
    fun personalRecordsRemainOneReadingColumnWhenViewportChanges() =
        runAppComposeUiTest {
            val width = mutableStateOf(1280.dp)
            val records = listOf("file", "title", "user", "topic", "room", "community")
            setContent {
                MaterialTheme {
                    Box(
                        Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(width.value, 720.dp),
                    ) {
                        ReadingRecordList {
                            items(records, key = { it }) { name ->
                                Text(name, Modifier.fillMaxWidth().height(70.dp).testTag(name))
                            }
                        }
                    }
                }
            }
            for (viewport in listOf(1280.dp, 390.dp)) {
                runOnIdle { width.value = viewport }
                val pane = onNodeWithTag("personal-record-list").getUnclippedBoundsInRoot()
                val expectedWidth = if (viewport > 740.dp) 740.dp else viewport
                assertEquals(expectedWidth, pane.right - pane.left)
                assertEquals((viewport - expectedWidth) / 2, pane.left)
                records.forEachIndexed { index, name ->
                    val row = onNodeWithTag(name).getUnclippedBoundsInRoot()
                    assertEquals(pane.left + 20.dp, row.left)
                    assertEquals(pane.right - 20.dp, row.right)
                    assertEquals(pane.top + 20.dp + (90 * index).dp, row.top)
                }
            }
        }
}
