/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class BrowserReactionLabelsTest {
    @Test
    fun bridgeLabelsIncludeEmojiAndUpdateOnTheSameNodeWithoutMovingIt() =
        runComposeUiTest {
            val originalUrl = window.location.href
            val url = originalUrl.substringBefore('#').substringBefore('?') + "?appium=true"
            window.history.replaceState(null, "", url)
            val count = mutableStateOf(1)
            var smileClicks = 0
            var heartClicks = 0
            try {
                setContent {
                    MaterialTheme {
                        Row {
                            Pill(text = "${count.value} ", emoji = "😀") { smileClicks++ }
                            Pill(text = "1 ", emoji = "❤️", selected = true) { heartClicks++ }
                        }
                    }
                }
                awaitIdle()
                val smile = assertNotNull(reactionButton("😀 1"))
                val heart = assertNotNull(reactionButton("❤️ 1"))
                val before = smile.getBoundingClientRect()
                smile.click()
                heart.click()
                awaitIdle()
                assertEquals(1, smileClicks)
                assertEquals(1, heartClicks)
                runOnIdle { count.value = 2 }
                awaitIdle()
                assertEquals(null, reactionButton("😀 1"))
                val updated = assertNotNull(reactionButton("😀 2"))
                assertTrue(smile === updated)
                assertEquals(before.width, updated.getBoundingClientRect().width)
                assertEquals(before.height, updated.getBoundingClientRect().height)
                onNodeWithContentDescription("❤️ 1").assertIsSelected()
                updated.click()
                awaitIdle()
                assertEquals(2, smileClicks)
                assertEquals(1, heartClicks)
            } finally {
                window.history.replaceState(null, "", originalUrl)
            }
        }

    @Test
    fun measurementCandidatesPublishNeitherNativeActionsNorBrowserButtons() =
        runComposeUiTest {
            val originalUrl = window.location.href
            window.history.replaceState(
                null,
                "",
                originalUrl.substringBefore('#').substringBefore('?') + "?appium=true",
            )
            var clicks = 0
            try {
                setContent {
                    MaterialTheme {
                        Row {
                            PillMeasurementScope {
                                Pill(text = "+1") { error("candidate must never be interactive") }
                                Pill(text = "+2") { error("candidate must never be interactive") }
                                Pill(text = "1", emoji = "🤔") { error("hidden emoji must not be interactive") }
                            }
                            Pill(text = "+3") { clicks++ }
                        }
                    }
                }
                awaitIdle()
                onNodeWithContentDescription("+1").assertDoesNotExist()
                onNodeWithContentDescription("+2").assertDoesNotExist()
                onNodeWithContentDescription("🤔 1").assertDoesNotExist()
                assertEquals(null, reactionButton("+1"))
                assertEquals(null, reactionButton("+2"))
                assertEquals(null, reactionButton("🤔 1"))
                assertNotNull(reactionButton("+3")).click()
                awaitIdle()
                assertEquals(1, clicks)
            } finally {
                window.history.replaceState(null, "", originalUrl)
            }
        }

    private fun reactionButton(label: String): HTMLElement? =
        document.querySelector("button[data-appium-semantics][aria-label='$label']") as? HTMLElement
}
