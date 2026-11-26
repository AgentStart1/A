/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.icons.IconRes
import com.storyteller_f.alphaheart.client.compose_core.icons.clickableIfNotNull
import com.storyteller_f.alphaheart.client.compose_core.icons.readingIcon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@Composable
fun CustomIcon(icon: IconRes, onClick: (() -> Unit)? = null) {
    CustomIcon(icon, 20.dp, onClick)
}

@Composable
fun CustomIcon(icon: IconRes, iconSize: Dp, onClick: (() -> Unit)? = null) {
    when (icon) {
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
