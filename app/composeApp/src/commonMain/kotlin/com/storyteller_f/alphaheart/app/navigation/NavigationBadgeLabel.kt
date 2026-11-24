/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.navigation

internal fun navigationDescription(label: String, hasUnread: Boolean, count: Long, unreadLabel: String): String =
    when {
        !hasUnread -> label
        count > 0 -> "$label, $unreadLabel, $count"
        else -> "$label, $unreadLabel"
    }
