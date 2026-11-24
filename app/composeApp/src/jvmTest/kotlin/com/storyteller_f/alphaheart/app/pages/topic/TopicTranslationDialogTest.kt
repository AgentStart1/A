/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.IntSize
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class TopicTranslationDialogTest {
    @Test
    fun translationReplacesMenuInOneDialogAndPlatformUnavailabilityIsLocalized() {
        val originalLocale = Locale.getDefault()
        try {
            for ((locale, expected) in listOf(
                Locale.ENGLISH to "Translation is not supported on this platform",
                Locale.SIMPLIFIED_CHINESE to "当前平台暂不支持翻译",
            )) {
                Locale.setDefault(locale)
                for (width in listOf(390, 1280)) {
                    assertTranslationReplacesMenu(width, expected)
                }
            }
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    private fun assertTranslationReplacesMenu(width: Int, expected: String) {
        runComposeUiTest {
            val shown = mutableStateOf(true)
            val translating = mutableStateOf(false)
            var starts = 0
            setContent {
                val window = LocalWindowInfo.current
                val testWindow =
                    object : WindowInfo by window {
                        override val containerSize = IntSize(width, 800)
                    }
                CompositionLocalProvider(LocalWindowInfo provides testWindow) {
                    MaterialTheme {
                        if (shown.value) {
                            TopicActionDialog(
                                translating.value,
                                { shown.value = false },
                                menu = {
                                    DialogContainer {
                                        Text("Topic menu")
                                        ReadingButton({ translating.value = true }) { Text("Translate now") }
                                    }
                                },
                                translation = {
                                    DisposableEffect(Unit) {
                                        starts++
                                        onDispose { }
                                    }
                                    DialogContainer(onDismissRequest = { shown.value = false }) {
                                        TranslationPlatformUnavailable()
                                    }
                                },
                            )
                        }
                    }
                }
            }
            val overlay = if (width == 390) isDialog() else isPopup()
            onAllNodes(overlay).assertCountEquals(1)
            onNodeWithText("Translate now").performClick()
            onAllNodes(overlay).assertCountEquals(1)
            onNodeWithText("Topic menu").assertDoesNotExist()
            onNodeWithText("Translate now").assertDoesNotExist()
            onNodeWithText(expected).assertIsDisplayed()
            onNodeWithText("unsupported").assertDoesNotExist()
            runOnIdle {
                assertEquals(1, starts)
                shown.value = false
            }
            onAllNodes(overlay).assertCountEquals(0)
        }
    }
}
