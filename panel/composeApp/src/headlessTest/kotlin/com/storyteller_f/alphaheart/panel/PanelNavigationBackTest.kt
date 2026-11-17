/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.panel.common.PanelAllTopicsScreen
import com.storyteller_f.alphaheart.panel.common.PanelAllUsersScreen
import com.storyteller_f.alphaheart.panel.common.PanelOverviewScreen
import com.storyteller_f.alphaheart.panel.common.PanelTopicDetailScreen
import com.storyteller_f.alphaheart.panel.common.gotoChildTopicDetail
import com.storyteller_f.alphaheart.panel.common.newPanelNav
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PanelNavigationBackTest {
    @Test
    fun retainOverviewWhenBackingFromRoot() =
        runTest {
            val stack = NavBackStack<NavKey>(PanelOverviewScreen)
            val nav = newPanelNav(stack, DrawerState(DrawerValue.Closed), this)
            repeat(2) { nav.back() }
            assertEquals(listOf(PanelOverviewScreen), stack.toList())
        }

    @Test
    fun removePageWhenBacking() =
        runTest {
            val stack = NavBackStack<NavKey>(PanelOverviewScreen, PanelAllUsersScreen)
            val nav = newPanelNav(stack, DrawerState(DrawerValue.Closed), this)
            nav.back()
            nav.back()
            assertEquals(listOf(PanelOverviewScreen), stack.toList())
        }

    @Test
    fun childTopicNavigationRetainsParentsUntilBack() =
        runTest {
            val parent = PanelTopicDetailScreen(37)
            val child = PanelTopicDetailScreen(4)
            val grandchild = PanelTopicDetailScreen(91)
            val stack = NavBackStack<NavKey>(PanelAllTopicsScreen, parent)
            val nav = newPanelNav(stack, DrawerState(DrawerValue.Closed), this)
            nav.gotoChildTopicDetail(child.id)
            nav.gotoChildTopicDetail(grandchild.id)
            assertEquals(listOf(PanelAllTopicsScreen, parent, child, grandchild), stack.toList())
            nav.back()
            assertEquals(listOf(PanelAllTopicsScreen, parent, child), stack.toList())
            nav.back()
            assertEquals(listOf(PanelAllTopicsScreen, parent), stack.toList())
        }

    @Test
    fun selectingDirectoryTopicStillReplacesCurrentDetail() =
        runTest {
            val stack = NavBackStack<NavKey>(PanelAllTopicsScreen, PanelTopicDetailScreen(37))
            val nav = newPanelNav(stack, DrawerState(DrawerValue.Closed), this)
            nav.gotoTopicDetail(4)
            assertEquals(listOf(PanelAllTopicsScreen, PanelTopicDetailScreen(4)), stack.toList())
        }
}
