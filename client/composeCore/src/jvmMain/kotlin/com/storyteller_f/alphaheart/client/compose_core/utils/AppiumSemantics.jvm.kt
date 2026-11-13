/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString

internal actual fun Modifier.nativeAppiumSemantics(
    tag: String?,
    description: String?,
    label: String?,
    click: (() -> Unit)?,
): Modifier =
    semantics {
        tag?.let { testTag = it }
        (label ?: description)?.let { contentDescription = it }
        label?.let { text = AnnotatedString(it) }
        click?.let { action ->
            onClick {
                action()
                true
            }
        }
    }
