/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Image operation boundary. */
sealed interface ImageError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        ImageError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : ImageError

    companion object {
        fun fromError(error: FailureDetails): ImageError = if (error is ImageError) error else from(error.cause)

        fun from(cause: Throwable): ImageError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
