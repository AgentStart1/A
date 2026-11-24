/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.notifications

import androidx.compose.runtime.compositionLocalOf
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.time.Duration.Companion.seconds

interface Toast {
    fun showMessage(message: String)

    fun showSuccess(message: String) = showMessage(message)

    fun showError(message: String) = showMessage(message)

    companion object {
        val EMPTY =
            object : Toast {
                override fun showMessage(message: String) = Unit
            }
    }
}

class Sonner(val toasterState: ToasterState) : Toast {
    override fun showMessage(message: String) = show(message, ToastType.Info)

    override fun showSuccess(message: String) = show(message, ToastType.Success)

    override fun showError(message: String) = show(message, ToastType.Error)

    private fun show(message: String, type: ToastType) {
        toasterState.show(message, type = type, duration = 1.seconds)
    }
}

@OptIn(DelicateCoroutinesApi::class)
val LocalToaster =
    compositionLocalOf {
        Toast.EMPTY
    }
