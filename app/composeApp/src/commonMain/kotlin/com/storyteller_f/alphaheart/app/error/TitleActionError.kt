/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the TitleAction operation boundary. */
sealed interface TitleActionError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        TitleActionError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : TitleActionError

    companion object {
        fun fromError(error: FailureDetails): TitleActionError =
            if (error is TitleActionError) error else from(error.cause)

        fun from(cause: Throwable): TitleActionError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
