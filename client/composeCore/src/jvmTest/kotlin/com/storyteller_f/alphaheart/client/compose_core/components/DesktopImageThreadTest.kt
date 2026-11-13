/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import com.github.panpf.zoomimage.util.requiredMainThread
import com.github.panpf.zoomimage.util.setMainThreadChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DesktopImageThreadTest {
    @Test
    fun nativeWindowThreadIsAcceptedAndWorkerRejected(): Unit =
        runBlocking {
            try {
                bindDesktopImageThread(Thread.currentThread())
                requiredMainThread()
                withContext(Dispatchers.Default) {
                    assertFailsWith<IllegalStateException> { requiredMainThread() }
                }
            } finally {
                setMainThreadChecker(null)
            }
        }
}
