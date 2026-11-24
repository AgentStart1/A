/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalRefCellHandlerProvider
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.createFileRefsViewModel
import com.storyteller_f.alphaheart.app.components.RefCellStateView
import com.storyteller_f.alphaheart.app.file_refs
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.ReadingRecordList
import com.storyteller_f.alphaheart.app.pages.community.CommunityCell
import com.storyteller_f.alphaheart.app.pages.room.PrimaryRoomCell
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.app.pages.user.UserCell
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import org.jetbrains.compose.resources.stringResource

@Composable
fun FileRefsPage(fileId: PrimaryKey) {
    val viewModel = createFileRefsViewModel(fileId)
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.file_refs)) }) { padding ->
        StateView(viewModel, modifier = Modifier.safeArea(padding, LocalLayoutDirection.current)) { items ->
            ReadingRecordList {
                pagingItems(items, key = { it.id }) { index ->
                    val ref = items[index]
                    if (ref != null) {
                        FileReferenceRow(ref.objectType, ref.objectId)
                        if (index < items.itemCount - 1) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FileReferenceRow(type: ObjectType, id: PrimaryKey) {
    val handlers = LocalRefCellHandlerProvider.current
    val nav = LocalAppNavFactory.current
    when (type) {
        ObjectType.COMMUNITY ->
            RefCellStateView(handlers.communityHandler(id), Modifier.fillMaxWidth()) {
                CommunityCell(it)
            }

        ObjectType.ROOM ->
            RefCellStateView(handlers.roomHandler(id), Modifier.fillMaxWidth()) {
                PrimaryRoomCell(it)
            }

        ObjectType.TOPIC ->
            RefCellStateView(handlers.topicHandler(id), Modifier.fillMaxWidth()) {
                TopicCell(it)
            }

        ObjectType.USER ->
            RefCellStateView(handlers.userHandler(id), Modifier.fillMaxWidth()) {
                UserCell(it) { user -> nav.newAppNav().gotoUser(user.id) }
            }

        else -> Unit
    }
}
