/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun SimpleMessageWithButton(message: String, title: String) {
    val alterDialogController = rememberAlertDialogController()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(message, maxLines = 1, modifier = Modifier.weight(1f), overflow = TextOverflow.MiddleEllipsis)
        IconButton({
            alterDialogController.showMessage(title, message, enableCopy = true)
        }) {
            Icon(Icons.Default.Fullscreen, "fullscreen")
        }
    }
    CustomAlertDialog(alterDialogController, {
        alterDialogController.close()
    }) {
    }
}
