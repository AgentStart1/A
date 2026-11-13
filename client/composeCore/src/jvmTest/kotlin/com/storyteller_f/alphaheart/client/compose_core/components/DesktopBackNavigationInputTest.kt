/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(InternalComposeUiApi::class)
class DesktopBackNavigationInputTest {
    @Test
    fun `escape dismisses overlay before navigation`() {
        val dispatcher = NavigationEventDispatcher()
        val input = DesktopBackNavigationInput()
        val navigation = CountingBackHandler()
        val dialog = CountingBackHandler()
        dispatcher.addHandler(navigation)
        dispatcher.addHandler(dialog, NavigationEventDispatcher.PRIORITY_OVERLAY)
        dispatcher.addInput(input)
        try {
            assertTrue(input.onKeyEvent(KeyEvent(Key.Escape, KeyEventType.KeyDown)))
            assertEquals(1, dialog.calls)
            assertEquals(0, navigation.calls)
            assertFalse(input.onKeyEvent(KeyEvent(Key.Escape, KeyEventType.KeyUp)))
            assertFalse(input.onKeyEvent(KeyEvent(Key.Enter, KeyEventType.KeyDown)))
            assertEquals(1, dialog.calls)
        } finally {
            dispatcher.dispose()
        }
    }

    @Test
    fun `disabled root navigation stays at root`() {
        val dispatcher = NavigationEventDispatcher()
        val input = DesktopBackNavigationInput()
        val navigation = CountingBackHandler().apply { isBackEnabled = false }
        dispatcher.addHandler(navigation)
        dispatcher.addInput(input)
        try {
            assertTrue(input.onKeyEvent(KeyEvent(Key.Escape, KeyEventType.KeyDown)))
            assertEquals(0, navigation.calls)
            dispatcher.removeInput(input)
            assertFalse(input.onKeyEvent(KeyEvent(Key.Escape, KeyEventType.KeyDown)))
        } finally {
            dispatcher.dispose()
        }
    }
}

private class CountingBackHandler : NavigationEventHandler<NavigationEventInfo>(NavigationEventInfo.None, true) {
    var calls = 0
        private set

    override fun onBackCompleted() {
        calls++
    }
}
