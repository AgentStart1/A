/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Download operation boundary. */
sealed interface DownloadError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        DownloadError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : DownloadError

    companion object {
        fun fromError(error: FailureDetails): DownloadError = if (error is DownloadError) error else from(error.cause)

        fun from(cause: Throwable): DownloadError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
