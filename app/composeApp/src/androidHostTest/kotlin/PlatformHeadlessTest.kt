/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import android.content.ContentProvider
import com.storyteller_f.shared.appContextRef
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.lang.ref.WeakReference

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
actual abstract class PlatformHeadlessTest {
    @Before
    fun setup() {
        System.setProperty("robolectric.logging.enabled", "true")
        setupAndroidContextProvider()
        val application = RuntimeEnvironment.getApplication()
        appContextRef = WeakReference(application)
    }

    // Configures Compose's AndroidContextProvider to access resources in tests.
    // See https://youtrack.jetbrains.com/issue/CMP-6612
    private fun setupAndroidContextProvider() {
        val type = findAndroidContextProvider() ?: return
        Robolectric.setupContentProvider(type)
    }

    private fun findAndroidContextProvider(): Class<ContentProvider>? {
        val providerClassName = "org.jetbrains.compose.resources.AndroidContextProvider"
        return try {
            @Suppress("UNCHECKED_CAST")
            Class.forName(providerClassName) as Class<ContentProvider>
        } catch (_: ClassNotFoundException) {
            // Tests that don't depend on Compose will not have the provider class in classpath and will get
            // ClassNotFoundException. Skip configuring the provider for them.
            null
        }
    }
}
