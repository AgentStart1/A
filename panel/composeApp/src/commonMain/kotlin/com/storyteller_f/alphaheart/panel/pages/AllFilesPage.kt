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
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource

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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.all_files)) },
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
                            val name = info.name
                            val type = info.contentType
                            val size = HumanReadable.fileSize(info.size)
                            val dim = info.dimension?.let { "${it.width}x${it.height}" }.orEmpty()
                            ListItem(
                                modifier =
                                Modifier.clickable { panelNav.gotoFileDetail(info.id) }
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
