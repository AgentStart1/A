/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily

/** Content-only font choices; surrounding navigation and action controls retain the app theme. */
data class TopicContentFonts(
    val content: FontFamily? = null,
    val code: FontFamily? = null,
    val editor: FontFamily? = null,
)

val LocalTopicContentFonts = staticCompositionLocalOf { TopicContentFonts() }
