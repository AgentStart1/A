/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Media operation boundary. */
sealed interface MediaError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        MediaError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : MediaError

    companion object {
        fun fromError(error: FailureDetails): MediaError = if (error is MediaError) error else from(error.cause)

        fun from(cause: Throwable): MediaError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
