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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class IdentitySettingsUiTest {
    @Test
    fun previewIsStaticAndEditAndResetAreSeparateAtBothWidths() =
        runAppComposeUiTest {
            val width = mutableStateOf(390.dp)
            val actions = mutableListOf<Boolean>()
            setContent {
                MaterialTheme {
                    Box(
                        Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                            .requiredSize(width.value, 720.dp),
                    ) {
                        SettingOptionResettableView("Avatar", true, actions::add) {
                            Text("U1", Modifier.testTag("avatar-preview"))
                        }
                    }
                }
            }
            for (size in listOf(390.dp, 1000.dp)) {
                runOnIdle { width.value = size }
                onNodeWithTag("avatar-preview").performTouchInput { click() }
                runOnIdle { assertEquals(emptyList(), actions) }
                onNodeWithContentDescription("Reset Avatar").performClick()
                runOnIdle { assertEquals(listOf(true), actions) }
                onNodeWithContentDescription("Edit Avatar").performClick()
                runOnIdle {
                    assertEquals(listOf(true, false), actions)
                    actions.clear()
                }
            }
        }

    @Test
    fun editRemainsAvailableWhenThereIsNoImageToReset() =
        runAppComposeUiTest {
            var edits = 0
            setContent {
                MaterialTheme {
                    SettingOptionResettableView("Avatar", false, { reset ->
                        assertEquals(false, reset)
                        edits++
                    }) { Text("U1") }
                }
            }
            onNodeWithContentDescription("Edit Avatar").performClick()
            runOnIdle { assertEquals(1, edits) }
        }
}
