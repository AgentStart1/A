/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.alphaheart.client.compose_core.utils.getAppDynamicColorScheme
import com.storyteller_f.alphaheart.client.compose_core.utils.readingColorScheme
import com.storyteller_f.alphaheart.client.compose_core.utils.readingShapes
import com.storyteller_f.alphaheart.client.compose_core.utils.readingTypography
import com.storyteller_f.alphaheart.client.compose_core.utils.rememberPlatformTypography

private val defaultTypography = readingTypography

@Composable
fun PanelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = getAppDynamicColorScheme(dynamicColor, darkTheme) ?: readingColorScheme(darkTheme)
    val resolvedTypography = rememberPlatformTypography(defaultTypography)
    MaterialTheme(colorScheme = colors, shapes = readingShapes, typography = resolvedTypography) {
        ReadingInteractions(content)
    }
}
