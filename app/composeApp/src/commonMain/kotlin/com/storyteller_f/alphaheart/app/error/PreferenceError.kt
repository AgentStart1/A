/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Preference operation boundary. */
sealed interface PreferenceError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        PreferenceError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : PreferenceError

    companion object {
        fun fromError(error: FailureDetails): PreferenceError =
            if (error is PreferenceError) error else from(error.cause)

        fun from(cause: Throwable): PreferenceError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
