/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.room

import com.storyteller_f.shared.model.TopicDetail
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

internal fun sameRoomMessageGroup(previous: TopicDetail?, current: TopicDetail): Boolean {
    if (previous == null || previous.author != current.author) return false
    val gap = current.createdTime.toInstant(TimeZone.UTC) - previous.createdTime.toInstant(TimeZone.UTC)
    return gap >= Duration.ZERO && gap <= 1.minutes
}
