/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.DialogProperties

internal actual fun readingDialogProperties(properties: DialogProperties, scrim: Color): DialogProperties =
    DialogProperties(
        dismissOnBackPress = properties.dismissOnBackPress,
        dismissOnClickOutside = properties.dismissOnClickOutside,
        usePlatformDefaultWidth = false,
    )
