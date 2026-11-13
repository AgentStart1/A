/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.app.common.CommunityScreen
import com.storyteller_f.alphaheart.app.common.HomeScreen
import com.storyteller_f.alphaheart.app.common.RoomScreen
import com.storyteller_f.alphaheart.app.common.TopicScreen
import com.storyteller_f.alphaheart.app.common.UserScreen
import com.storyteller_f.alphaheart.app.common.newAppNav
import com.storyteller_f.alphaheart.app.pages.HOME_START_DESTINATION_COMMUNITIES
import com.storyteller_f.alphaheart.app.pages.HOME_START_DESTINATION_ROOMS
import com.storyteller_f.alphaheart.app.pages.HOME_START_DESTINATION_WORLD
import com.storyteller_f.alphaheart.app.pages.HomeRoute
import com.storyteller_f.alphaheart.app.pages.homePageFromPreference
import com.storyteller_f.alphaheart.app.pages.homeRouteFromPreference
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeStartDestinationTest {
    @Test
    fun shouldMapPreferenceToHomeRoute() {
        assertEquals(HomeRoute.World, homeRouteFromPreference(HOME_START_DESTINATION_WORLD))
        assertEquals(HomeRoute.Communities, homeRouteFromPreference(HOME_START_DESTINATION_COMMUNITIES))
        assertEquals(HomeRoute.Rooms, homeRouteFromPreference(HOME_START_DESTINATION_ROOMS))
        assertEquals(HomeRoute.World, homeRouteFromPreference("unknown"))
    }

    @Test
    fun shouldMapPreferenceToHomePage() {
        assertEquals(0, homePageFromPreference(HOME_START_DESTINATION_WORLD))
        assertEquals(1, homePageFromPreference(HOME_START_DESTINATION_COMMUNITIES))
        assertEquals(2, homePageFromPreference(HOME_START_DESTINATION_ROOMS))
        assertEquals(0, homePageFromPreference("unknown"))
    }

    @Test
    fun homeNavigationPushesIndependentDestinations() {
        listOf<(com.storyteller_f.alphaheart.app.common.AppNav) -> Unit>(
            { it.gotoCommunity(2, false) },
            { it.gotoRoom(2, false) },
            { it.gotoTopic(2) },
            { it.gotoUser(2) },
        ).zip(listOf(CommunityScreen(2, false), RoomScreen(2, false), TopicScreen(2), UserScreen(2)))
            .forEach { (navigate, destination) ->
                val backStack = NavBackStack<NavKey>(HomeScreen)
                val nav = newAppNav(backStack, isSignedIn = { false }, onSignInRequired = {})
                navigate(nav)
                assertEquals(listOf(HomeScreen, destination), backStack.toList())
                nav.back()
                assertEquals(listOf(HomeScreen), backStack.toList())
            }
    }

    @Test
    fun detailNavigationPreservesBackHistory() {
        val backStack = NavBackStack<NavKey>(HomeScreen, RoomScreen(1))

        newAppNav(backStack, isSignedIn = { false }, onSignInRequired = {}).gotoTopic(2)

        assertEquals(
            listOf(HomeScreen, RoomScreen(1), TopicScreen(2)),
            backStack.toList(),
        )
    }

    @Test
    fun rootBackKeepsHome() {
        val backStack = NavBackStack<NavKey>(HomeScreen)
        newAppNav(backStack, isSignedIn = { false }, onSignInRequired = {}).back()
        assertEquals(listOf(HomeScreen), backStack.toList())
    }
}
