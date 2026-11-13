/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.RoomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.all_private_rooms
import com.storyteller_f.alphaheart.panel.common.AllPrivateRoomsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelAllPrivateRoomsViewModel
import com.storyteller_f.alphaheart.panel.room_private
import org.jetbrains.compose.resources.stringResource

@Composable
fun AllPrivateRoomsPage() {
    val viewModel = createPanelAllPrivateRoomsViewModel()
    AllPrivateRoomsPageInternal(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPrivateRoomsPageInternal(viewModel: AllPrivateRoomsViewModel) {
    val panelNav = LocalPanelNav.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.all_private_rooms)) },
                navigationIcon = { IconButton({ panelNav.open() }) { Icon(Icons.Default.Menu, null) } },
            )
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Box(Modifier.safeArea(paddingValues, direction)) {
            StateView(viewModel) { items ->
                LazyColumn {
                    pagingItems(items, key = { it.id }) { index ->
                        val info = items[index]
                        if (info != null) {
                            val members = info.memberCount.toString()
                            val aid = info.aid
                            ListItem(
                                modifier = Modifier.clickable { panelNav.gotoRoomDetail(info.id) },
                                leadingContent = {
                                    RoomIcon(info, 40.dp, false) {}
                                },
                                headlineContent = { Text(info.name) },
                                overlineContent = { Text(aid) },
                                supportingContent = {
                                    Text(
                                        listOf(
                                            members,
                                            stringResource(Res.string.room_private),
                                        ).filter { it.isNotEmpty() }.joinToString(" • "),
                                    )
                                },
                            )
                            HorizontalDivider()
                        } else {
                            ListItem(headlineContent = { Text("") })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
