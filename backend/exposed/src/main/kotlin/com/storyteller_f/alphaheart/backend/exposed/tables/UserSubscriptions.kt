/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.exposed.tables

import com.storyteller_f.alphaheart.backend.core.types.SubscriptionSentLog
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.backend.exposed.BaseTable
import com.storyteller_f.alphaheart.backend.exposed.customPrimaryKey
import com.storyteller_f.alphaheart.backend.exposed.objectType
import org.jetbrains.exposed.v1.core.ResultRow

object UserSubscriptions : BaseTable() {
    val uid = customPrimaryKey("uid")
    val objectId = customPrimaryKey("object_id")
    val objectType = objectType("object_type")

    init {
        uniqueIndex("subscription-main", uid, objectId)
    }
}

fun UserSubscription.Companion.wrapRow(resultRow: ResultRow): UserSubscription =
    UserSubscription(
        resultRow[UserSubscriptions.id],
        resultRow[UserSubscriptions.uid],
        resultRow[UserSubscriptions.objectId],
        resultRow[UserSubscriptions.objectType],
        resultRow[UserSubscriptions.createdTime],
    )

object SubscriptionSentLogs : BaseTable() {
    val uid = customPrimaryKey("uid")
    val objectId = customPrimaryKey("object_id")
    val objectType = objectType("object_type")
    val subscriptionId = customPrimaryKey("subscription_id")

    init {
        uniqueIndex("subscription-sent-main", objectId, subscriptionId)
    }
}

fun SubscriptionSentLog.Companion.wrapRow(resultRow: ResultRow): SubscriptionSentLog =
    SubscriptionSentLog(
        resultRow[SubscriptionSentLogs.id],
        resultRow[SubscriptionSentLogs.uid],
        resultRow[SubscriptionSentLogs.objectId],
        resultRow[SubscriptionSentLogs.objectType],
        resultRow[SubscriptionSentLogs.subscriptionId],
        resultRow[SubscriptionSentLogs.createdTime],
    )
