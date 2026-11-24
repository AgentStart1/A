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
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.all_titles
import com.storyteller_f.alphaheart.panel.common.AllTitlesViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelAllTitlesViewModel
import com.storyteller_f.alphaheart.panel.components.panelDirectoryRowColors
import com.storyteller_f.alphaheart.panel.components.panelDirectorySelection
import com.storyteller_f.alphaheart.panel.components.panelValueText
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.navigation.selectedPanelEntityId
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTitlesPage() {
    val viewModel = createPanelAllTitlesViewModel()
    AllTitlesPageInternal(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTitlesPageInternal(viewModel: AllTitlesViewModel) {
    val panelNav = LocalPanelNav.current
    val selectedId = selectedPanelEntityId(panelNav.backStack.lastOrNull(), ObjectType.TITLE)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.all_titles)) },
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
                            val type = panelValueText(info.type.name)
                            val scope = panelValueText(info.scopeType.name)
                            val creator = info.creator.toString()
                            val receiver = info.receiver.toString()
                            ListItem(
                                colors = panelDirectoryRowColors(info.id == selectedId),
                                modifier =
                                Modifier.panelDirectorySelection(info.id == selectedId)
                                    .clickable { panelNav.gotoTitleDetail(info.id) },
                                headlineContent = { Text(info.name) },
                                overlineContent = { Text(type) },
                                supportingContent = {
                                    Text(
                                        listOf(scope, creator, receiver).filter { it.isNotEmpty() }.joinToString(" • "),
                                    )
                                },
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
