/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.RoomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.all_private_rooms
import com.storyteller_f.alphaheart.panel.common.AllPrivateRoomsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelAllPrivateRoomsViewModel
import com.storyteller_f.alphaheart.panel.components.panelDirectoryRowColors
import com.storyteller_f.alphaheart.panel.components.panelDirectorySelection
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.navigation.selectedPanelEntityId
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@Composable
fun AllPrivateRoomsPage() {
    val viewModel = createPanelAllPrivateRoomsViewModel()
    AllPrivateRoomsPageInternal(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPrivateRoomsPageInternal(viewModel: AllPrivateRoomsViewModel) {
    val panelNav = LocalPanelNav.current
    val selectedId = selectedPanelEntityId(panelNav.backStack.lastOrNull(), ObjectType.ROOM)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.all_private_rooms)) },
                navigationIcon = {
                    IconButton({ panelNav.open() }) {
                        Icon(Icons.Default.Menu, stringResource(Res.string.menu))
                    }
                },
            )
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Box(Modifier.safeArea(paddingValues, direction)) {
            StateView(viewModel) { items ->
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                    pagingItems(items, key = { it.id }) { index ->
                        val info = items[index]
                        if (info != null) {
                            ListItem(
                                colors = panelDirectoryRowColors(info.id == selectedId),
                                modifier =
                                Modifier.panelDirectorySelection(info.id == selectedId)
                                    .clickable { panelNav.gotoRoomDetail(info.id) },
                                leadingContent = {
                                    RoomIcon(info, 40.dp, false) {}
                                },
                                headlineContent = { Text(info.name) },
                                supportingContent = { Text(CoreStrings.aid(info.aid)) },
                            )
                            if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                        } else {
                            ListItem(headlineContent = { Text("") })
                            if (index < items.itemCount - 1) HorizontalDivider(Modifier.padding(vertical = 16.dp))
                        }
                    }
                }
            }
        }
    }
}
