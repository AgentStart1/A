/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import com.storyteller_f.alphaheart.app.pages.topic.TopicEditorAccess
import com.storyteller_f.alphaheart.app.pages.topic.topicEditorAccess
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UserStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class TopicEditorAccessTest {
    @Test
    fun unknownParentRestrictionBlocksSubmissionUntilLoaded() {
        assertEquals(TopicEditorAccess.LOADING, topicEditorAccess(UserStatus.NORMAL, ObjectType.TOPIC, null))
        assertEquals(TopicEditorAccess.READ_ONLY, topicEditorAccess(UserStatus.NORMAL, ObjectType.TOPIC, true))
        assertEquals(TopicEditorAccess.WRITABLE, topicEditorAccess(UserStatus.NORMAL, ObjectType.TOPIC, false))
    }

    @Test
    fun accountRestrictionOverridesWritableParent() {
        assertEquals(TopicEditorAccess.READ_ONLY, topicEditorAccess(UserStatus.READ_ONLY, ObjectType.TOPIC, false))
        assertEquals(TopicEditorAccess.READ_ONLY, topicEditorAccess(null, ObjectType.TOPIC, false))
    }

    @Test
    fun rootCompositionDoesNotWaitForAnUnrelatedTopic() {
        assertEquals(TopicEditorAccess.WRITABLE, topicEditorAccess(UserStatus.NORMAL, ObjectType.COMMUNITY, null))
    }
}
