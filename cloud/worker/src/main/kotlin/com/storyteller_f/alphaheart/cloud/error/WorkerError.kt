/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Worker operation boundary. */
sealed interface WorkerError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        WorkerError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : WorkerError

    companion object {
        fun fromError(error: FailureDetails): WorkerError = if (error is WorkerError) error else from(error.cause)

        fun from(cause: Throwable): WorkerError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
