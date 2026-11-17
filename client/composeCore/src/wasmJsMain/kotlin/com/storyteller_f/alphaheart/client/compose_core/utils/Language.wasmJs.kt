/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlinx.browser.window

actual fun getCurrentLanguage(): String = window.navigator.language.takeIf { it.isNotBlank() } ?: "en"
