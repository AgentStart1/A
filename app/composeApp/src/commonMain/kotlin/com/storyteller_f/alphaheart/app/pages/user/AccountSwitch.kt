/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAccountSwitcher
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.add_child_account
import com.storyteller_f.alphaheart.app.child_account
import com.storyteller_f.alphaheart.app.common.ChildAccountsViewModel
import com.storyteller_f.alphaheart.app.common.getChildAccountsViewModel
import com.storyteller_f.alphaheart.app.current_account
import com.storyteller_f.alphaheart.app.main_account
import com.storyteller_f.alphaheart.app.switch_account
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.UserIcon
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.supportsChildAccountSwitching
import com.storyteller_f.alphaheart.client.core.RawUserPass
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.alphaheart.client.core.addChildAccount
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.Type2Algo
import com.storyteller_f.shared.algoRunCatching
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.ChildAccountInfo
import com.storyteller_f.shared.model.UserInfo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBadge as Badge
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedButton as OutlinedButton

class AccountSwitcher(val state: MutableState<Boolean> = mutableStateOf(false)) {
    fun switch() {
        state.value = true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState()
    BaseSheet(expand, sheetState, {
        expand = false
    }) {
        val viewModel = getChildAccountsViewModel()
        AccountSwitchInternal(viewModel)
    }
}

@Composable
private fun AccountSwitchInternal(viewModel: ChildAccountsViewModel) {
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
    Text(
        stringResource(Res.string.switch_account),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(end = 40.dp, bottom = 24.dp),
    )
    val pagingItems = viewModel.flow.collectAsLazyPagingItems()
    LaunchedEffect(Unit) { pagingItems.refresh() }
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End) {
        OutlinedButton({
            scope.launch {
                if (globalDialogController.useResult { mainSessionManager.addChildAccount() }.isSuccess) {
                    pagingItems.refresh()
                }
            }
        }) {
            Icon(Icons.Default.Add, null)
            Text(stringResource(Res.string.add_child_account), Modifier.padding(start = 8.dp))
        }
    }
    val mainUser by mainSessionManager.model.userHandler.data.collectAsState()
    mainUser?.let {
        AccountSwitchRow(
            it,
            stringResource(if (isInChildAccount) Res.string.main_account else Res.string.current_account),
            selected = !isInChildAccount,
        ) { if (isInChildAccount) uiViewModel.switchToMain() }
        if (pagingItems.itemCount > 0) {
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
        }
    }
    ChildAccountList(viewModel) {
        scope.launch { globalDialogController.switchUser(it, uiViewModel) }
    }
}

@Composable
private fun ChildAccountList(viewModel: ChildAccountsViewModel, onSelect: (ChildAccountInfo) -> Unit) {
    StateView(viewModel, modifier = Modifier.height(300.dp)) { pagingItems ->
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
        stringResource(if (selected) Res.string.current_account else Res.string.child_account),
        selected,
        childAccountInfo.hasUnreadRoomMessage,
        onClick,
    )
}

@Composable
private fun AccountSwitchRow(
    userInfo: UserInfo,
    role: String,
    selected: Boolean,
    hasUnread: Boolean = false,
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
        BadgedBox(badge = { if (hasUnread) Badge(containerColor = Color.Red) }) {
            UserIcon(setClickEvent = false, avatarUrl = userInfo.avatar?.url) {}
        }
        Column(Modifier.weight(1f)) {
            Text(userInfo.nickname, style = MaterialTheme.typography.bodyLarge)
            Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (!selected) Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
    }
}

@Composable
fun isInChildAccount(): Boolean {
    val uiViewModel = LocalUiViewModel.current
    val instance by uiViewModel.instance.collectAsState()
    return instance is IAccountInstance.Child
}

suspend fun AppGlobalDialogController.switchUser(childAccountInfo: ChildAccountInfo, uiViewModel: UIViewModel) {
    check(supportsChildAccountSwitching) { "Browser child account switching is unsupported" }
    val mainInstance =
        when (val instance = uiViewModel.instance.value) {
            is IAccountInstance.Child -> instance.main
            is IAccountInstance.None -> return
            is IAccountInstance.Regular -> instance
        }
    useResult {
        algoRunCatching {
            val currentUserPass = mainInstance.sessionManager.passHolder.currentUserPass
            val userPass = checkNotNull(currentUserPass) { "user not login" }
            val (decrypted, decryptedEnc) =
                userPass.decryptChildAccount(
                    childAccountInfo.encryptedPrivateKey,
                    childAccountInfo.encryptedAesKey,
                    childAccountInfo.algoType,
                    childAccountInfo.encryptedEncryptionPrivateKey,
                ).getOrThrow()
            val algoImpl = getAlgo(childAccountInfo.algoType)
            val pem = algoImpl.getPemPrivateKeyFromDer(decrypted).getOrThrow()
            val publicKey = algoImpl.getDerPublicKeyFromPrivateKey(pem).getOrThrow()

            val address = algoImpl.calcAddress(publicKey).getOrThrow()
            val authKey =
                if (childAccountInfo.algoType == com.storyteller_f.shared.model.AlgoType.DILITHIUM) {
                    checkNotNull(decryptedEnc) { "decryptedEnc is null" }
                    val type2Algo = algoImpl.encryptionAlgo as Type2Algo
                    val pemPrivateKey =
                        type2Algo.getPemEncryptionPrivateKeyFromDerPrivateKey(decryptedEnc)
                            .getOrThrow()
                    val publicEnc = type2Algo.getDerEncryptionPublicKeyFromPemPrivateKey(pemPrivateKey).getOrThrow()
                    com.storyteller_f.alphaheart.client.core.AuthKey.Dilithium(
                        pemPrivateKey = pem,
                        derPrivateKey = decrypted,
                        derPublicKey = publicKey,
                        pemEncryptionPrivateKey = pemPrivateKey, // Not implemented for child accounts
                        derEncryptionPrivateKey = decryptedEnc,
                        derEncryptionPublicKey = publicEnc,
                    )
                } else {
                    com.storyteller_f.alphaheart.client.core.AuthKey.P256(
                        pemPrivateKey = pem,
                        derPrivateKey = decrypted,
                        derPublicKey = publicKey,
                    )
                }
            uiViewModel.switchUser(mainInstance, RawUserPass(RawUserPassInfo(address, authKey)))
        }
    }
}
