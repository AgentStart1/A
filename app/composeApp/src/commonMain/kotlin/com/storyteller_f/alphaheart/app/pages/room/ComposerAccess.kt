/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.room

import com.storyteller_f.shared.type.UserStatus

/** A topic may restrict replies even when its containing room remains writable. */
internal fun canWriteRoomMessage(
    status: UserStatus?,
    joined: Boolean,
    roomReadOnly: Boolean,
    topicReadOnly: Boolean,
): Boolean = status == UserStatus.NORMAL && joined && !roomReadOnly && !topicReadOnly
