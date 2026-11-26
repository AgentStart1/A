/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalRefCellHandlerProvider
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.UserReactionRecordsViewModel
import com.storyteller_f.alphaheart.app.common.getUserReactionRecordsViewModel
import com.storyteller_f.alphaheart.app.my_reactions
import com.storyteller_f.alphaheart.app.navigation.recordListFooter
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.ReadingRecordList
import com.storyteller_f.alphaheart.app.pages.referenceTypeLabel
import com.storyteller_f.alphaheart.app.pages.topic.TopicCell
import com.storyteller_f.alphaheart.app.record_reactions_empty
import com.storyteller_f.alphaheart.app.record_reactions_end
import com.storyteller_f.alphaheart.app.record_reactions_failure
import com.storyteller_f.alphaheart.app.record_reactions_loading
import com.storyteller_f.alphaheart.app.reference_content_unavailable
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import com.storyteller_f.shared.model.ReactionRecordInfo
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.stringResource

@Composable
fun UserReactionRecordsPage() {
    val viewModel = getUserReactionRecordsViewModel()
    UserReactionRecordsPageInternal(viewModel)
}

@Composable
fun UserReactionRecordsPageInternal(viewModel: UserReactionRecordsViewModel) {
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.my_reactions)) }) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        StateView(
            viewModel,
            modifier = Modifier.safeArea(paddingValues, layoutDirection),
            emptyText = stringResource(Res.string.record_reactions_empty),
            loadingText = stringResource(Res.string.record_reactions_loading),
            failureText = stringResource(Res.string.record_reactions_failure),
        ) { items ->
            val topics by viewModel.topicPreviews.collectAsStateWithLifecycle()
            val endLabel = stringResource(Res.string.record_reactions_end)
            ReadingRecordList {
                pagingItems(items, {
                    it.id
                }) {
                    val record = items[it]
                    UserReactionRecordCell(record, record?.let { item -> topics[item.objectId] })
                    if (it < items.itemCount - 1) HorizontalDivider()
                }
                recordListFooter(items, endLabel)
            }
        }
    }
}

@Composable
private fun UserReactionRecordCell(reactionRecordInfo: ReactionRecordInfo?, topic: TopicDetail?) {
    val record = reactionRecordInfo ?: return
    if (record.objectType == ObjectType.TOPIC) {
        if (topic == null) {
            Text(stringResource(Res.string.reference_content_unavailable), Modifier.padding(vertical = 20.dp))
        } else {
            TopicCell(topic)
        }
        return
    }
    val nav = LocalAppNavFactory.current
    val open = {
        when (record.objectType) {
            ObjectType.COMMUNITY -> nav.newAppNav().gotoCommunity(record.objectId, false)
            ObjectType.ROOM -> nav.newAppNav().gotoRoom(record.objectId, false)
            ObjectType.USER -> nav.newAppNav().gotoUser(record.objectId)
            else -> Unit
        }
    }
    val canOpen = record.objectType in setOf(ObjectType.COMMUNITY, ObjectType.ROOM, ObjectType.USER)
    Row(
        Modifier.fillMaxWidth().clickable(enabled = canOpen, onClick = open).padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(record.emoji, Modifier.padding(end = 14.dp))
        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
            ReactionReferenceCaption(record)
        }
        if (canOpen) {
            com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon(
                com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon(
                    com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph.CHEVRON_RIGHT,
                ),
                null,
            )
        }
    }
}

@Composable
private fun ReactionReferenceCaption(record: ReactionRecordInfo) {
    val provider = LocalRefCellHandlerProvider.current
    val name =
        when (record.objectType) {
            ObjectType.COMMUNITY -> {
                val info by provider.communityHandler(record.objectId).data.collectAsStateWithLifecycle()
                info?.name
            }

            ObjectType.ROOM -> {
                val info by provider.roomHandler(record.objectId).data.collectAsStateWithLifecycle()
                info?.name
            }

            ObjectType.USER -> {
                val info by provider.userHandler(record.objectId).data.collectAsStateWithLifecycle()
                info?.nickname
            }

            else -> record.objectId.toString()
        }
    Text(
        "${referenceTypeLabel(
            record.objectType,
        )} · ${name ?: stringResource(Res.string.reference_content_unavailable)}",
    )
}
