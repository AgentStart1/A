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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalAccountSwitcher
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalSessionManager
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.common.UserScreen
import com.storyteller_f.alphaheart.app.common.hasRouteFlow
import com.storyteller_f.alphaheart.app.connected
import com.storyteller_f.alphaheart.app.create
import com.storyteller_f.alphaheart.app.disconnected
import com.storyteller_f.alphaheart.app.file_explorer
import com.storyteller_f.alphaheart.app.grant_notification
import com.storyteller_f.alphaheart.app.preference
import com.storyteller_f.alphaheart.app.settings
import com.storyteller_f.alphaheart.app.sign_out
import com.storyteller_f.alphaheart.app.sign_out_prompt
import com.storyteller_f.alphaheart.app.switch_account
import com.storyteller_f.alphaheart.app.ui.MaterialSymbolsOutlined
import com.storyteller_f.alphaheart.app.utils.createConnectivity
import com.storyteller_f.alphaheart.app.utils.rememberAppNotificationPermission
import com.storyteller_f.alphaheart.app.utils.requestAppNotificationPermission
import com.storyteller_f.alphaheart.app.utils.unregisterPushService
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.ButtonNav
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.CustomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.FavoriteButton
import com.storyteller_f.alphaheart.client.compose_core.components.IconRes
import com.storyteller_f.alphaheart.client.compose_core.components.SignInButton
import com.storyteller_f.alphaheart.client.compose_core.components.SubscriptionButton
import com.storyteller_f.alphaheart.client.compose_core.components.UserIcon
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
import com.storyteller_f.shared.type.UserStatus
import dev.jordond.connectivity.Connectivity
import dev.jordond.connectivity.compose.rememberConnectivityState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource

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
    DialogContainer {
        if (isAlreadySignIn) {
            SelfUserDetailCard(userInfo, dismiss, overviewHandler, clickCreate)
            AccountSwitchButton(dismiss, overviewHandler)
            NotificationButton()
            ConnectionButton()
            FileExplorerButton(dismiss)
            SettingsButton(dismiss)
            SignOutButton(signOutController)
        } else {
            SignInBox(dismiss)
        }
        SystemSettingsButton(dismiss)
    }
}

@Composable
private fun RowScope.StatCell(value: Long, iconRes: IconRes, onClick: () -> Unit) {
    val str =
        remember(value) {
            HumanReadable.number(value.toFloat())
        }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier =
        Modifier.weight(1f).clip(shape).clickable {
            onClick()
        }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        CustomIcon(iconRes)
        Spacer(modifier = Modifier.width(8.dp))
        Text(str)
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
        MaterialSymbolsOutlined.Settings,
        stringResource(Res.string.preference),
    ) {
        dismiss()
        appNavFactory.newAppNav().gotoPreference()
    }
}

