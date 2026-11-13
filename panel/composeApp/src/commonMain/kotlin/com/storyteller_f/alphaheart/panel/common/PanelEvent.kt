/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.common

import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.ObjectStatus
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus

data class OnUserAdded(val info: UserInfo)

data class OnUserStatusUpdated(val uid: PrimaryKey, val status: UserStatus)

data class OnCommunityStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

data class OnRoomStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

data class OnTopicStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

data class OnTitleStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)

data class OnFileStatusUpdated(val id: PrimaryKey, val status: ObjectStatus)
