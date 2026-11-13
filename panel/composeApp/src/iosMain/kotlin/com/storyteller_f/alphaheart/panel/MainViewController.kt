/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.Dispatchers

fun MainViewController() =
    ComposeUIViewController {
        val scope = rememberCoroutineScope { Dispatchers.Default.limitedParallelism(1) }
        val panelUiViewModel = remember(scope) { createPanelUIViewModel(scope) }
        CompositionLocalProvider(LocalPanelUiViewModel provides panelUiViewModel) {
            App()
        }
    }
