/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlinx.coroutines.CancellationException
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WasmBrowserProfileTest {
    @Test
    fun isolatesProfilesAndRemovesFailedStartup() =
        runAppiumBlockingTest {
            val allocated = mutableListOf<File>()
            assertFailsWith<IllegalStateException> {
                WasmBrowser.CHROME.withProfile { first ->
                    allocated.add(first)
                    File(first, "test-state").writeText("synthetic")
                    WasmBrowser.CHROME.withProfile { second ->
                        allocated.add(second)
                        assertNotEquals(first, second)
                        assertTrue(File(first, "test-state").isFile)
                        assertFalse(File(second, "test-state").exists())
                        error("Synthetic browser startup failure")
                    }
                }
            }
            assertTrue(allocated.all { !it.exists() })
        }

    @Test
    fun removesCancelledProfile() =
        runAppiumBlockingTest {
            val allocated = mutableListOf<File>()
            assertFailsWith<CancellationException> {
                WasmBrowser.FIREFOX.withProfile { profile ->
                    allocated.add(profile)
                    throw CancellationException("Synthetic cancellation")
                }
            }
            assertTrue(allocated.all { !it.exists() })
        }
}
