/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UserStatus

internal enum class TopicEditorAccess { LOADING, READ_ONLY, WRITABLE }

/** A reply cannot be submitted before its parent topic's write restriction is known. */
internal fun topicEditorAccess(
    status: UserStatus?,
    parentType: ObjectType,
    parentReadOnly: Boolean?,
): TopicEditorAccess =
    when {
        status != UserStatus.NORMAL -> TopicEditorAccess.READ_ONLY
        parentType != ObjectType.TOPIC -> TopicEditorAccess.WRITABLE
        parentReadOnly == null -> TopicEditorAccess.LOADING
        parentReadOnly -> TopicEditorAccess.READ_ONLY
        else -> TopicEditorAccess.WRITABLE
    }
