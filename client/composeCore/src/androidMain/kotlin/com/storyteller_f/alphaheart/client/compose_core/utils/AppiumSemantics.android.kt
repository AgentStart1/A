/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.Modifier

internal actual fun Modifier.nativeAppiumSemantics(
    tag: String?,
    description: String?,
    label: String?,
    click: (() -> Unit)?,
): Modifier = this
