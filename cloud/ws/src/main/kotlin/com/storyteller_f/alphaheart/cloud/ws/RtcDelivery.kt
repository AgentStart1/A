/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.ws

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.supervisorScope

/** Gives recipient delivery its own lifetime while retaining caller cancellation. */
internal suspend fun sendRtcFrame(send: suspend () -> Unit) =
    supervisorScope {
        ensureActive()
        val completion = CompletableDeferred<Throwable?>()
        val delivery = async { send() }
        delivery.invokeOnCompletion { completion.complete(it) }
        delivery.join()
        ensureActive()
        when (val failure = completion.await()) {
            null -> Unit
            is CancellationException -> throw IllegalStateException("RTC recipient connection closed", failure)
            else -> throw failure
        }
    }
