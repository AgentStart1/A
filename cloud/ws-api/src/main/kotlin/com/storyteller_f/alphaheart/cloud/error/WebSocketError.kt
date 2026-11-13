/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the WebSocket operation boundary. */
sealed interface WebSocketError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        WebSocketError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : WebSocketError

    companion object {
        fun fromError(error: FailureDetails): WebSocketError = if (error is WebSocketError) error else from(error.cause)

        fun from(cause: Throwable): WebSocketError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
