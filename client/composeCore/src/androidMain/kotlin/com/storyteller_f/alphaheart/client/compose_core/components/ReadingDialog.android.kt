/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

@Composable
internal actual fun ReadingDialogBody(
    properties: DialogProperties,
    scrim: Color,
    dismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val window = (LocalView.current.parent as DialogWindowProvider).window
    DisposableEffect(window) {
        val originalDim = window.attributes.dimAmount
        window.setDimAmount(0f)
        onDispose { window.setDimAmount(originalDim) }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // A sibling behind the surface receives outside taps without intercepting its controls.
        Box(
            Modifier.matchParentSize().background(scrim).pointerInput(properties.dismissOnClickOutside, dismiss) {
                detectTapGestures { if (properties.dismissOnClickOutside) dismiss() }
            },
        )
        val width = if (properties.usePlatformDefaultWidth) Modifier.widthIn(min = 280.dp, max = 560.dp) else Modifier
        Box(
            Modifier.then(if (properties.usePlatformDefaultWidth) Modifier.padding(20.dp) else Modifier).then(width),
            contentAlignment = Alignment.Center,
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}
