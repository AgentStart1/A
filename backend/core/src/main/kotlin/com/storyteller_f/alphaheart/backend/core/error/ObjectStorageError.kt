/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the ObjectStorage operation boundary. */
sealed interface ObjectStorageError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        ObjectStorageError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : ObjectStorageError

    companion object {
        fun fromError(error: FailureDetails): ObjectStorageError =
            if (error is ObjectStorageError) error else from(error.cause)

        fun from(cause: Throwable): ObjectStorageError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
