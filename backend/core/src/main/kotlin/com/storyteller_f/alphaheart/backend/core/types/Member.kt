/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.NestedMemberInfo
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

data class Member(
    val id: PrimaryKey,
    val uid: PrimaryKey,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
    val createdTime: LocalDateTime,
    val status: MemberStatus,
    val joinedTime: LocalDateTime? = null,
    val invitedTime: LocalDateTime? = null,
) {
    companion object
}

fun Member.toNestedMemberInfo() =
    NestedMemberInfo(
        status,
        joinedTime,
        invitedTime,
    )

fun Member.toMemberInfo(userInfo: UserInfo) =
    MemberInfo(
        id,
        uid,
        objectId,
        objectType,
        status,
        joinedTime,
        invitedTime,
        userInfo,
    )
