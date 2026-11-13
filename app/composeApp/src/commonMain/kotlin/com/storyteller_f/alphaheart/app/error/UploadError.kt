/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Upload operation boundary. */
sealed interface UploadError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        UploadError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : UploadError

    companion object {
        fun fromError(error: FailureDetails): UploadError = if (error is UploadError) error else from(error.cause)

        fun from(cause: Throwable): UploadError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
