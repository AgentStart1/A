/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.storyteller_f.alphaheart.app.common.TwoFactorStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class TwoFactorDialogGeometryTest {
    @Test
    fun verificationRequiresSixDigitsAndShowsAnErrorAfterFailure() =
        runComposeUiTest {
            val submitted = mutableListOf<String>()
            setContent {
                MaterialTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TwoFactorWorkspace(TwoFactorStage.Disabled) {
                            TwoFactorVerify(onBack = {}, onVerify = {
                                submitted += it
                                false
                            })
                        }
                    }
                }
            }
            onNodeWithTag("two-factor-verify").assertIsNotEnabled()
            onNodeWithTag("two-factor-code").performTextInput("12a345")
            onNodeWithTag("two-factor-verify").assertIsNotEnabled()
            onNodeWithTag("two-factor-code").performTextInput("6")
            onNodeWithTag("two-factor-verify").assertIsEnabled().performClick()
            onNodeWithTag("two-factor-code-error").assertExists()
            runOnIdle { assertEquals(listOf("123456"), submitted) }
        }

    @Test
    fun recoveryNeedsACompletedDownloadBeforeTheFinishAction() =
        runComposeUiTest {
            var attempts = 0
            var done = false
            setContent {
                MaterialTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TwoFactorWorkspace(TwoFactorStage.Recovery(null)) {
                            TwoFactorRecovery(onDownload = { ++attempts == 2 }, onDone = { done = true })
                        }
                    }
                }
            }
            onNodeWithTag("two-factor-done").assertDoesNotExist()
            onNodeWithTag("two-factor-download").performClick()
            onNodeWithTag("two-factor-download-error").assertExists()
            onNodeWithTag("two-factor-done").assertDoesNotExist()
            onNodeWithTag("two-factor-download").performClick()
            onNodeWithTag("two-factor-done").performClick()
            runOnIdle {
                assertEquals(2, attempts)
                assertTrue(done)
            }
        }
}
