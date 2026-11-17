/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.attafitamim.krop.core.crop.AspectRatio
import com.attafitamim.krop.core.crop.cropState
import com.attafitamim.krop.core.crop.cropperStyle
import com.attafitamim.krop.core.crop.getTransformedImageRect
import com.attafitamim.krop.core.images.ImageBitmapSrc
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class ReadingCropSurfaceUiTest {
    private val previousLocale = Locale.getDefault()

    @BeforeTest
    fun useEnglish() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `crop shell fits narrow and wide without runtime context`() =
        runComposeUiTest {
            val width = mutableStateOf(354.dp)
            setContent {
                MaterialTheme {
                    ReadingCropSurface(Modifier.width(width.value), cancel = {}, confirm = {}, tools = {}) {
                        Box(Modifier.size(220.dp))
                    }
                }
            }
            for (value in listOf(354.dp, 660.dp)) {
                runOnIdle { width.value = value }
                val dialog = onNodeWithTag("reading-crop-dialog").getUnclippedBoundsInRoot()
                val stage = onNodeWithTag("reading-crop-stage").getUnclippedBoundsInRoot()
                assertEquals(value, dialog.right - dialog.left)
                assertTrue(stage.left >= dialog.left && stage.right <= dialog.right)
                assertTrue(stage.bottom - stage.top >= 300.dp)
            }
        }

    @Test
    fun `loading preview content does not move actions and callbacks remain independent`() =
        runComposeUiTest {
            val loaded = mutableStateOf(false)
            var cancelled = 0
            var accepted = 0
            var reset = 0
            setContent {
                MaterialTheme {
                    ReadingCropSurface(
                        Modifier.width(354.dp),
                        cancel = { cancelled++ },
                        confirm = { accepted++ },
                        tools = { ReadingIconButton({ reset++ }) { Text("Reset") } },
                    ) {
                        Box(Modifier.size(220.dp)) { if (loaded.value) Text("Preview ready") }
                    }
                }
            }
            val initialStage = onNodeWithTag("reading-crop-stage").getUnclippedBoundsInRoot()
            val initialConfirm = onNodeWithText("Confirm crop").getUnclippedBoundsInRoot()
            runOnIdle { loaded.value = true }
            assertEquals(initialStage, onNodeWithTag("reading-crop-stage").getUnclippedBoundsInRoot())
            assertEquals(initialConfirm, onNodeWithText("Confirm crop").getUnclippedBoundsInRoot())
            onNodeWithText("Reset").performClick()
            onNodeWithText("Confirm crop").performClick()
            onNodeWithText("Cancel").performClick()
            runOnIdle {
                assertEquals(1, reset)
                assertEquals(1, accepted)
                assertEquals(1, cancelled)
            }
        }

    @Test
    fun `compact tools preserve the real crop controller transforms and reset`() =
        runComposeUiTest {
            val state = cropState(ImageBitmapSrc(ImageBitmap(160, 120)))
            state.setInitialState(cropperStyle(aspects = listOf(AspectRatio(1, 1))))
            val initial = state.region
            setContent { MaterialTheme { Row { ReadingCropTools(state) } } }
            onNodeWithContentDescription("Rotate right").performClick()
            runOnIdle { assertEquals(90, state.transform.angleDeg) }
            onNodeWithContentDescription("Rotate left").performClick()
            runOnIdle { assertEquals(0, state.transform.angleDeg) }
            onNodeWithContentDescription("Flip horizontally").performClick()
            runOnIdle { assertEquals(-1f, state.transform.scale.x) }
            onNodeWithContentDescription("Flip vertically").performClick()
            runOnIdle { assertEquals(-1f, state.transform.scale.y) }
            onNodeWithContentDescription("Reset crop").performClick()
            runOnIdle {
                assertEquals(0, state.transform.angleDeg)
                assertEquals(1f, state.transform.scale.x)
                assertEquals(1f, state.transform.scale.y)
                assertEquals(initial, state.region)
            }
        }

    @Test
    fun `rotated room and poster crops retain their required output ratios`() =
        runComposeUiTest {
            val state = cropState(ImageBitmapSrc(ImageBitmap(420, 595)))
            setContent { MaterialTheme { Row { ReadingCropTools(state) } } }
            for (aspect in listOf(AspectRatio(4, 3), AspectRatio(3, 4))) {
                runOnIdle {
                    state.reset()
                    state.setInitialState(cropperStyle(aspects = listOf(aspect)))
                }
                repeat(4) {
                    onNodeWithContentDescription("Rotate right").performClick()
                    runOnIdle {
                        assertEquals(aspect.x.toFloat() / aspect.y, state.region.width / state.region.height, .001f)
                        val bounds = getTransformedImageRect(state.transform, state.src.size)
                        assertTrue(state.region.left >= bounds.left && state.region.right <= bounds.right)
                        assertTrue(state.region.top >= bounds.top && state.region.bottom <= bounds.bottom)
                        assertTrue(state.aspectLock)
                    }
                }
                onNodeWithContentDescription("Reset crop").performClick()
                onNodeWithContentDescription("Rotate left").performClick()
                runOnIdle {
                    assertEquals(aspect.x.toFloat() / aspect.y, state.region.width / state.region.height, .001f)
                }
            }
        }
}
