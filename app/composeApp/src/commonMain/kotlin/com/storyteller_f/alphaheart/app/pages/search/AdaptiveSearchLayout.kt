/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Search results use the remaining viewport independently of the bounded input field. */
@Composable
fun DesktopSearchLayout(
    title: String?,
    expanded: Boolean,
    input: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    results: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val resultHeight = (maxHeight - 100.dp).coerceAtLeast(0.dp)
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (title != null) {
                    Text(
                        title,
                        Modifier.weight(1f).padding(end = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Box(Modifier.weight(3f), contentAlignment = Alignment.CenterEnd) {
                    Surface(
                        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) { input() }
                }
                actions()
            }
            if (expanded) {
                Surface(
                    Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(max = resultHeight),
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                ) {
                    Column(Modifier.fillMaxSize().padding(8.dp)) { results() }
                }
            }
        }
    }
}
