/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.rtc_call_title
import com.storyteller_f.alphaheart.app.rtc_join_audio
import com.storyteller_f.alphaheart.app.rtc_join_video
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class RtcReadingLayoutTest {
    @Test
    fun narrowHeaderAndActions() = checkReadingLayout(390.dp)

    @Test
    fun wideHeaderAndActions() = checkReadingLayout(850.dp)

    private fun checkReadingLayout(width: Dp) =
        runComposeUiTest {
            val titleLabel = getString(Res.string.rtc_call_title)
            val joinAudio = getString(Res.string.rtc_join_audio)
            val joinVideo = getString(Res.string.rtc_join_video)
            val roomName = "A room with a deliberately long name for the narrow call toolbar"
            var videoSelected: Boolean? = null
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    MaterialTheme {
                        Column(Modifier.requiredSize(width, 600.dp)) {
                            RtcToolbar(roomName) {}
                            RtcReadyContent(failed = false, join = { videoSelected = it })
                        }
                    }
                }
            }
            val title = onNodeWithText(titleLabel).assertIsDisplayed().getUnclippedBoundsInRoot()
            val subtitle = onNodeWithText(roomName).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(subtitle.top >= title.bottom)
            assertTrue(subtitle.bottom - title.top < 60.dp)
            onNodeWithText(joinAudio).assertDoesNotExist()
            onNodeWithText(joinVideo).assertDoesNotExist()
            onNodeWithContentDescription(joinAudio).performClick()
            assertEquals(false, videoSelected)
            onNodeWithContentDescription(joinVideo).performClick()
            assertEquals(true, videoSelected)
        }
}
