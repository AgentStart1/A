/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun CharSequenceText(content: CharSequence) {
    if (content is String) {
        Text(content)
    } else {
        Text(content::class.simpleName.orEmpty())
    }
}
