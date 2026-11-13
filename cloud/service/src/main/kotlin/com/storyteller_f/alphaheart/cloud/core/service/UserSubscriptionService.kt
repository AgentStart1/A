/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.addIfNotExists
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.backend.core.types.toUserSubscriptionInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.UserSubscriptionError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.firstOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.model.UserSubscriptionItem
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now

suspend fun Backend.addSubscription(uid: PrimaryKey, newSubscription: NewSubscription) =
    checkObjectWritable(
        newSubscription.objectType,
        newSubscription.objectId,
    ).andThenPresent(UserSubscriptionError::fromError) {
        addIfNotExists({
            database.subscription.getSubscription(uid, newSubscription.objectId)
        }, {
            val id = SnowflakeFactory.nextId()
            val userSubscription =
                UserSubscription(id, uid, newSubscription.objectId, newSubscription.objectType, now())
            database.subscription.addSubscription(userSubscription).onSuccess {
                addUserLog(uid, UserLogType.ADD_SUBSCRIPTION, newSubscription.tuple())
            }
        })
    }.andThenPresent(UserSubscriptionError::fromError) {
        buildSubscriptionItems(uid, listOf(it))
    }.firstOrNull()

suspend fun Backend.removeSubscription(uid: PrimaryKey, subscriptionId: PrimaryKey) =
    database.subscription.getSubscription(subscriptionId)
        .andThenPresent(UserSubscriptionError::fromError) { subscription ->
            if (subscription.uid == uid) {
                checkObjectWritable(
                    subscription.objectType,
                    subscription.objectId,
                ).andThenPresent(UserSubscriptionError::fromError) {
                    database.subscription.removeSubscription(subscriptionId).onSuccess {
                        addUserLog(uid, UserLogType.REMOVE_SUBSCRIPTION, subscription.objectTuple())
                    }
                }
            } else {
                CustomResult.Failure(
                    UserSubscriptionError.from(ForbiddenException("No permission to remove this subscription")),
                )
            }
        }

suspend fun Backend.removeSubscriptionByObject(uid: PrimaryKey, objectId: PrimaryKey) =
    database.subscription.getSubscription(uid, objectId)
        .andThenPresent(UserSubscriptionError::fromError) { subscription ->
            checkObjectWritable(
                subscription.objectType,
                subscription.objectId,
            ).andThenPresent(UserSubscriptionError::fromError) {
                database.subscription.removeSubscription(subscription.id).onSuccess {
                    addUserLog(uid, UserLogType.REMOVE_SUBSCRIPTION, subscription.objectTuple())
                }
            }
        }

suspend fun Backend.getUserSubscriptions(uid: PrimaryKey, fetch: PrimaryKeyFetch) =
    database.subscription.getUserSubscriptions(
        uid,
        fetch,
    ).mapPagingResultNotNull(UserSubscriptionError::fromError) { list ->
        buildSubscriptionItems(uid, list)
    }

suspend fun Backend.buildSubscriptionItems(uid: PrimaryKey, userSubscriptions: List<UserSubscription>) =
    captureFailure(UserSubscriptionError::from) {
        userSubscriptions.map {
            val favoriteInfo = it.toUserSubscriptionInfo()
            when (it.objectType) {
                ObjectType.TOPIC -> {
                    val topicInfo = getTopic(it.objectId, uid, true).getOrThrow()
                    UserSubscriptionItem(favoriteInfo, topicInfo)
                }

                else -> UserSubscriptionItem(favoriteInfo)
            }
        }
    }
