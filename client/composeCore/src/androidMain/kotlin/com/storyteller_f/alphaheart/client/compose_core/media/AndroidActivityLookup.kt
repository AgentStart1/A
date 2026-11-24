/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.media

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity

internal fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    error("Picture in picture should be called in the context of an Activity")
}
