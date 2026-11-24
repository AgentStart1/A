/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.topic

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.topic_time_days
import com.storyteller_f.alphaheart.app.topic_time_hours
import com.storyteller_f.alphaheart.app.topic_time_minutes
import com.storyteller_f.alphaheart.app.topic_time_now
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun TopicRelativeTime(createdTime: LocalDateTime, modifier: Modifier = Modifier) {
    val sentAt = createdTime.toInstant(TimeZone.UTC)
    val now by produceState(Clock.System.now(), sentAt) {
        while (true) {
            value = Clock.System.now()
            delay(1.minutes)
        }
    }
    val minutes = (now - sentAt).inWholeMinutes.coerceAtLeast(0)
    val label =
        when {
            minutes == 0L -> stringResource(Res.string.topic_time_now)
            minutes < 60 -> stringResource(Res.string.topic_time_minutes, minutes.toString())
            minutes < 1440 -> stringResource(Res.string.topic_time_hours, (minutes / 60).toString())
            else -> stringResource(Res.string.topic_time_days, (minutes / 1440).toString())
        }
    Text(
        label,
        modifier.testTag("topic-relative-time"),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
    )
}
