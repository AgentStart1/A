/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalAccountSwitcher
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalSessionManager
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.common.AppNav
import com.storyteller_f.alphaheart.app.common.UserScreen
import com.storyteller_f.alphaheart.app.common.finishSignOut
import com.storyteller_f.alphaheart.app.common.hasRouteFlow
import com.storyteller_f.alphaheart.app.connected
import com.storyteller_f.alphaheart.app.create
import com.storyteller_f.alphaheart.app.disconnected
import com.storyteller_f.alphaheart.app.file_explorer
import com.storyteller_f.alphaheart.app.grant_notification
import com.storyteller_f.alphaheart.app.menu_signed_out
import com.storyteller_f.alphaheart.app.my_comments
import com.storyteller_f.alphaheart.app.my_favorites
import com.storyteller_f.alphaheart.app.my_reactions
import com.storyteller_f.alphaheart.app.my_subscriptions
import com.storyteller_f.alphaheart.app.pages.title.ComposeMenu
import com.storyteller_f.alphaheart.app.pages.topic.TopicComposeData
import com.storyteller_f.alphaheart.app.preference
import com.storyteller_f.alphaheart.app.room_read_only
import com.storyteller_f.alphaheart.app.settings
import com.storyteller_f.alphaheart.app.sign_out
import com.storyteller_f.alphaheart.app.sign_out_prompt
import com.storyteller_f.alphaheart.app.switch_account
import com.storyteller_f.alphaheart.app.utils.createConnectivity
import com.storyteller_f.alphaheart.app.utils.rememberAppNotificationPermission
import com.storyteller_f.alphaheart.app.utils.requestAppNotificationPermission
import com.storyteller_f.alphaheart.app.utils.unregisterPushService
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.FavoriteButton
import com.storyteller_f.alphaheart.client.compose_core.components.IconActionMenu
import com.storyteller_f.alphaheart.client.compose_core.components.IconRes
import com.storyteller_f.alphaheart.client.compose_core.components.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingActionMenuSurface
import com.storyteller_f.alphaheart.client.compose_core.components.SignInButton
import com.storyteller_f.alphaheart.client.compose_core.components.SubscriptionButton
import com.storyteller_f.alphaheart.client.compose_core.components.UserIcon
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.components.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.supportsChildAccountSwitching
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.getUserInfo
import com.storyteller_f.alphaheart.client.core.getUserInfoByAid
import com.storyteller_f.alphaheart.client.core.restoreSession
import com.storyteller_f.alphaheart.client.core.signOut
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserOverview
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UserStatus
import dev.jordond.connectivity.Connectivity
import dev.jordond.connectivity.compose.rememberConnectivityState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBadge as Badge
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog as BasicAlertDialog

@Composable
private fun SelfDialogInternal(
    signOutController: CustomAlertDialogController,
    userInfo: UserInfo?,
    dismiss: () -> Unit,
    clickCreate: () -> Unit,
    overviewHandler: LoadingHandler<UserOverview>,
) {
    val instance by LocalUiViewModel.current.instance.collectAsState()
    val isAlreadySignIn = instance.isAlreadySign
    ReadingActionMenuSurface(
        onDismiss = dismiss,
        heading = {
            if (isAlreadySignIn) {
                UserCard(userInfo, dismiss)
            } else {
                Text(stringResource(Res.string.menu_signed_out), fontSize = 18.sp)
            }
        },
    ) {
        if (isAlreadySignIn) {
            IconActionMenu {
                if (userInfo?.status == UserStatus.NORMAL) CreateButton(dismiss, clickCreate)
                UserOverviewActions(overviewHandler, dismiss)
                AccountSwitchButton(dismiss, overviewHandler)
                NotificationButton()
                ConnectionButton()
                FileExplorerButton(dismiss)
                SettingsButton(dismiss)
                SignOutButton(signOutController)
                SystemSettingsButton(dismiss)
            }
        } else {
            SignInBox(dismiss)
            SystemSettingsButton(dismiss)
        }
    }
}

@Composable
fun SignInBox(dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
    ) {
        SignInButton {
            dismiss()
            appNavFactory.newAppNav().gotoSignIn()
        }
    }
}

@Composable
fun SystemSettingsButton(dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    ButtonNav(
        navigationIcon(NavigationGlyph.SETTINGS_2),
        stringResource(Res.string.preference),
    ) {
        dismiss()
        appNavFactory.newAppNav().gotoPreference()
    }
}

