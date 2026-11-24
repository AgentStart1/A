/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CreateFormLayout(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        val side =
            when {
                maxWidth <= 600.dp -> 16.dp
                maxWidth <= 820.dp -> 22.dp
                else -> 30.dp
            }
        val top = if (maxWidth <= 600.dp) 20.dp else side
        Column(
            Modifier.padding(horizontal = side).widthIn(max = 520.dp).fillMaxWidth()
                .verticalScroll(rememberScrollState()).padding(top = top, bottom = 30.dp),
            content = content,
        )
    }
}
