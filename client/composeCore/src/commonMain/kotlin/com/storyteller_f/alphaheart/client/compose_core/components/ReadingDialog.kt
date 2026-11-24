/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.storyteller_f.alphaheart.client.compose_core.navigation.readingDialogProperties

/** The platform dialog keeps modal focus and dismissal; only its scrim follows the reading design. */
@Composable
fun ReadingDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val scrim = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color(0x88000000) else Color(0x55173121)
    Dialog(onDismissRequest, readingDialogProperties(properties, scrim)) {
        ReadingDialogBody(properties, scrim, onDismissRequest, content)
    }
}

@Composable
fun ReadingBasicAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    ReadingDialog(onDismissRequest, properties) {
        Box(modifier.widthIn(min = 280.dp, max = 560.dp), propagateMinConstraints = true) { content() }
    }
}

@Composable
fun ReadingAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
    maxWidth: Dp = 320.dp,
) {
    ReadingDialog(onDismissRequest, properties) {
        Box(modifier) {
            ReadingActionMenuSurface(onDismissRequest, heading = {
                ProvideTextStyle(
                    MaterialTheme.typography.titleLarge.copy(
                        fontSize = 17.sp,
                        lineHeight = 23.8.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                ) { title?.invoke() }
            }, maxWidth = maxWidth) {
                icon?.invoke()
                Box(Modifier.fillMaxWidth().padding(top = 20.dp)) { text?.invoke() }
                Row(
                    Modifier.fillMaxWidth().padding(top = 22.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

@Composable
internal expect fun ReadingDialogBody(
    properties: DialogProperties,
    scrim: Color,
    dismiss: () -> Unit,
    content: @Composable () -> Unit,
)
