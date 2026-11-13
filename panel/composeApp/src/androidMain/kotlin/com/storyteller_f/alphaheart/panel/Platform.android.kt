/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
