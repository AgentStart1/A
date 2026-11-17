/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.common

import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import com.storyteller_f.storage.PanelLogCollection

data class OnUserAdded(val info: UserInfo)

data class OnUserStatusUpdated(val uid: PrimaryKey, val status: UserStatus)

data class OnCommunityStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

data class OnRoomStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

data class OnTopicStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

data class OnTitleStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

data class OnFileStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

internal fun Any.statusLogCollection(): PanelLogCollection.PanelLogs? =
    when (this) {
        is OnUserStatusUpdated -> PanelLogCollection.PanelLogs(uid, ObjectType.USER)
        is OnCommunityStatusUpdated -> PanelLogCollection.PanelLogs(id, ObjectType.COMMUNITY)
        is OnRoomStatusUpdated -> PanelLogCollection.PanelLogs(id, ObjectType.ROOM)
        is OnTopicStatusUpdated -> PanelLogCollection.PanelLogs(id, ObjectType.TOPIC)
        is OnTitleStatusUpdated -> PanelLogCollection.PanelLogs(id, ObjectType.TITLE)
        is OnFileStatusUpdated -> PanelLogCollection.PanelLogs(id, ObjectType.FILE)
        else -> null
    }
