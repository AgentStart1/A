/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.Modifier

/**
 * Adds an Appium-only interaction surface on platforms that require one.
 *
 * Native targets deliberately return the receiver unchanged.
 */
@Suppress("LibraryEntitiesShouldNotBePublic", "LongParameterList")
expect fun Modifier.appiumSemantics(
    testTag: String? = null,
    description: String? = null,
    text: String? = null,
    input: Boolean = false,
    inputValue: String? = null,
    onInputValueChange: ((String) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onInputSubmit: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
): Modifier
