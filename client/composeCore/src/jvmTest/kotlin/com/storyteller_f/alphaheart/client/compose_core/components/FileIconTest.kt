/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.shared.model.FileInfo
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class FileIconTest {
    @Test
    fun `quicktime and mp4 videos have a stable visible file icon`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            runComposeUiTest {
                val file = mutableStateOf(FileInfo.EMPTY.copy(name = "Synthetic", contentType = "video/quicktime"))
                val reference = mutableStateOf(Icons.Default.VideoFile)
                setContent {
                    MaterialTheme {
                        Row {
                            Box(Modifier.background(Color.White).testTag("file-icon")) { FileIcon(file.value) }
                            Box(Modifier.background(Color.White).testTag("reference-icon")) {
                                ReadingIcon(reference.value, null, Modifier.size(40.dp))
                            }
                        }
                    }
                }
                val variants =
                    listOf(
                        "video/quicktime" to Icons.Default.VideoFile,
                        "video/mp4" to Icons.Default.VideoFile,
                        "audio/flac" to Icons.Default.AudioFile,
                        "application/octet-stream" to Icons.Default.AttachFile,
                    )
                variants.forEach { (mime, expectedIcon) ->
                    runOnIdle {
                        file.value = file.value.copy(contentType = mime)
                        reference.value = expectedIcon
                    }
                    val icon = onNodeWithTag("file-icon").assertIsDisplayed()
                    val bounds = icon.getUnclippedBoundsInRoot()
                    assertEquals(40.dp, bounds.right - bounds.left)
                    assertEquals(40.dp, bounds.bottom - bounds.top)
                    val actualImage = icon.captureToImage()
                    val expectedImage = onNodeWithTag("reference-icon").captureToImage()
                    assertEquals(expectedImage.width, actualImage.width)
                    assertEquals(expectedImage.height, actualImage.height)
                    val actualPixels = actualImage.toPixelMap()
                    val expectedPixels = expectedImage.toPixelMap()
                    var ink = 0
                    for (y in 0 until actualImage.height) {
                        for (x in 0 until actualImage.width) {
                            val pixel = actualPixels[x, y].toArgb()
                            assertEquals(expectedPixels[x, y].toArgb(), pixel, "$mime glyph at $x,$y")
                            if (pixel != Color.White.toArgb()) ink++
                        }
                    }
                    assertTrue(ink > 0, "$mime must draw a real file glyph")
                    assertTrue(ink < actualImage.width * actualImage.height, "$mime must retain visible glyph geometry")
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
