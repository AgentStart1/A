/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.pdf

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Pdf operation boundary. */
sealed interface PdfError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        PdfError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : PdfError

    companion object {
        fun fromError(error: FailureDetails): PdfError = if (error is PdfError) error else from(error.cause)

        fun from(cause: Throwable): PdfError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
