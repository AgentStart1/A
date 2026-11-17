/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.runtime.Composable

@Composable
internal fun AuthPageChrome(title: String, footer: @Composable () -> Unit, content: @Composable () -> Unit) {
    com.storyteller_f.alphaheart.client.compose_core.components.ReadingAuthLayout(title) {
        content()
        footer()
    }
}
