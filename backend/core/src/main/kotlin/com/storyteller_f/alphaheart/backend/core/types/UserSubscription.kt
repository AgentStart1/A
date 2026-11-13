/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.types

import com.storyteller_f.shared.model.UserSubscriptionInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.datetime.LocalDateTime

class UserSubscription(
    val id: PrimaryKey,
    val uid: PrimaryKey,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
    val createdTime: LocalDateTime,
) {
    fun objectTuple() = ObjectTuple(objectId, objectType)

    companion object
}

fun UserSubscription.toUserSubscriptionInfo(): UserSubscriptionInfo =
    UserSubscriptionInfo(
        id,
        uid,
        objectId,
        objectType,
        createdTime,
    )

data class SubscriptionSentLog(
    val id: PrimaryKey,
    val uid: PrimaryKey,
    val objectId: PrimaryKey,
    val objectType: ObjectType,
    val subscriptionId: PrimaryKey,
    val createdTime: LocalDateTime,
) {
    companion object
}
