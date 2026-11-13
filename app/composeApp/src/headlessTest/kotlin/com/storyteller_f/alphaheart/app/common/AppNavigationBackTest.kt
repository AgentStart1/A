/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals

class AppNavigationBackTest {
    @Test
    fun retainHomeWhenBackingFromRoot() {
        val backStack = NavBackStack<NavKey>(HomeScreen)
        val nav = newAppNav(backStack, isSignedIn = { false }, onSignInRequired = {})
        repeat(2) { nav.back() }
        assertEquals(listOf(HomeScreen), backStack.toList())
    }

    @Test
    fun removeDetailWhenBacking() {
        val backStack = NavBackStack<NavKey>(HomeScreen, RoomScreen(1))
        val nav = newAppNav(backStack, isSignedIn = { false }, onSignInRequired = {})
        nav.back()
        nav.back()
        assertEquals(listOf(HomeScreen), backStack.toList())
    }
}
