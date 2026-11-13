/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.LayoutDefaults

/** Keeps the reading flow mounted while the optional Home context appears or disappears. */
@Composable
fun HomeFeedLayout(modifier: Modifier = Modifier, context: @Composable () -> Unit, content: @Composable () -> Unit) {
    val contextState = rememberSaveableStateHolder()
    BoxWithConstraints(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val showContext = maxWidth >= 1180.dp
        Row(
            Modifier.widthIn(max = 1280.dp).fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(if (showContext) 24.dp else 0.dp),
        ) {
            Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 900.dp).fillMaxSize()) { content() }
            }
            if (showContext) {
                Box(Modifier.width(300.dp).fillMaxHeight().padding(end = 16.dp)) {
                    contextState.SaveableStateProvider("home-context") { context() }
                }
            }
        }
    }
}

internal fun adaptiveColumnCount(widthDp: Float): Int =
    if (widthDp < 640f) 1 else (widthDp / 300f).toInt().coerceAtLeast(2)

/** A single grid owner retains item keys and reading position while its column count changes. */
@Composable
fun AdaptiveItemGrid(content: LazyGridScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(adaptiveColumnCount(maxWidth.value)),
            modifier = Modifier.fillMaxSize().testTag("adaptive-grid"),
            contentPadding = LayoutDefaults.contentPadding,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}
