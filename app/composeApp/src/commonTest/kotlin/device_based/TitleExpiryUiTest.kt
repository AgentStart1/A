/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import com.storyteller_f.alphaheart.app.pages.title.ExpireTimePickerDialog
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class TitleExpiryUiTest {
    @Test
    fun existingSecondsAreRetainedByDateAndTimeFields() =
        runAppComposeUiTest {
            var confirmed: String? = null
            setContent {
                MaterialTheme {
                    ExpireTimePickerDialog(true, "2026-11-17T12:30:45", {}, { confirmed = it }, {})
                }
            }
            onNodeWithText("2026-11-17").assertIsDisplayed()
            onNodeWithText("12:30:45").assertIsDisplayed()
            onNodeWithText("Confirm").performClick()
            runOnIdle { assertEquals("2026-11-17T12:30:45", confirmed) }
        }

    @Test
    fun permanentSelectionAndInvalidDateKeepTheExpectedConfirmationBehavior() =
        runAppComposeUiTest {
            var cleared = 0
            var confirmed: String? = null
            setContent {
                MaterialTheme {
                    ExpireTimePickerDialog(true, "", {}, { confirmed = it }, { cleared++ })
                }
            }
            onNode(isToggleable()).assertIsOn()
            onNodeWithText("Confirm").performClick()
            runOnIdle { assertEquals(1, cleared) }
            onNode(hasSetTextAction() and hasText("Date (UTC+00:00)")).performTextReplacement("2026-02-30")
            onNode(hasSetTextAction() and hasText("Time (UTC+00:00)")).performTextReplacement("12:30")
            onNode(isToggleable()).assertIsOff()
            onNodeWithText("Confirm").performClick()
            onNodeWithText("Invalid date/time").assertIsDisplayed()
            runOnIdle { assertEquals(null, confirmed) }
            onNode(hasSetTextAction() and hasText("Date (UTC+00:00)")).performTextReplacement("2026-02-28")
            onNodeWithText("Confirm").performClick()
            runOnIdle { assertEquals("2026-02-28T12:30", confirmed) }
        }
}
