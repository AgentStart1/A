/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.error

import com.storyteller_f.result.FailureDetails
import kotlin.coroutines.cancellation.CancellationException

/** Dialog admission and presentation failures, independent of the requested operation's error domain. */
sealed interface DialogError : FailureDetails {
    data class Busy(override val cause: Throwable = IllegalStateException("dialog show failed")) : DialogError
    data class OperationFailed(override val cause: Throwable) : DialogError

    companion object {
        fun fromError(error: FailureDetails): DialogError = if (error is DialogError) error else from(error.cause)

        fun from(cause: Throwable): DialogError {
            if (cause is CancellationException) throw cause
            return OperationFailed(cause)
        }
    }
}
