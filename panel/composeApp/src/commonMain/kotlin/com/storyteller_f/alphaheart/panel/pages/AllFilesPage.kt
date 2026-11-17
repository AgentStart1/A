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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.FileIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.all_files
import com.storyteller_f.alphaheart.panel.common.AllFilesViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelAllFilesViewModel
import com.storyteller_f.alphaheart.panel.components.panelDirectoryRowColors
import com.storyteller_f.alphaheart.panel.components.panelDirectorySelection
import com.storyteller_f.alphaheart.panel.components.selectedPanelEntityId
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.shared.type.ObjectType
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFilesPage() {
    val viewModel = createPanelAllFilesViewModel()
    AllFilesPageInternal(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFilesPageInternal(viewModel: AllFilesViewModel) {
    val panelNav = LocalPanelNav.current
    val selectedId = selectedPanelEntityId(panelNav.backStack.lastOrNull(), ObjectType.FILE)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.all_files)) },
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
                            val name = info.name
                            val type = info.contentType
                            val size = HumanReadable.fileSize(info.size)
                            val dim =
                                info.dimension?.takeIf { it.width > 0 && it.height > 0 }
                                    ?.let { "${it.width}x${it.height}" }.orEmpty()
                            val rowDescription =
                                listOf(name, type, size, dim).filter { it.isNotEmpty() }.joinToString(", ")
                            ListItem(
                                colors = panelDirectoryRowColors(info.id == selectedId),
                                modifier =
                                Modifier.panelDirectorySelection(info.id == selectedId)
                                    .clickable { panelNav.gotoFileDetail(info.id) }
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = rowDescription
                                    }
                                    .appiumSemantics(text = name, onClick = { panelNav.gotoFileDetail(info.id) }),
                                leadingContent = { FileIcon(info) },
                                headlineContent = { Text(name) },
                                overlineContent = { Text(type) },
                                supportingContent = {
                                    Text(
                                        listOf(size, dim).filter { it.isNotEmpty() }.joinToString(" • "),
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
