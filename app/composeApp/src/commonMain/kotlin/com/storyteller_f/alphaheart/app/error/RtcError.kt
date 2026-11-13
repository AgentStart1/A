/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Rtc operation boundary. */
sealed interface RtcError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        RtcError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : RtcError

    companion object {
        fun fromError(error: FailureDetails): RtcError = if (error is RtcError) error else from(error.cause)

        fun from(cause: Throwable): RtcError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
