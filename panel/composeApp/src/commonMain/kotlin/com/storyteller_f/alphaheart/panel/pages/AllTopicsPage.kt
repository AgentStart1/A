/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.all_topics
import com.storyteller_f.alphaheart.panel.common.AllTopicsViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelAllTopicsViewModel
import com.storyteller_f.alphaheart.panel.components.TopicCell
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.navigation.selectedPanelEntityId
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.stringResource

@Composable
fun AllTopicsPage() {
    val viewModel = createPanelAllTopicsViewModel()
    AllTopicsPageInternal(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTopicsPageInternal(viewModel: AllTopicsViewModel) {
    val panelNav = LocalPanelNav.current
    val selectedId = selectedPanelEntityId(panelNav.backStack.lastOrNull(), ObjectType.TOPIC)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.all_topics)) },
                navigationIcon = {
                    IconButton({ panelNav.open() }) {
                        Icon(actionIcon(ActionGlyph.MENU), stringResource(Res.string.menu))
                    }
                },
            )
        },
    ) {
        val direction = LocalLayoutDirection.current
        Box(Modifier.safeArea(it, direction)) {
            StateView(viewModel) { items ->
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                    pagingItems(items, key = { it.id }) { index ->
                        val info = items[index]
                        if (info != null) {
                            val panelNav = LocalPanelNav.current
                            TopicCell(info, panelNav, selected = info.id == selectedId)
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
