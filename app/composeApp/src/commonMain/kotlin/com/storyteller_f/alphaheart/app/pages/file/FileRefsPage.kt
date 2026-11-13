/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.createFileRefsViewModel
import com.storyteller_f.alphaheart.app.file_refs
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileRefsPage(fileId: PrimaryKey) {
    val viewModel = createFileRefsViewModel(fileId)
    val appNavFactory = LocalAppNavFactory.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.file_refs)) },
                navigationIcon = {
                    IconButton(onClick = { appNavFactory.newAppNav().back() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Box(Modifier.safeArea(paddingValues, direction)) {
            StateView(viewModel, modifier = Modifier.fillMaxSize()) { items ->
                LazyColumn {
                    pagingItems(items, key = { it.id }) { index ->
                        val ref = items[index]
                        if (ref != null) {
                            ListItem(
                                modifier =
                                Modifier.clickable {
                                    when (ref.objectType) {
                                        ObjectType.COMMUNITY ->
                                            appNavFactory.newAppNav()
                                                .gotoCommunity(ref.objectId, false)

                                        ObjectType.ROOM ->
                                            appNavFactory.newAppNav()
                                                .gotoRoom(ref.objectId, false)

                                        ObjectType.TOPIC ->
                                            appNavFactory.newAppNav()
                                                .gotoTopic(ref.objectId)

                                        ObjectType.USER ->
                                            appNavFactory.newAppNav()
                                                .gotoUser(ref.objectId)

                                        else -> {}
                                    }
                                },
                                headlineContent = {
                                    Text("${ref.objectType.name} #${ref.objectId}")
                                },
                                supportingContent = {
                                    Text("Author: ${ref.author}")
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}
