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
import com.storyteller_f.alphaheart.backend.core.getRawTopicListByIds
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
import com.storyteller_f.shared.model.TopicDetail
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
        val topics = loadSubscriptionTopics(uid, userSubscriptions)
        userSubscriptions.map {
            val subscriptionInfo = it.toUserSubscriptionInfo()
            val topicInfo = if (it.objectType == ObjectType.TOPIC) topics[it.objectId] else null
            UserSubscriptionItem(subscriptionInfo, topicInfo)
        }
    }

private suspend fun Backend.loadSubscriptionTopics(
    uid: PrimaryKey,
    subscriptions: List<UserSubscription>,
): Map<PrimaryKey, TopicDetail> {
    val topicIds = subscriptions.filter { it.objectType == ObjectType.TOPIC }.map { it.objectId }.distinct()
    val readableIds =
        topicIds.filter { id ->
            val permission = checkTopicReadPermission(id, uid).getOrThrow()
            if (permission != null && !permission.hasRead) throw ForbiddenException("Permission Denied")
            permission != null
        }
    if (readableIds.isEmpty()) return emptyMap()
    val topics = database.getRawTopicListByIds(uid, readableIds).getOrThrow()
    return processTopicDetails(topics, uid).getOrThrow().orEmpty().associateBy { it.id }
}
