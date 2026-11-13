/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the ModelGeneration operation boundary. */
sealed interface ModelGenerationError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        ModelGenerationError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : ModelGenerationError

    companion object {
        fun fromError(error: FailureDetails): ModelGenerationError =
            if (error is ModelGenerationError) error else from(error.cause)

        fun from(cause: Throwable): ModelGenerationError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
