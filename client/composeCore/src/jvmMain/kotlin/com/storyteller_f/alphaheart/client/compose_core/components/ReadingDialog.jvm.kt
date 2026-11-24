/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

@Composable
internal actual fun ReadingDialogBody(
    properties: DialogProperties,
    scrim: Color,
    dismiss: () -> Unit,
    content: @Composable () -> Unit,
) = content()
