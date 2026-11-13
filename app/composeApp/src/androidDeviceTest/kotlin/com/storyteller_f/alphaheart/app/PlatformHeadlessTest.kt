/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.shared.appContextRef
import org.junit.Before
import java.lang.ref.WeakReference

actual abstract class PlatformHeadlessTest {
    @Before
    fun setup() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        appContextRef = WeakReference(application)
    }
}
