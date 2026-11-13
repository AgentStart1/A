/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the TopicAction operation boundary. */
sealed interface TopicActionError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        TopicActionError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : TopicActionError

    companion object {
        fun fromError(error: FailureDetails): TopicActionError =
            if (error is TopicActionError) error else from(error.cause)

        fun from(cause: Throwable): TopicActionError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
