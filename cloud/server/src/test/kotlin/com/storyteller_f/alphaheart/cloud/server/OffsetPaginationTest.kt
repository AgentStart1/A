/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.cloud.server.common.GeneralOffsetPagingGenerator
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class OffsetPaginationTest {
    private val generator = GeneralOffsetPagingGenerator

    @Test
    fun `test parse offset`() {
        val fetch = generator.parse(null, "10", 10)
        Assertions.assertEquals(10, fetch.cursor?.value)
        Assertions.assertEquals(10, fetch.size)

        val fetch2 = generator.parse("10", null, 10)
        Assertions.assertEquals(10, fetch2.cursor?.value)

        val fetch3 = generator.parse(null, null, 20)
        Assertions.assertEquals(null, fetch3.cursor)
        Assertions.assertEquals(20, fetch3.size)
    }

    @Test
    fun `test generate tokens`() {
        val list = List(10) { it }

        // Case 1: Start (offset 0), has next
        val fetch1 = OffsetFetch(null, 10)
        val (pre1, next1) = generator.generate(list, fetch1)
        Assertions.assertEquals(null, pre1)
        Assertions.assertEquals("10", next1)

        // Case 2: Middle (offset 10)
        val fetch2 = generator.parse(null, "10", 10)
        val (pre2, next2) = generator.generate(list, fetch2)
        Assertions.assertEquals("0", pre2) // 10 - 10 = 0
        Assertions.assertEquals("20", next2) // 10 + 10 = 20

        // Case 3: End (offset 20, less items returned implies end, but generator checks list size vs fetch size)
        // If list size < fetch size, it means end.
        val partialList = List(5) { it }
        val fetch3 = generator.parse(null, "20", 10)
        val (pre3, next3) = generator.generate(partialList, fetch3)
        Assertions.assertEquals("10", pre3) // 20 - 10 = 10
        Assertions.assertEquals(null, next3) // list.size (5) < fetch.size (10), so no next
    }
}
