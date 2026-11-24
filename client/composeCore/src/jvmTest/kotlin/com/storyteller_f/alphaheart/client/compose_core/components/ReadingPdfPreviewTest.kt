/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.media.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.pdf_zoom_in
import com.storyteller_f.alphaheart.client.compose_core.pdf_zoom_out
import com.storyteller_f.alphaheart.client.core.FixedLoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.shared.model.FileInfo
import org.jetbrains.compose.resources.getString
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class ReadingPdfPreviewTest {
    @Test
    fun renderedPdfUsesBothFilePreviewGuttersAfterResize() =
        runComposeUiTest {
            val fixture = Path.of("../../dev/appiumCore/src/main/resources/media/sample.pdf").toUri().toString()
            val resource = ResourceDownloadRequest(fixture, "sample.pdf")
            val width = mutableStateOf(390.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 650.dp)) {
                        ReadingFilePreview(resource, download = {}) {
                            PdfView(resource.url, Modifier.fillMaxSize(), showTools = true)
                        }
                    }
                }
            }
            waitUntil(timeoutMillis = 10_000) {
                onAllNodesWithContentDescription("Page 0").fetchSemanticsNodes().isNotEmpty()
            }
            val zoomIn = getString(Res.string.pdf_zoom_in)
            val zoomOut = getString(Res.string.pdf_zoom_out)
            val pageNode = onNodeWithContentDescription("Page 0")
            for (nextWidth in listOf(390.dp, 950.dp)) {
                runOnIdle { width.value = nextWidth }
                val page = pageNode.getUnclippedBoundsInRoot()
                val content = onNodeWithTag("file-preview-content").getUnclippedBoundsInRoot()
                val toolbar = onNodeWithTag("pdf-preview-toolbar").getUnclippedBoundsInRoot()
                assertEquals(toolbar.left, page.left)
                assertEquals(toolbar.right, page.right)
                assertEquals(content.left, page.left)
                assertEquals(content.right, page.right)
                onNodeWithContentDescription(zoomIn).performClick()
                assertTrue(pageNode.getUnclippedBoundsInRoot().right > page.right)
                onNodeWithContentDescription(zoomOut).performClick()
                assertEquals(page.right, pageNode.getUnclippedBoundsInRoot().right)
            }
        }

    @Test
    fun cachedRefreshLineSpansTheAreaImmediatelyBelowTheToolbar() =
        runComposeUiTest {
            val width = mutableStateOf(390.dp)
            val file = FileInfo.EMPTY.copy(name = "sample.pdf")
            val handler = FixedLoadingHandler<FileInfo>().apply { done(file) }
            setContent {
                MaterialTheme {
                    Column(Modifier.requiredSize(width.value, 650.dp)) {
                        Box(Modifier.fillMaxWidth().height(56.dp).testTag("toolbar"))
                        ReadingFilePreviewBody(handler, Modifier.weight(1f)) {}
                    }
                }
            }
            val progress = onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
            progress.assertDoesNotExist()
            runOnIdle { handler.state.value = LoadingState.Loading }
            for (nextWidth in listOf(390.dp, 950.dp)) {
                runOnIdle { width.value = nextWidth }
                val toolbar = onNodeWithTag("toolbar").getUnclippedBoundsInRoot()
                val line = progress.getUnclippedBoundsInRoot()
                assertEquals(toolbar.bottom + 1.dp, (line.top + line.bottom) / 2)
                assertEquals(toolbar.left, line.left)
                assertEquals(toolbar.right, line.right)
                onNodeWithTag("file-preview-content").assertExists()
            }
        }
}
