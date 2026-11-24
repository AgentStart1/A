/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.navigation

internal fun adaptiveColumnCount(widthDp: Float): Int =
    if (widthDp < 640f) 1 else (widthDp / 300f).toInt().coerceAtLeast(2)
