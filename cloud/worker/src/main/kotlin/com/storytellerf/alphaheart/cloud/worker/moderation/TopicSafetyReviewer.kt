/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.moderation

internal fun interface TopicSafetyReviewer : AutoCloseable {
    suspend fun isHarmful(content: String): Boolean

    override fun close() = Unit
}

internal class UnexpectedTopicSafetyDecisionException(
    response: String,
    expectedFormat: String,
    cause: Throwable? = null,
) : IllegalStateException(
    "Topic safety model did not return $expectedFormat, response: $response",
    cause,
)
