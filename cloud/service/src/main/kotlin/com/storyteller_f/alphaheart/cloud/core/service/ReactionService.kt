/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.CommonPath
import com.storyteller_f.alphaheart.api.DeleteReaction
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.ReactionFetch
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.backend.core.types.ReactionRecord
import com.storyteller_f.alphaheart.cloud.core.service.error.ReactionError
import com.storyteller_f.result.ConstraintFailure
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.recover
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import com.storyteller_f.shared.utils.safeFirstEmoji
import io.github.aakira.napier.Napier

suspend fun Backend.addReaction(userId: PrimaryKey, topicId: PrimaryKey, emojiText: String) =
    checkObjectWritable(ObjectType.TOPIC, topicId).andThenPresent(ReactionError::fromError) {
        checkTopicWritePermission(topicId, userId)
    }.andThenPresent(ReactionError::fromError) {
        database.reaction.getReactionInfo(userId, topicId, emojiText)
            .andThen(ReactionError::fromError) { oldReaction ->
                if (oldReaction != null && oldReaction.hasReacted) {
                    CustomResult.success(oldReaction)
                } else {
                    val newId = SnowflakeFactory.nextId()
                    val reactionRecord = ReactionRecord(userId, topicId, ObjectType.TOPIC, emojiText, newId, now())
                    val reactionInfo =
                        ReactionInfo(
                            reactionRecord.emoji,
                            reactionRecord.objectId,
                            (oldReaction?.count ?: 0) + 1,
                            true,
                            reactionRecord.id,
                        )
                    database.reaction.insertReaction(reactionRecord).map {
                        database.reaction.statsReactionRecord(
                            reactionRecord.objectId,
                            reactionRecord.emoji,
                            reactionRecord.objectType,
                        ).onFailure { throwable ->
                            Napier.e(throwable = throwable.cause) {
                                "addReaction"
                            }
                        }
                        reactionInfo
                    }.recover(ReactionError::fromError) { failure ->
                        if (failure is ConstraintFailure) {
                            CustomResult.success(reactionInfo)
                        } else {
                            CustomResult.Failure(failure)
                        }
                    }
                }
            }
    }

suspend fun Backend.reactionList(
    objectId: PrimaryKey,
    uid: PrimaryKey?,
    fillHasReacted: Boolean?,
    reactionFetch: ReactionFetch,
): CustomResult<PaginationResult<ReactionInfo>, ReactionError> {
    if (fillHasReacted == true && uid == null) {
        return CustomResult.Failure(
            ReactionError.from(UnauthorizedException()),
        ).mapError(ReactionError::fromError)
    }
    return database.reaction.getReactionInfoPaginationResult(
        listOf(objectId),
        uid,
        reactionFetch,
    ).mapError(ReactionError::fromError)
}

suspend fun addReaction(
    emoji: String,
    backend: Backend,
    uid: PrimaryKey,
    p: CommonPath,
): CustomResult<ReactionInfo?, ReactionError> =
    (
        if (isEmoji(emoji)) {
            backend.addReaction(uid, p.id, emoji)
        } else {
            CustomResult.Failure(ReactionError.from(CustomBadRequestException("invalid emoji")))
        }
        ).mapError(ReactionError::fromError)

suspend fun deleteReaction(
    deleteReaction: DeleteReaction,
    backend: Backend,
    uid: PrimaryKey,
    p: CommonPath,
): CustomResult<ReactionInfo?, ReactionError> {
    val emoji = deleteReaction.emoji
    return if (isEmoji(emoji)) {
        backend.checkObjectWritable(ObjectType.TOPIC, p.id).andThenPresent(ReactionError::fromError) {
            backend.database.reaction.deleteReaction(uid, emoji, p.id).andThen(ReactionError::fromError) {
                if (it) {
                    backend.database.reaction.statsReactionRecord(p.id, emoji, ObjectType.TOPIC)
                } else {
                    CustomResult.Success(Unit)
                }
            }
        }
    } else {
        CustomResult.Failure(ReactionError.from(CustomBadRequestException("invalid emoji")))
    }.andThen(ReactionError::fromError) {
        backend.database.reaction.getReactionInfo(uid, p.id, emoji)
            .map { reactionInfo ->
                reactionInfo ?: ReactionInfo(emoji, p.id, 0, false, 0)
            }
    }.mapError(ReactionError::fromError)
}

private fun isEmoji(emoji: String): Boolean = safeFirstEmoji(emoji)?.length == emoji.length
