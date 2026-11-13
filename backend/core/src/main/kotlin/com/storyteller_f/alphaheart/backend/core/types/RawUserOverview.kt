/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

data class RawUserOverview(
    val subscriptionCount: Long,
    val favoriteCount: Long,
    val acg: Long,
    val childAccountCount: Long,
    val reactionRecordCount: Long,
    val commentCount: Long,
    val hasUnreadChildRoomMessage: Boolean,
    val rawUser: RawUser,
)
