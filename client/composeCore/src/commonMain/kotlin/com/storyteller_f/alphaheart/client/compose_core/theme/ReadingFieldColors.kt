/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.theme

import androidx.compose.material3.TextFieldColors

internal data class FieldColors(
    val text: androidx.compose.ui.graphics.Color,
    val background: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
)

internal fun fieldColors(colors: TextFieldColors, enabled: Boolean, error: Boolean, focused: Boolean): FieldColors =
    when {
        !enabled -> FieldColors(colors.disabledTextColor, colors.disabledContainerColor, colors.disabledIndicatorColor)
        error -> FieldColors(colors.errorTextColor, colors.errorContainerColor, colors.errorIndicatorColor)
        focused -> FieldColors(colors.focusedTextColor, colors.focusedContainerColor, colors.focusedIndicatorColor)
        else -> FieldColors(colors.unfocusedTextColor, colors.unfocusedContainerColor, colors.unfocusedIndicatorColor)
    }
