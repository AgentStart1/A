/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Keeps the editor mounted while adding a separate preview when both panes fit. */
@Composable
fun AdaptiveEditorLayout(
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
    editor: @Composable ColumnScope.(Boolean) -> Unit,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        BoxWithConstraints(Modifier.widthIn(max = 1600.dp).fillMaxSize()) {
            val wide = maxWidth >= 1000.dp
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(1f).fillMaxSize()) { editor(wide) }
                if (wide) {
                    VerticalDivider()
                    Box(Modifier.weight(1f).fillMaxSize().padding(top = 12.dp)) { preview() }
                }
            }
        }
    }
}
