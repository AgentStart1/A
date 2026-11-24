/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey

sealed interface TopicComposeData {
    fun getMediaTarget(): ObjectTuple?
    fun getParent(): ObjectTuple

    data class PublicRoom(val roomId: PrimaryKey, val communityId: PrimaryKey, val parentTuple: ObjectTuple) :
        TopicComposeData {
        override fun getMediaTarget(): ObjectTuple? = null

        override fun getParent() = parentTuple
    }

    data class PrivateRoom(val roomId: PrimaryKey, val parentTuple: ObjectTuple) : TopicComposeData {
        override fun getMediaTarget(): ObjectTuple = roomId ob ObjectType.ROOM

        override fun getParent() = parentTuple
    }

    data class User(val uid: PrimaryKey, val objectTuple: ObjectTuple) : TopicComposeData {
        override fun getMediaTarget(): ObjectTuple? = null

        override fun getParent() = objectTuple
    }

    data class Community(val communityId: PrimaryKey, val objectTuple: ObjectTuple) : TopicComposeData {
        override fun getMediaTarget(): ObjectTuple? = null

        override fun getParent(): ObjectTuple = objectTuple
    }
}
