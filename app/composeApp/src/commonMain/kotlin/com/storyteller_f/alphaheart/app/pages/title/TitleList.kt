/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.title

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.itemKey
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.TitlesViewModel
import com.storyteller_f.alphaheart.app.pages.community.CommunityIconWithDialog
import com.storyteller_f.alphaheart.app.pages.room.RoomIconWithDialog
import com.storyteller_f.alphaheart.app.pages.user.UserIconWithDialog
import com.storyteller_f.alphaheart.app.title_expired
import com.storyteller_f.alphaheart.app.title_granted_by
import com.storyteller_f.alphaheart.app.title_permanent
import com.storyteller_f.alphaheart.app.title_received_by
import com.storyteller_f.alphaheart.app.title_scope
import com.storyteller_f.alphaheart.app.title_valid
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.rememberCommonDialogController
import com.storyteller_f.alphaheart.client.compose_core.paging.bottomAppending
import com.storyteller_f.alphaheart.client.compose_core.paging.topPrepend
import com.storyteller_f.alphaheart.client.compose_core.theme.LayoutDefaults
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TitleWorkStatus
import com.storyteller_f.shared.type.ObjectType
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@Composable
fun TitleList(titlesViewModel: TitlesViewModel) {
    StateView(titlesViewModel) { pagingItems ->
        LazyColumn(
            modifier = Modifier.widthIn(max = 740.dp).fillMaxHeight(),
            contentPadding = LayoutDefaults.contentPadding,
            verticalArrangement = LayoutDefaults.pagingVerticalArrangement,
        ) {
            topPrepend(pagingItems.loadState, pagingItems::retry)
            items(
                count = pagingItems.itemCount,
                key =
                pagingItems.itemKey {
                    it.id.toString()
                },
            ) { index ->
                val title = pagingItems[index]
                title?.let {
                    TitleItem(it)
                }
            }
            bottomAppending(pagingItems.loadState, pagingItems::retry)
        }
    }
}

@Composable
fun TitleItem(titleInfo: TitleDetail) {
    val status =
        stringResource(
            if (titleInfo.titleStatus == TitleWorkStatus.EXPIRED) {
                Res.string.title_expired
            } else if (titleInfo.expiresAt == null) {
                Res.string.title_permanent
            } else {
                Res.string.title_valid
            },
        )
    Surface(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(titleInfo.name, style = MaterialTheme.typography.titleMedium)
                    titleInfo.expiresAt?.let {
                        Text(
                            it.formatDisplayTime(),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        status,
                        Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            TitleParticipants(titleInfo)
            TitleScope(titleInfo)
        }
    }
}

@Composable
private fun TitleParticipants(titleInfo: TitleDetail) {
    val nav = LocalAppNavFactory.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.title_granted_by), style = MaterialTheme.typography.labelMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UserIconWithDialog(titleInfo.creatorInfo)
                Text(
                    titleInfo.creatorInfo.nickname,
                    Modifier.clickable {
                        nav.newAppNav().gotoUser(titleInfo.creatorInfo.id)
                    },
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.title_received_by), style = MaterialTheme.typography.labelMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UserIconWithDialog(titleInfo.receiverInfo)
                Text(
                    titleInfo.receiverInfo.nickname,
                    Modifier.clickable {
                        nav.newAppNav().gotoUser(titleInfo.receiverInfo.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun TitleScope(titleInfo: TitleDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.title_scope), style = MaterialTheme.typography.labelMedium)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val dialog = rememberCommonDialogController()
            when (titleInfo.scopeType) {
                ObjectType.COMMUNITY -> {
                    CommunityIconWithDialog(
                        titleInfo.communityInfo,
                        dialog.shown.value,
                        onClickIcon = dialog::update,
                    )
                    Text(titleInfo.communityInfo?.name.orEmpty())
                }

                ObjectType.ROOM -> {
                    RoomIconWithDialog(titleInfo.roomInfo, dialog.shown.value, updateDialog = dialog::update)
                    Text(titleInfo.roomInfo?.name.orEmpty())
                }

                ObjectType.USER -> UserIconWithDialog(titleInfo.receiverInfo)

                else -> Unit
            }
        }
    }
}
