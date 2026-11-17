/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/** Stored dates are UTC; display the local date and its offset at that instant, including daylight saving. */
@OptIn(ExperimentalTime::class)
fun LocalDateTime.formatDisplayTime(zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val instant = toInstant(TimeZone.UTC)
    val local = instant.toLocalDateTime(zone)
    val offset = zone.offsetAt(instant).toString().let { if (it == "Z") "+00:00" else it }
    return "${local.toString().substringBefore('.').replace('T', ' ')} UTC$offset"
}

/** Date-only audit records retain their stored UTC date. */
fun LocalDate.formatDisplayTime(): String = "$this UTC+00:00"
