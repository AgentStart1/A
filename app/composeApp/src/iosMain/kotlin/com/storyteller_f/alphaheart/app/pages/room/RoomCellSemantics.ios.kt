/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.ui.Modifier
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics

internal actual fun Modifier.roomCellSemantics(label: String?, onClick: (() -> Unit)?): Modifier =
    appiumSemantics(testTag = label, onClick = onClick)
