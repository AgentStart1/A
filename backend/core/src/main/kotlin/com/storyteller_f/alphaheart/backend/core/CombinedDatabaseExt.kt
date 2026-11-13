/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.storyteller_f.alphaheart.backend.core.error.DatabaseError
import com.storyteller_f.alphaheart.backend.core.types.RawTopic
import com.storyteller_f.alphaheart.backend.core.types.RawUserOverview
import com.storyteller_f.alphaheart.backend.core.types.Topic
import com.storyteller_f.alphaheart.backend.core.types.UserFavorite
import com.storyteller_f.alphaheart.backend.core.types.UserSubscription
import com.storyteller_f.alphaheart.backend.core.types.UserTopicRead
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.firstOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicPinSearch
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.associateByPair

/** Assembles a full [RawUserOverview] for the given user. */
suspend fun CombinedDatabase.getUserOverview(uid: PrimaryKey): CustomResult<RawUserOverview, DatabaseError> =
    captureFailure(DatabaseError::from) {
        val subscriptionCount = subscription.getUserSubscriptionCount(uid).getOrThrow()
        val favoriteCount = favorite.getUserFavoriteCount().getOrThrow()
        val childAccountCount = user.getChildAccountCount(uid).getOrThrow()
        val reactionRecordCount = reaction.getUserReactionRecordCount(uid).getOrThrow()
        val commentCount = topic.getUserCommentCount(uid).getOrThrow()
        val childAccountIds = user.getChildAccountIds(uid).getOrThrow()
        val hasUnreadChildRoomMessage =
            if (childAccountIds.isEmpty()) {
                false
            } else {
                container.getUsersHasUnreadRoomMap(childAccountIds)
                    .getOrThrow()
                    .values
                    .any { it }
            }
        val rawUser =
            user.getRawUser(ObjectFetch.IdFetch(uid), uid).getOrThrow()
                ?: error("user not found")
        RawUserOverview(
            subscriptionCount,
            favoriteCount,
            rawUser.user.acgAmount,
            childAccountCount,
            reactionRecordCount,
            commentCount,
            hasUnreadChildRoomMessage,
            rawUser,
        )
    }

/** Enriches raw [Topic] objects with content, counts, and user state. */
suspend fun CombinedDatabase.processTopicToRawTopic(
    uid: PrimaryKey?,
    topics: List<Topic>,
): CustomResult<List<RawTopic>, DatabaseError> =
    captureFailure(DatabaseError::from) {
        val topicIds = topics.map { it.id }
        if (topicIds.isEmpty()) {
            emptyList()
        } else {
            buildRawTopics(uid, topics, topicIds)
        }
    }

private suspend fun CombinedDatabase.buildRawTopics(
    uid: PrimaryKey?,
    topics: List<Topic>,
    topicIds: List<PrimaryKey>,
): List<RawTopic> {
    val commentedSet =
        if (uid != null) {
            topic.isUserCommented(uid, topicIds).map { it.toSet() }.getOrThrow()
        } else {
            emptySet()
        }
    val commentCountMap =
        topic.getTopicCommentCount(topicIds).map { it.associateByPair() }.getOrThrow()
    val reactionCountMap =
        reaction.getReactionCount(topicIds).map { it.associateByPair() }.getOrThrow()
    val lastReadMap = fetchLastReadMap(uid, topicIds)
    val contentMap = topic.getTopicContentFromByteArray(topics, uid).getOrThrow()
    val favoriteMap = fetchFavoriteMap(uid, topicIds)
    val subscriptionMap = fetchSubscriptionMap(uid, topicIds)

    return topics.map { t ->
        val id = t.id
        RawTopic(
            t,
            contentMap[id] ?: TopicContent.Nil,
            commentCountMap[id] ?: 0,
            commentedSet.contains(id),
            reactionCountMap[id] ?: 0,
            lastReadMap[id]?.topicId,
            favoriteId = favoriteMap[id]?.id,
            subscriptionId = subscriptionMap[id]?.id,
        )
    }
}

private suspend fun CombinedDatabase.fetchLastReadMap(
    uid: PrimaryKey?,
    topicIds: List<PrimaryKey>,
): Map<PrimaryKey, UserTopicRead> =
    if (uid != null) {
        container.getTopicReadList(topicIds, uid)
            .map {
                it.associateBy { userTopicRead -> userTopicRead.objectId }
            }
            .getOrThrow()
    } else {
        emptyMap()
    }

private suspend fun CombinedDatabase.fetchFavoriteMap(
    uid: PrimaryKey?,
    topicIds: List<PrimaryKey>,
): Map<PrimaryKey, UserFavorite> =
    if (uid != null) {
        favorite.getHasFavorite(ObjectListFetch.IdListFetch(topicIds), uid)
            .getOrThrow()
            .associateBy { it.objectId }
    } else {
        emptyMap()
    }

private suspend fun CombinedDatabase.fetchSubscriptionMap(
    uid: PrimaryKey?,
    topicIds: List<PrimaryKey>,
): Map<PrimaryKey, UserSubscription> =
    if (uid != null) {
        subscription.getHasSubscription(ObjectListFetch.IdListFetch(topicIds), uid)
            .getOrThrow()
            .associateBy { it.objectId }
    } else {
        emptyMap()
    }

/** Returns a single enriched topic or null when not found. */
suspend fun CombinedDatabase.getRawTopic(fetch: ObjectFetch, uid: PrimaryKey?): CustomResult<RawTopic?, DatabaseError> =
    topic.getTopic(fetch).andThenPresent(DatabaseError::fromError) { t ->
        processTopicToRawTopic(uid, listOf(t))
    }.firstOrNull()

/** Returns a paginated list of enriched topics. */
suspend fun CombinedDatabase.getAllRawTopics(
    primaryKeyFetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<RawTopic>, DatabaseError> =
    topic.getAllTopicPagination(primaryKeyFetch).andThen(DatabaseError::fromError) {
        processTopicToRawTopic(null, it.list).pagingNotNull(it.total)
    }

/** Returns enriched topics for the given [ids]. */
suspend fun CombinedDatabase.getRawTopicListByIds(
    uid: PrimaryKey?,
    ids: List<PrimaryKey>,
): CustomResult<List<RawTopic>, DatabaseError> =
    topic.getTopicListByIds(ids).andThen(DatabaseError::fromError) {
        processTopicToRawTopic(uid, it)
    }

/** Returns paginated child topics for a parent, enriched with user state. */
suspend fun CombinedDatabase.getRawTopicByParentId(
    uid: PrimaryKey?,
    primaryKeyFetch: PrimaryKeyFetch,
    parentId: PrimaryKey,
    pinType: TopicPinSearch?,
): CustomResult<PaginationResult<RawTopic>, DatabaseError> =
    topic.getTopicByParentId(uid, primaryKeyFetch, parentId, pinType).andThen(DatabaseError::fromError) {
        processTopicToRawTopic(uid, it.list).pagingNotNull(it.total)
    }

/** Returns the most recent child topic for a parent, enriched with user state. */
suspend fun CombinedDatabase.getLatestRawTopic(
    uid: PrimaryKey?,
    parentId: PrimaryKey,
): CustomResult<List<RawTopic>, DatabaseError> =
    topic.getLatestTopic(parentId).andThen(DatabaseError::fromError) {
        processTopicToRawTopic(uid, it)
    }
