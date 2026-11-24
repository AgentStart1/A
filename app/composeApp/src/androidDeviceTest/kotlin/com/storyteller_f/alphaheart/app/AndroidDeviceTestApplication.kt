/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.app.Application
import com.storyteller_f.shared.appContextRef
import java.lang.ref.WeakReference

internal class AndroidDeviceTestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appContextRef = WeakReference(this)
    }
}
