/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.file

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalRefCellHandlerProvider
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.createFileRefsViewModel
import com.storyteller_f.alphaheart.app.components.ActionMenuReference
import com.storyteller_f.alphaheart.app.components.RefCellStateView
import com.storyteller_f.alphaheart.app.file_refs
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.ReadingRecordList
import com.storyteller_f.alphaheart.app.pages.community.CommunityIconWithDialog
import com.storyteller_f.alphaheart.app.pages.room.RoomIconWithDialog
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.app.pages.user.UserIconWithDialog
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
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
            ReadingRecordList(
                modifier = Modifier.padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 20.dp),
                itemSpacing = 0.dp,
            ) {
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
    val rowPadding = PaddingValues(20.dp)
    when (type) {
        ObjectType.COMMUNITY ->
            RefCellStateView(handlers.communityHandler(id), Modifier.fillMaxWidth()) {
                val dialog = rememberCommonDialogController()
                val shown by dialog.shown
                ActionMenuReference(it.name, it.aid, { nav.newAppNav().gotoCommunity(it.id, false) }, rowPadding) {
                    CommunityIconWithDialog(it, shown, onClickIcon = dialog::update)
                }
            }

        ObjectType.ROOM ->
            RefCellStateView(handlers.roomHandler(id), Modifier.fillMaxWidth()) {
                val dialog = rememberCommonDialogController()
                val shown by dialog.shown
                ActionMenuReference(it.name, it.aid, { nav.newAppNav().gotoRoom(it.id, false) }, rowPadding) {
                    RoomIconWithDialog(it, shown, width = 48.dp, updateDialog = dialog::update)
                }
            }

        ObjectType.TOPIC ->
            RefCellStateView(handlers.topicHandler(id), Modifier.fillMaxWidth()) {
                TopicCell(it)
            }

        ObjectType.USER ->
            RefCellStateView(handlers.userHandler(id), Modifier.fillMaxWidth()) {
                ActionMenuReference(it.nickname, it.aid.orEmpty(), { nav.newAppNav().gotoUser(it.id) }, rowPadding) {
                    UserIconWithDialog(it, size = 34.dp)
                }
            }

        else -> Unit
    }
}
