/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.Modifier

/** Native accessibility adaptation of [appiumSemantics]. */
@Suppress("LibraryEntitiesShouldNotBePublic", "LongParameterList")
actual fun Modifier.appiumSemantics(
    testTag: String?,
    description: String?,
    text: String?,
    input: Boolean,
    inputValue: String?,
    onInputValueChange: ((String) -> Unit)?,
    onClick: (() -> Unit)?,
    onInputSubmit: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
): Modifier = nativeAppiumSemantics(testTag, description, text, onClick)

internal expect fun Modifier.nativeAppiumSemantics(
    tag: String?,
    description: String?,
    label: String?,
    click: (() -> Unit)?,
): Modifier
