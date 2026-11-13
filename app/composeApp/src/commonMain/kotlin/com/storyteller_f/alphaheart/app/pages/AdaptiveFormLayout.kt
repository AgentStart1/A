/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp

/** Measures fields against their own pane and retains composition when columns change. */
@Composable
fun AdaptiveFormLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Layout(
            content = content,
            modifier =
            Modifier.widthIn(max = 1200.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(20.dp),
        ) { measurables, constraints ->
            val columns = if (constraints.maxWidth >= 800.dp.roundToPx()) 2 else 1
            val gap = 24.dp.roundToPx()
            val fieldWidth = ((constraints.maxWidth - gap * (columns - 1)) / columns).coerceAtLeast(0)
            val fields = measurables.map { it.measure(Constraints.fixedWidth(fieldWidth)) }
            val rows = fields.chunked(columns)
            val height = rows.sumOf { row -> row.maxOf { it.height } } + gap * (rows.size - 1).coerceAtLeast(0)
            layout(constraints.maxWidth, constraints.constrainHeight(height)) {
                var y = 0
                rows.forEach { row ->
                    row.forEachIndexed { index, field -> field.placeRelative(index * (fieldWidth + gap), y) }
                    y += row.maxOf { it.height } + gap
                }
            }
        }
    }
}
