/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MediaTimeTest {
    @Test
    fun `time labels use minutes and optional hours`() {
        listOf(
            0.0 to "0:00",
            9.9 to "0:09",
            65.0 to "1:05",
            3599.0 to "59:59",
            3600.0 to "1:00:00",
            3723.0 to "1:02:03",
        ).forEach { (seconds, label) ->
            assertEquals(label, formatMediaTime(seconds))
        }
    }

    @Test
    fun `invalid and negative times display zero`() {
        listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertEquals("0:00", formatMediaTime(it))
        }
    }
}
