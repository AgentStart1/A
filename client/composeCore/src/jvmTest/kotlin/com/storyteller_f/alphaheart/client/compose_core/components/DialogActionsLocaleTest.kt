/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomAlertDialogState
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class DialogActionsLocaleTest {
    @Test
    fun `failure confirmation uses current locale without changing diagnostic text`() {
        val original = Locale.getDefault()
        try {
            listOf(Locale.ENGLISH to "OK", Locale.SIMPLIFIED_CHINESE to "确定").forEach { (locale, label) ->
                Locale.setDefault(locale)
                runComposeUiTest {
                    var dismissed = 0
                    var confirmed = 0
                    setContent {
                        CustomAlertDialogInternal(
                            CustomAlertDialogState("Save failed", "503 test diagnostic"),
                            { dismissed++ },
                            { confirmed++ },
                        )
                    }
                    onNodeWithText("503 test diagnostic").assertIsDisplayed()
                    onNodeWithText("Yes").assertDoesNotExist()
                    onNodeWithText(label).assertIsDisplayed().performClick()
                    assertEquals(1, dismissed)
                    assertEquals(1, confirmed)
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