@Composable
fun CreateButton(dismiss: () -> Unit, clickCreate: () -> Unit) {
    ButtonNav(
        actionIcon(ActionGlyph.PLUS),
        stringResource(Res.string.create),
        semanticDescription = "create",
    ) {
        dismiss()
        clickCreate()
    }
}

class UserInfoPreviewProvider : PreviewParameterProvider<UserInfo> {
    override val values: Sequence<UserInfo>
        get() = sequenceOf(UserInfo.EMPTY.copy(nickname = "hello"))
}

@Preview
@Composable
fun UserCard(@PreviewParameter(UserInfoPreviewProvider::class) userInfo: UserInfo?, dismiss: () -> Unit = {}) {
    UserCardContainer(userInfo, dismiss) {
        UnboundSimpleUserCell(userInfo)
    }
}

@Composable
fun UserCardContainer(userInfo: UserInfo?, dismiss: () -> Unit, content: @Composable () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    val isUserPage =
        appNavFactory.hasRouteFlow<UserScreen> {
            it.uid == userInfo?.id
        }
    val shape = RoundedCornerShape(8.dp)
    val cellClickable = !isUserPage
    val openUser: () -> Unit = {
        dismiss()
        userInfo?.id?.let { appNavFactory.newAppNav().gotoUser(it) }
    }
    val userLabel =
        userInfo?.let { info ->
            val aid = info.aid
            val identity = if (aid != null) CoreStrings.aid(aid) else CoreStrings.ad(info.address)
            "${info.nickname}, $identity"
        }
    val modifier =
        Modifier.fillMaxWidth()
            .appiumSemantics(
                testTag = "user-dialog-cell",
                description = userLabel,
                text = userLabel,
                onClick = openUser.takeIf { userInfo != null && cellClickable },
            )
            .clip(shape)
            .clickable(userInfo != null && cellClickable, onClick = openUser)

    Box(modifier) {
        content()
    }
}

@Composable
private fun UserOverviewActions(overviewHandler: LoadingHandler<UserOverview>, dismiss: () -> Unit) {
    val overview by overviewHandler.data.collectAsState()
    val state by overviewHandler.state.collectAsState()
    val loading = state is LoadingState.Loading
    val appNav = LocalAppNavFactory.current
    fun open(action: () -> Unit) {
        dismiss()
        action()
    }
    UserOverviewActionTiles(
        overview,
        loading,
        onFavorite = { open { appNav.newAppNav().gotoFavoritePage() } },
        onSubscription = { open { appNav.newAppNav().gotoSubscriptionPage() } },
        onReaction = { open { appNav.newAppNav().gotoUserReactionRecordsPage() } },
        onComment = { open { appNav.newAppNav().gotoUserCommentsPage() } },
    )
}

@Composable
internal fun UserOverviewActionTiles(
    overview: UserOverview?,
    loading: Boolean,
    onFavorite: () -> Unit,
    onSubscription: () -> Unit,
    onReaction: () -> Unit,
    onComment: () -> Unit,
) {
    OverviewMenuAction(overview?.acg ?: 0, "ACG", actionIcon(ActionGlyph.WALLET), loading) {}
    OverviewMenuAction(
        overview?.favoriteCount ?: 0,
        stringResource(Res.string.my_favorites),
        actionIcon(ActionGlyph.HEART),
        loading,
    ) {
        onFavorite()
    }
    OverviewMenuAction(
        overview?.subscriptionCount ?: 0,
        stringResource(Res.string.my_subscriptions),
        actionIcon(ActionGlyph.BELL),
        loading,
    ) { onSubscription() }
    OverviewMenuAction(
        overview?.reactionRecordCount ?: 0,
        stringResource(Res.string.my_reactions),
        actionIcon(ActionGlyph.THUMBS_UP),
        loading,
    ) {
        onReaction()
    }
    OverviewMenuAction(
        overview?.commentCount ?: 0,
        stringResource(Res.string.my_comments),
        actionIcon(ActionGlyph.MESSAGE_CIRCLE),
        loading,
    ) {
        onComment()
    }
}

@Composable
private fun OverviewMenuAction(
    count: Long,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    loading: Boolean,
    onClick: () -> Unit,
) {
    ButtonNav(
        if (loading) IconRes.Loading else IconRes.Vector(icon),
        label,
        suffix = { ButtonBadgeSuffix(count) },
        semanticDescription = "$label, $count",
        onClick = onClick,
    )
}

