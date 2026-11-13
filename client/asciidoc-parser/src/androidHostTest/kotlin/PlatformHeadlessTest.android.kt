/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.asciidoc_parser

import android.content.ContentProvider
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
actual abstract class PlatformHeadlessTest {
    @Before
    fun setup() {
        val provider =
            Class.forName("org.jetbrains.compose.resources.AndroidContextProvider")
                .asSubclass(ContentProvider::class.java)
        Robolectric.setupContentProvider(provider)
    }
}
