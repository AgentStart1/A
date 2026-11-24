/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.icons

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface IconRes {
    data class Vector(val vector: ImageVector, val description: String = "") : IconRes
    data class Font(val char: Char, val description: String = "") : IconRes
    data object Loading : IconRes
}

fun Modifier.clickableIfNotNull(onClick: (() -> Unit)?): Modifier {
    if (onClick != null) {
        return clickable {
            onClick.invoke()
        }
    }
    return this
}
