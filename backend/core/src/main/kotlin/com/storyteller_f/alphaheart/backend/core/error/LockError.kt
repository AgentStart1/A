/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Lock operation boundary. */
sealed interface LockError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        LockError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : LockError

    companion object {
        fun fromError(error: FailureDetails): LockError = if (error is LockError) error else from(error.cause)

        fun from(cause: Throwable): LockError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
