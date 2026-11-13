/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import androidx.compose.ui.Modifier

/** Keeps native merged text while retaining the browser test overlay's room label. */
internal expect fun Modifier.roomCellSemantics(label: String?, onClick: (() -> Unit)?): Modifier