@Composable
private fun UnboundSimpleUserCell(userInfo: UserInfo?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserIcon(
            setClickEvent = false,
            userInfo?.avatar?.url,
            40.dp,
        ) {}
        if (userInfo != null) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        userInfo.nickname,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (userInfo.status == UserStatus.READ_ONLY) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            stringResource(Res.string.room_read_only),
                            style = MaterialTheme.typography.labelSmall,
                            modifier =
                            Modifier
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(4.dp),
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                val aid = userInfo.aid
                if (aid != null) {
                    Text(CoreStrings.aid(aid), style = MaterialTheme.typography.labelSmall)
                } else {
                    Text(
                        CoreStrings.ad(userInfo.address),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
fun AccountSwitchButton(dismiss: () -> Unit, overviewHandler: LoadingHandler<UserOverview>) {
    if (!supportsChildAccountSwitching) return
    val accountSwitcher = LocalAccountSwitcher.current
    val userOverview by overviewHandler.data.collectAsState()
    val userOverviewState by overviewHandler.state.collectAsState()
    val isLoading by remember {
        derivedStateOf {
            userOverviewState is LoadingState.Loading
        }
    }
    ButtonNav(
        if (isLoading) IconRes.Loading else IconRes.Vector(Icons.Default.SwitchAccount),
        stringResource(Res.string.switch_account),
        {
            ButtonBadgeSuffix(
                number = userOverview?.childAccountCount ?: 0,
                hasUnread = userOverview?.hasUnreadChildRoomMessage == true,
            )
        },
    ) {
        dismiss()
        accountSwitcher.switch()
    }
}

@Composable
fun ButtonBadgeSuffix(number: Long, hasUnread: Boolean = false) {
    val shape = RoundedCornerShape(5.dp)
    BadgedBox(badge = {
        if (hasUnread) {
            Badge(containerColor = Color.Red)
        }
    }) {
        Text(
            number.toString(),
            modifier =
            Modifier.widthIn(min = 16.dp).clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer, shape)
                .padding(horizontal = 3.dp),
            fontSize = 11.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun SignOutButton(controller: CustomAlertDialogController) {
    val title = stringResource(Res.string.sign_out_prompt)
    if (!isInChildAccount()) {
        ButtonNav(navigationIcon(NavigationGlyph.LOG_OUT), stringResource(Res.string.sign_out)) {
            controller.showTitle(title)
        }
    }
}

@Composable
fun NotificationButton() {
    val inspectionMode = LocalInspectionMode.current
    if (inspectionMode) return
    val hasPermission = rememberAppNotificationPermission()

    if (!hasPermission) {
        ButtonNav(actionIcon(ActionGlyph.BELL_RING), stringResource(Res.string.grant_notification)) {
            requestAppNotificationPermission()
        }
    }
}

@Composable
fun ConnectionButton() {
    val inspectionMode = LocalInspectionMode.current
    if (inspectionMode) return
    val connectivity =
        remember {
            createConnectivity()
        }
    val scope = rememberCoroutineScope()
    val connectivityState = rememberConnectivityState(connectivity, scope)
    val status = connectivityState.status
    if (status is Connectivity.Status.Connected) {
        ButtonNav(
            actionIcon(if (status.metered) ActionGlyph.SIGNAL else ActionGlyph.WIFI),
            stringResource(Res.string.connected),
        )
    } else {
        ButtonNav(
            actionIcon(ActionGlyph.WIFI_OFF),
            stringResource(Res.string.disconnected),
        )
    }
}

@Composable
fun SettingsButton(dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    ButtonNav(actionIcon(ActionGlyph.USER_ROUND_COG), stringResource(Res.string.settings)) {
        dismiss()
        appNavFactory.newAppNav().gotoUserSetting()
    }
}

@Composable
fun FileExplorerButton(dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    ButtonNav(navigationIcon(NavigationGlyph.FOLDER), stringResource(Res.string.file_explorer)) {
        dismiss()
        appNavFactory.newAppNav().gotoFileExplorer()
    }
}

suspend fun AppGlobalDialogController.signOut(uiViewModel: UIViewModel, appNav: AppNav) {
    val address = uiViewModel.instance.value.address
    useResult {
        request {
            signOut()
        }
    }.onSuccess {
        uiViewModel.historyManager.exitSession(address)
        unregisterPushService()
        uiViewModel.logout()
        appNav.finishSignOut()
    }
}

private suspend fun refreshMyInfo(my: UserInfo?, sessionManager: UserSessionManager) {
    try {
        val sessionModel = sessionManager.model
        if (my == null) {
            sessionManager.restoreSession().onSuccess { sessionModel.updateUser(it) }
        } else {
            val aid = my.aid
            val userInfo =
                if (aid.isNullOrBlank()) {
                    sessionManager.getUserInfo(my.id)
                } else {
                    sessionManager.getUserInfoByAid(aid)
                }.getOrThrow()
            sessionModel.updateUser(userInfo)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Napier.e(e) {
            "refresh user info"
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserDialog(userInfo: UserInfo?, showDialog: Boolean, dismiss: () -> Unit) {
    if (showDialog) {
        BasicAlertDialog({
            dismiss()
        }) {
            ReadingActionMenuSurface(onDismiss = dismiss, heading = { UserCard(userInfo, dismiss) }) {
                if (userInfo != null) {
                    IconActionMenu(twoActions = true) {
                        FavoriteButton(userInfo.favoriteId, userInfo.tuple())
                        SubscriptionButton(userInfo.subscriptionId, userInfo.tuple())
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SelfDialog(
    userInfo: UserInfo?,
    showDialog: Boolean,
    overviewHandler: LoadingHandler<UserOverview>,
    clickCreate: () -> Unit,
    dismiss: () -> Unit,
) {
    val signOutController = remember { CustomAlertDialogController() }
    val signOutState by signOutController.state
    val globalDialogController = LocalGlobalDialog.current
    val uiViewModel = LocalUiViewModel.current
    val appNavFactory = LocalAppNavFactory.current
    if (showDialog) {
        // Replace the account modal instead of opening a second modal inside it.
        if (signOutState == null) {
            BasicAlertDialog(dismiss) {
                val sessionManager = LocalSessionManager.current
                val refreshCached =
                    remember(overviewHandler, userInfo?.id) {
                        overviewHandler.state.value == LoadingState.Done || overviewHandler.data.value != null
                    }
                LaunchedEffect(overviewHandler, userInfo?.id) {
                    if (userInfo != null && refreshCached) {
                        overviewHandler.state.first { it !is LoadingState.Loading }
                        overviewHandler.refresh()
                    }
                }
                LaunchedEffect(userInfo) {
                    refreshMyInfo(userInfo, sessionManager)
                }
                SelfDialogInternal(signOutController, userInfo, dismiss, clickCreate, overviewHandler)
            }
        } else {
            CustomAlertDialog(signOutController, signOutController::close) {
                dismiss()
                globalDialogController.launch {
                    globalDialogController.signOut(uiViewModel, appNavFactory.newAppNav())
                }
            }
        }
    }
}

@Composable
fun UserIconWithDialog(userInfo: UserInfo?, setClickEvent: Boolean = true, size: Dp = 40.dp) {
    var showUserDialog by remember {
        mutableStateOf(false)
    }
    val url = userInfo?.avatar?.url
    UserIcon(setClickEvent, url, size = size) {
        showUserDialog = true
    }
    UserDialog(
        userInfo,
        showUserDialog,
    ) {
        showUserDialog = false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelfUserIconWithDialog(
    userInfo: UserInfo?,
    size: Dp = 40.dp,
    overviewHandler: LoadingHandler<UserOverview>,
    onClickCreate: (() -> Unit)? = null,
) {
    var showCreate by remember { mutableStateOf(false) }
    val nav = LocalAppNavFactory.current
    ComposeMenu(showCreate, { showCreate = false }) { type ->
        showCreate = false
        when (type) {
            ObjectType.COMMUNITY -> nav.newAppNav().gotoCommunityCompose()

            ObjectType.ROOM -> nav.newAppNav().gotoRoomCompose()

            ObjectType.TITLE -> nav.newAppNav().gotoTitleCompose()

            ObjectType.TOPIC ->
                userInfo?.let {
                    nav.newAppNav().gotoTopicCompose(
                        TopicComposeData.User(it.id, it.tuple()),
                    )
                }

            else -> Unit
        }
    }
    var showUserDialog by remember {
        mutableStateOf(false)
    }
    val url = userInfo?.avatar?.url
    val showDialog = {
        showUserDialog = true
    }
    Box(
        modifier =
        Modifier.appiumSemantics(
            testTag = "me",
            description = "avatar",
            onClick = showDialog,
        ),
    ) {
        UserIcon(true, url, size = size, onClick = showDialog)
    }
    SelfDialog(
        userInfo,
        showUserDialog,
        overviewHandler,
        { if (onClickCreate != null) onClickCreate() else showCreate = true },
    ) {
        showUserDialog = false
    }
}
