/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.panel

import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.launch

@Composable
internal fun PanelDrawerBackHandler(drawerState: DrawerState) {
    val scope = rememberCoroutineScope()
    // The modal drawer owns Back while its content dispatcher is disabled.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = drawerState.isOpen,
    ) { scope.launch { drawerState.close() } }
}
