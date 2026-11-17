/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.file.QuotaDialogContent
import com.storyteller_f.alphaheart.app.pages.file.QuotaDialogState
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.shared.model.QuotaDetail
import com.storyteller_f.shared.model.QuotaInfo
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.model.UploadRecordInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UploadRecordStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class QuotaDialogContentUiTest {
    @Test
    fun quotaContentFitsBothWidthsAndHandlesZeroTotal() =
        runComposeUiTest {
            val width = mutableStateOf(350.dp)
            val quota = QuotaDetail(QuotaInfo(1L, ObjectType.COMMUNITY, QuotaType.FILE, 0L, 0L, null))
            setContent {
                MaterialTheme {
                    Box(Modifier.width(width.value)) {
                        ReadingActionMenuSurface({}, { Text("Quota") }) { QuotaDialogContent(quota) }
                    }
                }
            }
            for (value in listOf(350.dp, 700.dp)) {
                runOnIdle { width.value = value }
                val bounds = onNodeWithTag("action-menu-surface").getUnclippedBoundsInRoot()
                assertEquals(320.dp, bounds.right - bounds.left)
                onNodeWithText("0%").assertIsDisplayed()
            }
        }

    @Test
    fun quotaLoadingErrorRetryThenDataFitsScrollableMenu() =
        runComposeUiTest {
            val width = 350.dp
            val handler = QuotaLoadingHandler()
            setContent {
                MaterialTheme {
                    Box(Modifier.width(width)) {
                        ReadingActionMenuSurface({}, { Text("Quota") }) {
                            QuotaDialogState(handler)
                        }
                    }
                }
            }

            waitForIdle()
            onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists().assertIsDisplayed()
            assertCloseButtonInsideSurface()

            runOnIdle {
                handler.state.value = LoadingState.Error(IllegalStateException("quota request failed"))
            }
            waitForIdle()
            onNodeWithText("Could not load content").assertIsDisplayed()
            assertCloseButtonInsideSurface()

            onNodeWithText("Retry").performClick()
            runOnIdle { assertEquals(1, handler.refreshCount) }
            onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists().assertIsDisplayed()

            runOnIdle {
                handler.data.value = loadedQuota()
                handler.state.value = LoadingState.Done
            }
            waitForIdle()

            onNodeWithText("UI-review-multichunk120.bin", substring = true).assertIsDisplayed()
            assertCloseButtonInsideSurface()
        }

    private fun androidx.compose.ui.test.ComposeUiTest.assertCloseButtonInsideSurface() {
        val surface = onNodeWithTag("action-menu-surface").getUnclippedBoundsInRoot()
        val close = onNodeWithTag("action-menu-close").getUnclippedBoundsInRoot()
        assertTrue(close.right > close.left && close.bottom > close.top)
        assertTrue(close.left >= surface.left && close.top >= surface.top)
        assertTrue(close.right <= surface.right && close.bottom <= surface.bottom)
    }

    private fun loadedQuota() =
        QuotaDetail(
            QuotaInfo(1L, ObjectType.COMMUNITY, QuotaType.FILE, 1024L * 1024 * 1024, 0L, 120L),
            UploadRecordInfo(
                id = 120L,
                createdTime = LocalDateTime(2026, 11, 17, 0, 0),
                objectId = 1L,
                objectType = ObjectType.COMMUNITY,
                status = UploadRecordStatus.PENDING,
                total = 64L * 1024 * 1024,
                progress = 10L * 1024 * 1024,
                name = "UI-review-multichunk120.bin",
                chunkSize = 10L * 1024 * 1024,
                sha256 = "281e519df3077b557c6b03f5da83c4e8d397219259615dd7c3308f89cae8f2a6",
                crc32c = "hkV4iw==",
            ),
        )
}

private class QuotaLoadingHandler : LoadingHandler<QuotaDetail> {
    override val state = MutableStateFlow<LoadingState?>(LoadingState.Loading)
    override val data = MutableStateFlow<QuotaDetail?>(null)
    var refreshCount = 0
        private set

    override fun refresh() {
        refreshCount++
        state.value = LoadingState.Loading
    }
}
