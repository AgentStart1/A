/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.alphaheart.client.compose_core.utils.getAppDynamicColorScheme
import com.storyteller_f.alphaheart.client.compose_core.utils.readingColorScheme
import com.storyteller_f.alphaheart.client.compose_core.utils.readingShapes

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    typography: Typography? = null,
    content: @Composable () -> Unit,
) {
    val colors = getAppDynamicColorScheme(dynamicColor, darkTheme) ?: readingColorScheme(darkTheme)
    val resolvedTypography = typography ?: rememberAppTypography()
    MaterialTheme(colorScheme = colors, shapes = readingShapes, typography = resolvedTypography) {
        ReadingInteractions(content)
    }
}
