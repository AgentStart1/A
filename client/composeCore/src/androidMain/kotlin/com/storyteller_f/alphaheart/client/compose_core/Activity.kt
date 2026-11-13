/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat

fun ComponentActivity.commonForActivity() {
    enableEdgeToEdge()
    /*
     * 在Android 13 上必须加这一行
     */
    WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightNavigationBars = false
}
