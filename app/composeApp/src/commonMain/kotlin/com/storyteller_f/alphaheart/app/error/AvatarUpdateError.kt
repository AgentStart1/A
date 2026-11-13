/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the AvatarUpdate operation boundary. */
sealed interface AvatarUpdateError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        AvatarUpdateError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : AvatarUpdateError

    companion object {
        fun fromError(error: FailureDetails): AvatarUpdateError =
            if (error is AvatarUpdateError) error else from(error.cause)

        fun from(cause: Throwable): AvatarUpdateError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
