/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** A stable slot for translating, streamed text, completion and failure. */
@Composable
fun ReadingTranslationResult(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(160.dp).testTag("translation-result-slot"),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
