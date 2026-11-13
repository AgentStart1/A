/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import kotlin.coroutines.cancellation.CancellationException

/** Errors at the native font boundary, before OpenPDF document creation. */
internal sealed interface FontError : FailureDetails {
    data class InvalidFont(override val cause: Throwable) : FontError
    data class NativeUnavailable(override val cause: Throwable) : FontError
    data class InstancingFailed(override val cause: Throwable) : FontError
}

internal inline fun <T> captureFontFailure(block: () -> T): CustomResult<T, FontError> =
    try {
        CustomResult.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: IllegalArgumentException) {
        CustomResult.failure(FontError.InvalidFont(error))
    } catch (error: LinkageError) {
        CustomResult.failure(FontError.NativeUnavailable(error))
    } catch (error: Exception) {
        CustomResult.failure(FontError.InstancingFailed(error))
    }
