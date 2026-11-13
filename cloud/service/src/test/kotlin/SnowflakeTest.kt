/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class SnowflakeTest {
    @Test
    fun `test 'MAX_MACHINE_ID' generate snowflake`() {
        runBlocking {
            SnowflakeFactory.setMachine(SnowflakeFactory.MAX_MACHINE_ID)
            val newId = SnowflakeFactory.nextId()
            assertEquals(SnowflakeFactory.parse(newId).machineId, SnowflakeFactory.MAX_MACHINE_ID)
        }
    }

    @Test
    fun `test 'MACHINE_ID = 0' generate snowflake`() {
        runBlocking {
            SnowflakeFactory.setMachine(0)
            val newId = SnowflakeFactory.nextId()
            assertEquals(SnowflakeFactory.parse(newId).machineId, 0)
        }
    }
}
