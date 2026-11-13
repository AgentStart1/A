/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the ActivityPreview operation boundary. */
sealed interface ActivityPreviewError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        ActivityPreviewError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : ActivityPreviewError

    companion object {
        fun fromError(error: FailureDetails): ActivityPreviewError =
            if (error is ActivityPreviewError) error else from(error.cause)

        fun from(cause: Throwable): ActivityPreviewError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
