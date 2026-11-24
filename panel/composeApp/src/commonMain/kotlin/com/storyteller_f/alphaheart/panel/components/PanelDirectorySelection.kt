/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun Modifier.panelDirectorySelection(selected: Boolean): Modifier =
    clip(RoundedCornerShape(8.dp))
        .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
        .semantics { this.selected = selected }

@Composable
internal fun panelDirectoryRowColors(selected: Boolean): ListItemColors =
    ListItemDefaults.colors(
        containerColor =
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    )
