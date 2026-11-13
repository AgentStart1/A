/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.buildNameService
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SnowflakeTest {
    @Test
    fun `test name`() {
        runBlocking {
            SnowflakeFactory.setMachine(0)
            val nameService = buildNameService(MergedEnv(emptyList()))
            assertTrue(nameService.parse(SnowflakeFactory.nextId()).isNotBlank())
        }
    }

    @Test
    fun `test path`() {
        val get = Paths.get(".")
        assertNotEquals(get.pathString, get.toRealPath().pathString)
    }
}
