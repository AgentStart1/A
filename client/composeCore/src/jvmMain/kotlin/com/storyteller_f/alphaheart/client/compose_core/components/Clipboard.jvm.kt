/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.awtClipboard
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual suspend fun Clipboard.setText(string: String) {
    awtClipboard?.setContents(StringSelection(string), null)
}
