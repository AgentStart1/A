/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard

actual suspend fun Clipboard.setText(string: String) {
    setClipEntry(ClipEntry(ClipData.newPlainText("text", string)))
}
