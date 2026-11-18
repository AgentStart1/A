/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.text.font.createFontFamilyResolver
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import org.jetbrains.skiko.InternalSkikoApi
import org.jetbrains.skiko.wasm.awaitSkiko
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertFailsWith

@OptIn(InternalSkikoApi::class, ExperimentalWasmJsInterop::class)
internal class BrowserFontPreloadTest {
    @Test
    internal fun rejectsEmptyFontBeforePreloadAcceptance() =
        runTest {
            awaitSkiko.await()
            assertFailsWith<IllegalArgumentException> {
                preloadBrowserFont("Empty", byteArrayOf(), createFontFamilyResolver())
            }
        }

    @Test
    internal fun rejectsNonFontBytesBeforePreloadAcceptance() =
        runTest {
            awaitSkiko.await()
            assertFailsWith<IllegalArgumentException> {
                preloadBrowserFont("Invalid", "not a font".encodeToByteArray(), createFontFamilyResolver())
            }
        }
}
