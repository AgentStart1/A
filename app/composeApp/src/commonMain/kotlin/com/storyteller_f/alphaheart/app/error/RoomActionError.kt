/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the RoomAction operation boundary. */
sealed interface RoomActionError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        RoomActionError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : RoomActionError

    companion object {
        fun fromError(error: FailureDetails): RoomActionError =
            if (error is RoomActionError) error else from(error.cause)

        fun from(cause: Throwable): RoomActionError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
