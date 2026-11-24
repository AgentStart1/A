/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.navigation

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInput
import com.github.panpf.zoomimage.util.setMainThreadChecker

/** Supplies the same Escape-to-navigation binding as Compose's AWT window mediator. */
internal class DesktopBackNavigationInput : NavigationEventInput() {
    internal var attached = false

    override fun onAdded(dispatcher: NavigationEventDispatcher) {
        attached = true
    }

    override fun onRemoved() {
        attached = false
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!attached || event.type != KeyEventType.KeyDown || event.key != Key.Escape) return false
        dispatchOnBackCompleted()
        return true
    }
}

/** ZoomImage defaults to Swing; the native window owns its own UI event thread. */
internal fun bindDesktopImageThread(owner: Thread) {
    setMainThreadChecker { Thread.currentThread() === owner }
}
