/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class PanelSignoutHostTest {
    @Test
    fun cancelDoesNotSignOut() =
        runTest {
            var requests = 0
            val host = PanelSignoutHost(backgroundScope, StandardTestDispatcher(testScheduler)) { requests++ }
            host.request()
            runCurrent()
            assertTrue(host.confirming.value)
            host.cancel()
            runCurrent()
            assertFalse(host.confirming.value)
            assertEquals(0, requests)
        }

    @Test
    fun confirmationSignsOutOnlyOnce() =
        runTest {
            var requests = 0
            val host = PanelSignoutHost(backgroundScope, StandardTestDispatcher(testScheduler)) { requests++ }
            host.request()
            host.confirm()
            host.confirm()
            runCurrent()
            assertFalse(host.confirming.value)
            assertEquals(1, requests)
        }

    @Test
    fun closedHostCannotSignOut() =
        runTest {
            var requests = 0
            val host = PanelSignoutHost(backgroundScope, StandardTestDispatcher(testScheduler)) { requests++ }
            host.request()
            host.confirm()
            host.close()
            runCurrent()
            assertFalse(host.confirming.value)
            assertEquals(0, requests)
        }
}
