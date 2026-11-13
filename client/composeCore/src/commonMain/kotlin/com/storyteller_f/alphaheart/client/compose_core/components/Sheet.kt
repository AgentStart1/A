/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseSheet(
    showSheet: Boolean,
    sheetState: SheetState,
    hideSheet: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                hideSheet()
            },
            dragHandle = null,
            sheetState = sheetState,
            contentWindowInsets = {
                WindowInsets(0)
            },
        ) {
            content()
        }
    }
}

@Composable
fun SheetContainer(block: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().consumeWindowInsets(WindowInsets.navigationBars)) {
        Spacer(modifier = Modifier.height(20.dp))
        block()
    }
}
