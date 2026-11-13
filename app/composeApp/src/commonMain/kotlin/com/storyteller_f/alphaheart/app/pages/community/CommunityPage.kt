/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.all_members
import com.storyteller_f.alphaheart.app.common.AppNavFactory
import com.storyteller_f.alphaheart.app.common.CommunityScreen
import com.storyteller_f.alphaheart.app.common.CommunityViewModel
import com.storyteller_f.alphaheart.app.common.OnCommunityExited
import com.storyteller_f.alphaheart.app.common.OnCommunityJoined
import com.storyteller_f.alphaheart.app.common.createCommunityRoomsViewModel
import com.storyteller_f.alphaheart.app.common.createCommunityTopicsViewModel
import com.storyteller_f.alphaheart.app.common.createCommunityViewModel
import com.storyteller_f.alphaheart.app.common.hasRouteFlow
import com.storyteller_f.alphaheart.app.components.FontView
import com.storyteller_f.alphaheart.app.exit_community
import com.storyteller_f.alphaheart.app.join_community
import com.storyteller_f.alphaheart.app.pages.AdaptiveSectionScaffold
import com.storyteller_f.alphaheart.app.pages.room.RoomList
import com.storyteller_f.alphaheart.app.pages.search.SearchScope
import com.storyteller_f.alphaheart.app.pages.search.detailSearchBar
import com.storyteller_f.alphaheart.app.pages.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.pages.topic.TopicList
import com.storyteller_f.alphaheart.app.pages.user.ButtonBadgeSuffix
import com.storyteller_f.alphaheart.app.rooms
import com.storyteller_f.alphaheart.app.topics
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.CommunityIcon
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.FavoriteButton
import com.storyteller_f.alphaheart.client.compose_core.components.IconRes
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.components.SubscriptionButton
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.core.exitCommunity
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun CommunityPage(communityId: PrimaryKey, showDialog: Boolean) {
    val model = createCommunityViewModel(communityId)
    CommunityPageInternal(communityId, showDialog, model)
}

@Composable
private fun CommunityPageInternal(communityId: PrimaryKey, needShowDialog: Boolean, model: CommunityViewModel) {
    val community by model.handler.data.collectAsState()
    val dialogShown by model.dialog.dialogShown.collectAsState()
    val pagerState = rememberPagerState { 2 }
    var showDialog by remember { mutableStateOf(false) }
    LaunchedEffect(needShowDialog, dialogShown) {
        if (needShowDialog && !dialogShown) {
            model.dialog.markDialogShown()
            showDialog = true
        }
    }
    AdaptiveSectionScaffold(pagerState, communityNavRoutes(), search = { page, _ ->
        val searchScope =
            if (page == 0) {
                SearchScope.CommunityTopic(
                    communityId,
                )
            } else {
                SearchScope.CommunityRoom(communityId)
            }
        detailSearchBar(searchScope) {
            CommunityIconWithDialog(community, showDialog) { showDialog = it }
        }
    }) { page ->
        if (page == 0) {
            TopicList(createCommunityTopicsViewModel(communityId))
        } else {
            RoomList(createCommunityRoomsViewModel(communityId))
        }
    }
}

