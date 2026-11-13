/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.platform.Clipboard
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.copied
import com.storyteller_f.alphaheart.client.compose_core.copy_failed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.jetbrains.compose.resources.getString

/** Writes text or throws; cancellation is propagated unchanged. */
expect suspend fun Clipboard.setText(string: String)

/** Reports the write outcome without cancelling the caller on ordinary clipboard failures. */
suspend fun Clipboard.copyText(text: String, toast: Toast) {
    val message =
        try {
            setText(text)
            currentCoroutineContext().ensureActive()
            Res.string.copied
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            currentCoroutineContext().ensureActive()
            Res.string.copy_failed
        }
    val feedback = getString(message)
    currentCoroutineContext().ensureActive()
    toast.showMessage(feedback)
}
