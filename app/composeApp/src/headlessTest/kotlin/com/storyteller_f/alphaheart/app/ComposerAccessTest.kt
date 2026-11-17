/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import com.storyteller_f.alphaheart.app.pages.room.canWriteRoomMessage
import com.storyteller_f.shared.type.UserStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposerAccessTest {
    @Test
    fun readOnlyTopicDisablesRepliesInWritableRoom() {
        assertFalse(canWriteRoomMessage(UserStatus.NORMAL, true, false, true))
    }

    @Test
    fun roomRestrictionsStillApplyToWritableTopics() {
        assertFalse(canWriteRoomMessage(UserStatus.NORMAL, true, true, false))
        assertFalse(canWriteRoomMessage(UserStatus.NORMAL, false, false, false))
        assertFalse(canWriteRoomMessage(null, true, false, false))
    }

    @Test
    fun joinedWriterCanReplyToWritableTopic() {
        assertTrue(canWriteRoomMessage(UserStatus.NORMAL, true, false, false))
    }
}