@Composable
fun communityNavRoutes(): List<NavRoute> =
    listOf(
        NavRoute("/topics", Icons.Default.Topic, stringResource(Res.string.topics)),
        NavRoute("/rooms", Icons.Default.ChatBubble, stringResource(Res.string.rooms)),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDialog(communityInfo: CommunityInfo?, showDialog: Boolean, dismiss: () -> Unit) {
    if (communityInfo != null && showDialog) {
        BasicAlertDialog(
            {
                dismiss()
            },
        ) {
            CommunityDialogInternal(communityInfo, dismiss)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDialogInternal(communityInfo: CommunityInfo, dismiss: () -> Unit) {
    val communityId = communityInfo.id
    DialogContainer {
        Row(
            modifier =
            Modifier.background(MaterialTheme.colorScheme.surfaceDim, RoundedCornerShape(8.dp))
                .padding(8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CommunityIconWithDialog(
                communityInfo,
                showDialog = false,
                50.dp,
                setClickEvent = false,
            ) {}
            Column {
                Text(communityInfo.name)
            }
        }
        communityInfo.fontSettings?.contentFont?.let {
            FontView(it)
        }
        communityInfo.fontSettings?.codeFont?.let {
            FontView(it)
        }
        CommunityMenus(communityId, communityInfo, dismiss)
    }
}

@Composable
private fun CommunityMenus(communityId: PrimaryKey, communityInfo: CommunityInfo, dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    val globalDialogController = LocalGlobalDialog.current
    Column {
        ButtonNav(
            IconRes.Vector(Icons.Default.CardMembership),
            stringResource(Res.string.all_members),
            {
                ButtonBadgeSuffix(communityInfo.memberCount)
            },
            semanticDescription = "all-members-action",
        ) {
            dismiss()
            appNavFactory.newAppNav().gotoMemberPage(communityId, ObjectType.COMMUNITY)
        }
        val appNavFactory = LocalAppNavFactory.current
        val isCommunityPage = appNavFactory.hasRouteFlow<CommunityScreen>()
        if (isCommunityPage) {
            CommunityMemberStatusButton(communityInfo, globalDialogController, communityId)
            FavoriteButton(communityInfo.favoriteId, communityInfo.tuple())
            SubscriptionButton(communityInfo.subscriptionId, communityInfo.tuple())
            CommunityCreateButton(dismiss, appNavFactory, communityId)
            CommunityAdminButtons(communityInfo, dismiss, appNavFactory, communityId)
        }
    }
}

@Composable
private fun CommunityAdminButtons(
    communityInfo: CommunityInfo,
    dismiss: () -> Unit,
    appNavFactory: AppNavFactory,
    communityId: PrimaryKey,
) {
    val myInfo = LocalUserInfo.current
    if (myInfo?.id == communityInfo.owner) {
        ButtonNav(Icons.Default.Folder, "Files") {
            dismiss()
            appNavFactory.newAppNav().gotoFileExplorer(communityId ob ObjectType.COMMUNITY)
        }

        ButtonNav(Icons.Default.Title, "Add Title") {
            dismiss()
            appNavFactory.newAppNav().gotoTitleCompose()
        }

        ButtonNav(Icons.Default.Settings, "Settings") {
            dismiss()
            appNavFactory.newAppNav().gotoSettingPage(communityId, ObjectType.COMMUNITY)
        }
    }
}

@Composable
private fun CommunityCreateButton(dismiss: () -> Unit, appNavFactory: AppNavFactory, communityId: PrimaryKey) {
    ButtonNav(Icons.Default.Add, "Add", semanticDescription = "community-add-topic-action") {
        dismiss()
        appNavFactory.newAppNav().gotoTopicCompose(
            TopicComposeData.Community(communityId, communityId ob ObjectType.COMMUNITY),
        )
    }
}

@Composable
private fun CommunityMemberStatusButton(
    communityInfo: CommunityInfo,
    globalDialogController: AppGlobalDialogController,
    communityId: PrimaryKey,
) {
    val scope = rememberCoroutineScope()
    if (communityInfo.isJoined) {
        ButtonNav(Icons.Default.Close, stringResource(Res.string.exit_community)) {
            scope.launch {
                globalDialogController.useResult {
                    request {
                        exitCommunity(communityId)
                    }
                }.onSuccess { info ->
                    globalDialogController.emitEvent(OnCommunityExited(info))
                }
            }
        }
    } else {
        ButtonNav(Icons.Default.AddHome, stringResource(Res.string.join_community)) {
            scope.launch {
                globalDialogController.useResult {
                    request {
                        joinCommunity(communityId)
                    }
                }.onSuccess { info ->
                    globalDialogController.emitEvent(OnCommunityJoined(info))
                }
            }
        }
    }
}

@Composable
fun CommunityIconWithDialog(
    communityInfo: CommunityInfo?,
    showDialog: Boolean,
    iconSize: Dp = 40.dp,
    setClickEvent: Boolean = true,
    onClickIcon: (Boolean) -> Unit,
) {
    CommunityIcon(communityInfo, iconSize, setClickEvent, onClickIcon)
    CommunityDialog(communityInfo, showDialog) {
        onClickIcon(false)
    }
}
