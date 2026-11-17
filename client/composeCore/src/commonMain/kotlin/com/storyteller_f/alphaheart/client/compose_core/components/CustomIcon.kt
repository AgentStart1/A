/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.tclement.fonticons.FontIcon
import dev.tclement.fonticons.LocalIconFont
import dev.tclement.fonticons.LocalIconSize
import dev.tclement.fonticons.LocalIconTintProvider
import dev.tclement.fonticons.LocalIconWeight
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

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

@Composable
fun CustomIcon(icon: IconRes, onClick: (() -> Unit)? = null) {
    CustomIcon(icon, 20.dp, onClick)
}

@Composable
fun CustomIcon(icon: IconRes, iconSize: Dp, onClick: (() -> Unit)? = null) {
    when (icon) {
        is IconRes.Font -> {
            CompositionLocalProvider(
                LocalIconFont provides LocalIconFont.current,
                LocalIconSize provides iconSize,
                LocalIconTintProvider provides LocalContentColor,
                LocalIconWeight provides LocalIconWeight.current,
                content = {
                    FontIcon(
                        icon.char,
                        icon.description,
                        modifier = Modifier.clickableIfNotNull(onClick).size(iconSize),
                    )
                },
            )
        }

        is IconRes.Vector -> {
            Icon(
                imageVector = readingIcon(icon.vector),
                contentDescription = icon.description,
                modifier = Modifier.clickableIfNotNull(onClick).size(iconSize),
            )
        }

        IconRes.Loading -> CircularProgressIndicator(modifier = Modifier.size(iconSize))
    }
}

@Composable
fun CustomIcon(vector: ImageVector, onClick: (() -> Unit)? = null) {
    CustomIcon(IconRes.Vector(vector), onClick)
}
