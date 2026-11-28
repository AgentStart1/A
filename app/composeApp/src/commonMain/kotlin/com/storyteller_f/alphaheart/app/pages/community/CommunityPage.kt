/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.storyteller_f.alphaheart.app.compose_topic
import com.storyteller_f.alphaheart.app.create_room_action
import com.storyteller_f.alphaheart.app.create_title_action
import com.storyteller_f.alphaheart.app.created_at
import com.storyteller_f.alphaheart.app.exit_community
import com.storyteller_f.alphaheart.app.files_title
import com.storyteller_f.alphaheart.app.join_community
import com.storyteller_f.alphaheart.app.pages.AdaptiveSectionScaffold
import com.storyteller_f.alphaheart.app.pages.room.RoomList
import com.storyteller_f.alphaheart.app.pages.search.detailSearchBar
import com.storyteller_f.alphaheart.app.pages.topic.TopicList
import com.storyteller_f.alphaheart.app.pages.user.ButtonBadgeSuffix
import com.storyteller_f.alphaheart.app.rooms
import com.storyteller_f.alphaheart.app.search.SearchScope
import com.storyteller_f.alphaheart.app.settings_title
import com.storyteller_f.alphaheart.app.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.topics
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.CommunityIcon
import com.storyteller_f.alphaheart.client.compose_core.components.FavoriteButton
import com.storyteller_f.alphaheart.client.compose_core.components.IconActionMenu
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAdaptiveOverlay
import com.storyteller_f.alphaheart.client.compose_core.components.SubscriptionButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.IconRes
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import com.storyteller_f.alphaheart.client.compose_core.utils.formatDisplayTime
import com.storyteller_f.alphaheart.client.core.exitCommunity
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.MemberStatus
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

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
    }, floatingActionButton = {
        if (pagerState.currentPage == 0) CommunityTopicFab(community)
    }) { page ->
        if (page == 0) {
            TopicList(createCommunityTopicsViewModel(communityId))
        } else {
            RoomList(createCommunityRoomsViewModel(communityId), showAid = false)
        }
    }
}

@Composable
private fun CommunityTopicFab(community: CommunityInfo?) {
    val user = LocalUserInfo.current ?: return
    if (community == null) return
    val hasAccess = community.owner == user.id || community.member?.status == MemberStatus.JOINED
    if (!hasAccess || community.readOnly || user.status != UserStatus.NORMAL) return
    val appNav = LocalAppNavFactory.current
    FloatingActionButton(onClick = {
        appNav.newAppNav().gotoTopicCompose(
            TopicComposeData.Community(
                community.id,
                community.id ob ObjectType.COMMUNITY,
            ),
        )
    }) {
        Icon(actionIcon(ActionGlyph.PLUS), stringResource(Res.string.compose_topic))
    }
}

@Composable
fun communityNavRoutes(): List<NavRoute> =
    listOf(
        NavRoute("/topics", navigationIcon(NavigationGlyph.TEXT), stringResource(Res.string.topics)),
        NavRoute("/rooms", navigationIcon(NavigationGlyph.MESSAGES_SQUARE), stringResource(Res.string.rooms)),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDialog(communityInfo: CommunityInfo?, showDialog: Boolean, dismiss: () -> Unit) {
    if (communityInfo != null && showDialog) {
        ReadingAdaptiveOverlay(dismiss, ReadingOverlaySize.MENU) {
            CommunityDialogInternal(communityInfo, dismiss)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityDialogInternal(communityInfo: CommunityInfo, dismiss: () -> Unit) {
    val communityId = communityInfo.id
    val appNavFactory = LocalAppNavFactory.current
    val currentCommunity = appNavFactory.hasRouteFlow<CommunityScreen> { it.communityId == communityId }
    ReadingActionMenuSurface(onDismiss = dismiss, heading = {
        Row(
            modifier =
            Modifier.fillMaxWidth().clickable(enabled = !currentCommunity) {
                dismiss()
                appNavFactory.newAppNav().gotoCommunity(communityId, false)
            },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CommunityIconWithDialog(
                communityInfo,
                showDialog = false,
                40.dp,
                setClickEvent = false,
            ) {}
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(communityInfo.name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                if (communityInfo.aid.isNotBlank()) Text(CoreStrings.aid(communityInfo.aid), fontSize = 12.sp)
            }
        }
    }) {
        Text(
            stringResource(Res.string.created_at, communityInfo.createdTime.formatDisplayTime()),
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            fontSize = 12.sp,
        )
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
    IconActionMenu {
        ButtonNav(
            IconRes.Vector(navigationIcon(NavigationGlyph.USERS)),
            stringResource(Res.string.all_members),
            {
                ButtonBadgeSuffix(communityInfo.memberCount)
            },
            semanticDescription = "all-members-action",
        ) {
            dismiss()
            appNavFactory.newAppNav().gotoMemberPage(communityId, ObjectType.COMMUNITY)
        }
        CommunityMemberStatusButton(communityInfo, globalDialogController, communityId)
        FavoriteButton(communityInfo.favoriteId, communityInfo.tuple())
        SubscriptionButton(communityInfo.subscriptionId, communityInfo.tuple())
        CommunityAdminButtons(communityInfo, dismiss, appNavFactory, communityId)
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
        ButtonNav(navigationIcon(NavigationGlyph.FOLDER), stringResource(Res.string.files_title)) {
            dismiss()
            appNavFactory.newAppNav().gotoFileExplorer(communityId ob ObjectType.COMMUNITY)
        }

        if (myInfo.status == UserStatus.NORMAL && !communityInfo.readOnly) {
            ButtonNav(actionIcon(ActionGlyph.MESSAGE_SQUARE_PLUS), stringResource(Res.string.create_room_action)) {
                dismiss()
                appNavFactory.newAppNav().gotoRoomCompose(communityId)
            }
            ButtonNav(actionIcon(ActionGlyph.BADGE_PLUS), stringResource(Res.string.create_title_action)) {
                dismiss()
                appNavFactory.newAppNav().gotoCommunityTitleCompose(communityId)
            }
        }
        ButtonNav(actionIcon(ActionGlyph.SETTINGS), stringResource(Res.string.settings_title)) {
            dismiss()
            appNavFactory.newAppNav().gotoSettingPage(communityId, ObjectType.COMMUNITY)
        }
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
        ButtonNav(navigationIcon(NavigationGlyph.LOG_OUT), stringResource(Res.string.exit_community)) {
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
        ButtonNav(actionIcon(ActionGlyph.LOG_IN), stringResource(Res.string.join_community)) {
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
