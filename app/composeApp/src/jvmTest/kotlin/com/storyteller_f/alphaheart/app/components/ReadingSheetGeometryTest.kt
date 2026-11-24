/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.file.DownloadRecordItemPreviewProvider
import com.storyteller_f.alphaheart.app.pages.file.DownloadInfoTable
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBottomOverlay
import device_based.runAppComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class ReadingSheetGeometryTest {
    @Test
    fun narrowSheetUsesTheBottomEdgeAndLeavesThePageVisible() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    ReadingBottomOverlay({}, Modifier.requiredSize(390.dp, 600.dp).testTag("viewport")) {
                        Box(Modifier.fillMaxWidth().height(150.dp).testTag("transfer"))
                    }
                }
            }
            val viewport = onNodeWithTag("viewport").getUnclippedBoundsInRoot()
            val content = onNodeWithTag("transfer").getUnclippedBoundsInRoot()
            assertEquals(viewport.left, content.left)
            assertEquals(viewport.right, content.right)
            assertEquals(viewport.bottom, content.bottom)
            assertTrue(content.top > viewport.top)
        }

    @Test
    fun downloadDetailsCanRenderAnApplicationTransferAtBothWidths() =
        runAppComposeUiTest {
            val windowWidth = mutableStateOf(390)
            val record = DownloadRecordItemPreviewProvider().values.filterNotNull().first()
            setContent {
                val window =
                    object : WindowInfo {
                        override val isWindowFocused = true
                        override val containerSize = IntSize(windowWidth.value, 800)
                    }
                CompositionLocalProvider(LocalWindowInfo provides window) {
                    MaterialTheme {
                        ReadingSheet("Download details", {}) {
                            DownloadInfoTable(record)
                        }
                    }
                }
            }
            for (width in listOf(390, 1280)) {
                runOnIdle { windowWidth.value = width }
                onNodeWithText("Download details").assertIsDisplayed()
                onNodeWithText(record.path).assertIsDisplayed()
            }
        }
}
