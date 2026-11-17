/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.shared.model.TotpSetupInfo
import com.storyteller_f.shared.model.TwoFactorSettingsInfo
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class TwoFactorDialogGeometryTest {
    @Test
    fun canonicalCompactStatusHasNoFixedBottomGapAndLoadingFailureKeepItsGeometry() {
        withEnglishLocale {
            for (compact in listOf(false, true)) {
                runComposeUiTest {
                    var settings by mutableStateOf<TwoFactorSettingsInfo?>(TwoFactorSettingsInfo(false))
                    var failed by mutableStateOf(false)
                    var retries = 0
                    setContent {
                        MaterialTheme {
                            ReadingInteractions {
                                Box(Modifier.width(if (compact) 390.dp else 800.dp)) {
                                    TwoFactorDialogSurface(
                                        compact, settings, null, "", {}, emptyActions(), failed, { retries++ }, {},
                                    )
                                }
                            }
                        }
                    }
                    val padding = if (compact) 18 else 20
                    val surface = onNodeWithTag("two-factor-surface")
                    surface.assertWidthIsEqualTo(320.dp).assertHeightIsEqualTo((99 + padding * 2).dp)
                    onNodeWithTag("two-factor-status").assertHeightIsEqualTo(44.dp)
                    onNodeWithTag("two-factor-checkbox", useUnmergedTree = true)
                        .assertWidthIsEqualTo(20.dp).assertHeightIsEqualTo(20.dp)
                    onNodeWithTag("two-factor-toggle").assertIsOff()
                        .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
                    val initial = surface.getUnclippedBoundsInRoot()
                    val status = onNodeWithTag("two-factor-status").getUnclippedBoundsInRoot()
                    assertEquals((padding + 1).dp, initial.bottom - status.bottom)
                    assertEquals((padding + 54).dp, status.top - initial.top)
                    runOnIdle { settings = null }
                    surface.assertHeightIsEqualTo(initial.bottom - initial.top)
                    onNodeWithTag("two-factor-status").assertHeightIsEqualTo(44.dp)
                    onNodeWithTag("two-factor-toggle").assertDoesNotExist()
                    runOnIdle { failed = true }
                    surface.assertHeightIsEqualTo(initial.bottom - initial.top)
                    onNodeWithText("Loading failed · Retry").assertIsDisplayed().performClick()
                    runOnIdle { assertEquals(1, retries) }
                    runOnIdle { settings = TwoFactorSettingsInfo(false) }
                    surface.assertHeightIsEqualTo(initial.bottom - initial.top)
                    onNodeWithTag("two-factor-toggle").assertIsOff()
                }
            }
        }
    }

    @Test
    fun setupCodeEnableRecoveryAndDisableKeepTheirOriginalActionsAtBothWidths() {
        withEnglishLocale {
            for (compact in listOf(false, true)) {
                runComposeUiTest {
                    var settings by mutableStateOf(TwoFactorSettingsInfo(false))
                    var setup by mutableStateOf<TotpSetupInfo?>(null)
                    var code by mutableStateOf("")
                    val actions = mutableListOf<String>()
                    val fixture = TotpSetupInfo("SYNTHETIC", "otpauth://totp/Test?secret=SYNTHETIC", emptyList())
                    setContent {
                        MaterialTheme {
                            ReadingInteractions {
                                Box(Modifier.width(if (compact) 390.dp else 800.dp)) {
                                    TwoFactorDialogSurface(
                                        compact, settings, setup, code, { code = it },
                                        TwoFactorActions(
                                            setupTotp = {
                                                actions += "setup"
                                                setup = fixture
                                            },
                                            disableTwoFactor = {
                                                actions += "disable"
                                                settings = TwoFactorSettingsInfo(false)
                                            },
                                            copyUri = { actions += it },
                                            enableTotp = {
                                                actions += "enable:$code"
                                                settings = TwoFactorSettingsInfo(true)
                                                setup = null
                                            },
                                            downloadRecoveryCodes = { actions += "recovery" },
                                        ),
                                        false, {}, {},
                                    )
                                }
                            }
                        }
                    }
                    onNodeWithTag("two-factor-toggle").performClick()
                    onNodeWithText("Copy URI").performScrollTo().performClick()
                    onNode(hasSetTextAction()).performScrollTo().performTextInput("123456")
                    onNodeWithText("Enable", substring = false).performScrollTo().performClick()
                    onNodeWithTag("two-factor-toggle").assertIsOn()
                    onNodeWithText("Download recovery codes").performScrollTo().performClick()
                    onNodeWithTag("two-factor-toggle").performScrollTo().performClick()
                    onNodeWithTag("two-factor-toggle").assertIsOff()
                    runOnIdle {
                        assertEquals(
                            listOf("setup", fixture.otpauthUri, "enable:123456", "recovery", "disable"),
                            actions,
                        )
                        assertEquals("123456", code)
                    }
                }
            }
        }
    }
}

private fun emptyActions() = TwoFactorActions({}, {}, {}, {}, {})

private inline fun withEnglishLocale(block: () -> Unit) {
    val previous = Locale.getDefault()
    try {
        Locale.setDefault(Locale.ENGLISH)
        block()
    } finally {
        Locale.setDefault(previous)
    }
}
