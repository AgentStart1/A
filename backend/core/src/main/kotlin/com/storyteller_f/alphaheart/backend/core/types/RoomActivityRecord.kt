/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.type.PrimaryKey

data class RoomActivityRecord(val roomId: PrimaryKey, val latestMessageId: PrimaryKey?, val unreadMessageCount: Long)
