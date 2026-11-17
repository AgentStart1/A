/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DisplayTimeTest {
    @Test
    fun `display converts stored UTC across a date boundary`() {
        assertEquals(
            "2026-11-17 01:30 UTC+08:00",
            LocalDateTime(2026, 11, 16, 17, 30).formatDisplayTime(TimeZone.of("Asia/Singapore")),
        )
    }

    @Test
    fun `display uses the offset at the recorded instant`() {
        val zone = TimeZone.of("America/New_York")
        assertEquals("2026-01-15 07:00 UTC-05:00", LocalDateTime(2026, 1, 15, 12, 0).formatDisplayTime(zone))
        assertEquals("2026-07-15 08:00 UTC-04:00", LocalDateTime(2026, 7, 15, 12, 0).formatDisplayTime(zone))
    }

    @Test
    fun `UTC and date only records always identify their timezone`() {
        assertEquals("2026-11-16 12:00 UTC+00:00", LocalDateTime(2026, 11, 16, 12, 0).formatDisplayTime(TimeZone.UTC))
        assertEquals("2026-11-16 UTC+00:00", LocalDate(2026, 11, 16).formatDisplayTime())
        assertEquals(
            "2026-11-16 12:00:57 UTC+00:00",
            LocalDateTime(2026, 11, 16, 12, 0, 57, 283_209_000).formatDisplayTime(TimeZone.UTC),
        )
    }
}
