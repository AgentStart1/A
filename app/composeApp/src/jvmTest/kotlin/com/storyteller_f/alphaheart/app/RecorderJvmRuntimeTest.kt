/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import space.kodio.core.SystemAudioSystem
import kotlin.test.Test
import kotlin.test.assertNotNull

class RecorderJvmRuntimeTest {
    @Test
    fun audioSystemLoadsOnTheProjectRuntime() {
        assertNotNull(SystemAudioSystem)
    }
}
