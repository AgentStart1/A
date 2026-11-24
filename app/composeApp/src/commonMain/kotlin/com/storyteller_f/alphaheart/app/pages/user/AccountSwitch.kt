/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAccountSwitcher
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.add_child_account
import com.storyteller_f.alphaheart.app.auth.switchUser
import com.storyteller_f.alphaheart.app.child_account
import com.storyteller_f.alphaheart.app.common.ChildAccountsViewModel
import com.storyteller_f.alphaheart.app.common.getChildAccountsViewModel
import com.storyteller_f.alphaheart.app.components.ReadingSheet
import com.storyteller_f.alphaheart.app.current_account
import com.storyteller_f.alphaheart.app.main_account
import com.storyteller_f.alphaheart.app.room_unread
import com.storyteller_f.alphaheart.app.switch_account
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.supportsChildAccountSwitching
import com.storyteller_f.alphaheart.client.core.addChildAccount
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.ChildAccountInfo
import com.storyteller_f.shared.model.UserInfo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBadge as Badge
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

@Composable
fun AccountSwitch() {
    if (!supportsChildAccountSwitching) return
    val accountSwitcher = LocalAccountSwitcher.current
    var expand by accountSwitcher.state
    val instance by LocalUiViewModel.current.instance.collectAsState()
    LaunchedEffect(instance) {
        // The chooser belongs to the previous account after a successful switch.
        expand = false
    }
    if (expand) {
        ReadingSheet(
            stringResource(Res.string.switch_account),
            onDismiss = { expand = false },
        ) {
            AccountSwitchInternal(getChildAccountsViewModel())
        }
    }
}

@Composable
private fun ColumnScope.AccountSwitchInternal(viewModel: ChildAccountsViewModel) {
    val isInChildAccount = isInChildAccount()
    val uiViewModel = LocalUiViewModel.current
    val mainInstance =
        when (val instance = uiViewModel.instance.value) {
            is IAccountInstance.Child -> instance.main
            is IAccountInstance.None -> return
            is IAccountInstance.Regular -> instance
        }
    val mainSessionManager = mainInstance.sessionManager
    val globalDialogController = LocalGlobalDialog.current
    val scope = rememberCoroutineScope()
    val pagingItems = viewModel.flow.collectAsLazyPagingItems()
    LaunchedEffect(Unit) { pagingItems.refresh() }
    val mainUser by mainSessionManager.model.userHandler.data.collectAsState()
    mainUser?.let {
        AccountSwitchRow(
            it,
            stringResource(Res.string.main_account),
            selected = !isInChildAccount,
        ) { if (isInChildAccount) uiViewModel.switchToMain() }
        if (pagingItems.itemCount > 0) {
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
        }
    }
    ChildAccountList(viewModel, Modifier.weight(1f, fill = false)) {
        scope.launch { globalDialogController.switchUser(it, uiViewModel) }
    }
    FloatingActionButton(
        onClick = {
            scope.launch {
                if (globalDialogController.useResult { mainSessionManager.addChildAccount() }.isSuccess) {
                    pagingItems.refresh()
                }
            }
        },
        modifier = Modifier.align(Alignment.End),
    ) {
        Icon(actionIcon(ActionGlyph.USER_ROUND_PLUS), stringResource(Res.string.add_child_account))
    }
}

@Composable
private fun ChildAccountList(
    viewModel: ChildAccountsViewModel,
    modifier: Modifier,
    onSelect: (ChildAccountInfo) -> Unit,
) {
    StateView(viewModel, modifier = modifier.fillMaxWidth()) { pagingItems ->
        LazyColumn(
            contentPadding = PaddingValues(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            pagingItems(
                pagingItems,
                key = { accountInfo ->
                    accountInfo.id
                },
            ) { index ->
                val childAccountInfo = pagingItems[index]
                childAccountInfo?.let {
                    ChildAccountCell(it) { onSelect(it) }
                    if (index < pagingItems.itemCount - 1) HorizontalDivider(Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

@Composable
fun ChildAccountCell(childAccountInfo: ChildAccountInfo, onClick: () -> Unit) {
    val instance by LocalUiViewModel.current.instance.collectAsState()
    val selected = instance.address == childAccountInfo.userInfo.address
    AccountSwitchRow(
        childAccountInfo.userInfo,
        stringResource(Res.string.child_account),
        selected,
        hasUnread = childAccountInfo.hasUnreadRoomMessage,
        algorithm = if (childAccountInfo.algoType == AlgoType.DILITHIUM) "ML-DSA" else "P256",
        onClick = onClick,
    )
}

@Composable
private fun AccountSwitchRow(
    userInfo: UserInfo,
    role: String,
    selected: Boolean,
    hasUnread: Boolean = false,
    algorithm: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier =
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .appiumSemantics(text = userInfo.address, onClick = onClick)
            .clickable(enabled = !selected, onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val unread = stringResource(Res.string.room_unread)
        BadgedBox(badge = {
            if (hasUnread) Badge(Modifier.semantics { contentDescription = unread }, containerColor = Color.Red)
        }) {
            UserIconWithDialog(userInfo)
        }
        Column(Modifier.weight(1f)) {
            Text(userInfo.nickname, style = MaterialTheme.typography.bodyLarge)
            userInfo.aid?.let { Text(CoreStrings.aid(it), style = MaterialTheme.typography.labelSmall) }
            algorithm?.let { Text(it, style = MaterialTheme.typography.labelSmall) }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(role, style = MaterialTheme.typography.labelSmall)
            if (selected) {
                Text(stringResource(Res.string.current_account), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun isInChildAccount(): Boolean {
    val uiViewModel = LocalUiViewModel.current
    val instance by uiViewModel.instance.collectAsState()
    return instance is IAccountInstance.Child
}
