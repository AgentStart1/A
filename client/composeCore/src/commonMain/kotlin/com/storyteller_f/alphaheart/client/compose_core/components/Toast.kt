/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.compositionLocalOf
import com.dokar.sonner.ToasterState
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlin.time.Duration.Companion.seconds

interface Toast {
    fun showMessage(message: String)

    companion object {
        val EMPTY =
            object : Toast {
                override fun showMessage(message: String) = Unit
            }
    }
}

class Sonner(val toasterState: ToasterState) : Toast {
    override fun showMessage(message: String) {
        toasterState.show(message, duration = 1.seconds)
    }
}

@OptIn(DelicateCoroutinesApi::class)
val LocalToaster =
    compositionLocalOf {
        Toast.EMPTY
    }