@Composable
fun CreateButton(dismiss: () -> Unit, clickCreate: () -> Unit) {
    ButtonNav(
        Icons.Default.Add,
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
    val modifier =
        Modifier.fillMaxWidth()
            .appiumSemantics(
                testTag = "user-dialog-cell",
                description = "user-dialog-cell",
                text = userInfo?.let { "${it.nickname}, ${CoreStrings.ad(it.address)}" },
                onClick = openUser.takeIf { userInfo != null && cellClickable },
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceDim, shape)
            .clickable(userInfo != null && cellClickable, onClick = openUser)
            .padding(8.dp)
    Box(modifier) {
        content()
    }
}

@Composable
fun SelfUserDetailCard(
    userInfo: UserInfo?,
    dismiss: () -> Unit = {},
    overviewHandler: LoadingHandler<UserOverview>,
    onClickCreate: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        UserCardContainer(userInfo, dismiss) {
            UnboundSimpleUserCell(userInfo)
        }
        val userOverview by overviewHandler.data.collectAsState()
        val userOverviewState by overviewHandler.state.collectAsState()
        val isLoading by remember {
            derivedStateOf {
                userOverviewState is LoadingState.Loading
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val createTopic = {
                dismiss()
                onClickCreate()
            }
            FilledIconButton(
                onClick = createTopic,
                modifier =
                Modifier.appiumSemantics(
                    description = "create",
                    onClick = createTopic,
                ),
            ) {
                Icon(Icons.Default.Add, "create")
            }
            UserOverviewRow(userOverview, isLoading, dismiss)
        }
    }
}

@Composable
private fun UserOverviewRow(userOverview: UserOverview?, isLoading: Boolean, dismiss: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val appNav = LocalAppNavFactory.current
    Column(
        modifier =
        Modifier.clip(shape)
            .background(MaterialTheme.colorScheme.surfaceDim, shape),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatCell(
                userOverview?.acg ?: 0,
                if (isLoading) IconRes.Loading else IconRes.Vector(Icons.Default.AccountBalanceWallet),
            ) {
            }
            StatCell(
                userOverview?.favoriteCount ?: 0,
                if (isLoading) IconRes.Loading else IconRes.Vector(Icons.Default.Favorite),
            ) {
                dismiss()
                appNav.newAppNav().gotoFavoritePage()
            }
            StatCell(
                userOverview?.subscriptionCount ?: 0,
                if (isLoading) IconRes.Loading else IconRes.Vector(Icons.Default.NotificationsActive),
            ) {
                dismiss()
                appNav.newAppNav().gotoSubscriptionPage()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatCell(
                userOverview?.reactionRecordCount ?: 0,
                if (isLoading) IconRes.Loading else IconRes.Vector(Icons.Default.ThumbUp),
            ) {
                dismiss()
                appNav.newAppNav().gotoUserReactionRecordsPage()
            }
            StatCell(
                userOverview?.commentCount ?: 0,
                if (isLoading) IconRes.Loading else IconRes.Vector(Icons.Default.ChatBubble),
            ) {
                dismiss()
                appNav.newAppNav().gotoUserCommentsPage()
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun UnboundSimpleUserCell(userInfo: UserInfo?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserIcon(
            setClickEvent = false,
            userInfo?.avatar?.url,
            60.dp,
        ) {}
        if (userInfo != null) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(userInfo.nickname, style = MaterialTheme.typography.titleMedium)
                    if (userInfo.status == UserStatus.READ_ONLY) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "ReadOnly",
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
    val shape = RoundedCornerShape(8.dp)
    BadgedBox(badge = {
        if (hasUnread) {
            Badge(containerColor = Color.Red)
        }
    }) {
        Text(
            number.toString(),
            modifier =
            Modifier.clip(shape)
                .background(MaterialTheme.colorScheme.primaryContainer, shape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun SignOutButton(controller: CustomAlertDialogController) {
    val title = stringResource(Res.string.sign_out_prompt)
    if (!isInChildAccount()) {
        ButtonNav(Icons.AutoMirrored.Default.Logout, stringResource(Res.string.sign_out)) {
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
        ButtonNav(Icons.Default.Notifications, stringResource(Res.string.grant_notification)) {
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
            if (status.metered) Icons.Default.SignalCellularAlt else Icons.Default.Wifi,
            stringResource(Res.string.connected),
        )
    } else {
        ButtonNav(
            MaterialSymbolsOutlined.SignalDisconnected,
            stringResource(Res.string.disconnected),
        )
    }
}

@Composable
fun SettingsButton(dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    ButtonNav(MaterialSymbolsOutlined.SettingsAccountBox, stringResource(Res.string.settings)) {
        dismiss()
        appNavFactory.newAppNav().gotoUserSetting()
    }
}

@Composable
fun FileExplorerButton(dismiss: () -> Unit) {
    val appNavFactory = LocalAppNavFactory.current
    ButtonNav(Icons.Default.Folder, stringResource(Res.string.file_explorer)) {
        dismiss()
        appNavFactory.newAppNav().gotoFileExplorer()
    }
}

suspend fun AppGlobalDialogController.signOut(uiViewModel: UIViewModel) {
    val address = uiViewModel.instance.value.address
    useResult {
        request {
            signOut()
        }
    }.onSuccess {
        uiViewModel.historyManager.exitSession(address)
        unregisterPushService()
        uiViewModel.logout()
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
            DialogContainer {
                UserCard(userInfo, dismiss)
                if (userInfo != null) {
                    FavoriteButton(userInfo.favoriteId, userInfo.tuple())
                    SubscriptionButton(userInfo.subscriptionId, userInfo.tuple())
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
    if (showDialog) {
        // Replace the account modal instead of opening a second modal inside it.
        if (signOutState == null) {
            BasicAlertDialog(dismiss) {
                val sessionManager = LocalSessionManager.current
                LaunchedEffect(userInfo) {
                    refreshMyInfo(userInfo, sessionManager)
                }
                SelfDialogInternal(signOutController, userInfo, dismiss, clickCreate, overviewHandler)
            }
        } else {
            CustomAlertDialog(signOutController, signOutController::close) {
                dismiss()
                globalDialogController.launch {
                    globalDialogController.signOut(uiViewModel)
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

@Composable
fun SelfUserIconWithDialog(
    userInfo: UserInfo?,
    size: Dp = 40.dp,
    overviewHandler: LoadingHandler<UserOverview>,
    onClickCreate: () -> Unit = {},
) {
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
        onClickCreate,
    ) {
        showUserDialog = false
    }
}
