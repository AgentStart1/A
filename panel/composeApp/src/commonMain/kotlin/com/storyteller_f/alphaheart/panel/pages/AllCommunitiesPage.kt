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
import com.storyteller_f.alphaheart.client.compose_core.components.CommunityIcon
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.all_communities
import com.storyteller_f.alphaheart.panel.common.AllCommunitiesViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelAllCommunitiesViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun AllCommunitiesPage() {
    val viewModel = createPanelAllCommunitiesViewModel()
    AllCommunitiesPageInternal(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCommunitiesPageInternal(viewModel: AllCommunitiesViewModel) {
    val panelNav = LocalPanelNav.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.all_communities)) },
                navigationIcon = {
                    IconButton({ panelNav.open() }) { Icon(Icons.Default.Menu, null) }
                },
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
                            ListItem(
                                modifier = Modifier.clickable { panelNav.gotoCommunityDetail(info.id) },
                                leadingContent = {
                                    CommunityIcon(info, 40.dp, false) {}
                                },
                                headlineContent = { Text(info.name) },
                                overlineContent = { Text(info.aid) },
                                supportingContent = {
                                    val owner = info.owner.toString()
                                    val members = info.memberCount.toString()
                                    val policy = info.memberPolicy.name
                                    Text(
                                        listOf(owner, members, policy).filter { it.isNotEmpty() }
                                            .joinToString(" • "),
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
