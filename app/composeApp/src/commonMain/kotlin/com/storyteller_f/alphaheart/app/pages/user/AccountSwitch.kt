/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Input
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.LocalAccountSwitcher
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.common.ChildAccountsViewModel
import com.storyteller_f.alphaheart.app.common.getChildAccountsViewModel
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.CustomIcon
import com.storyteller_f.alphaheart.client.compose_core.components.SheetContainer
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
import kotlinx.coroutines.launch

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
        SheetContainer {
            val viewModel = getChildAccountsViewModel()
            AccountSwitchInternal(viewModel)
        }
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
    Row(modifier = Modifier.padding(horizontal = 10.dp)) {
        if (isInChildAccount) {
            FilledIconButton({
                uiViewModel.switchToMain()
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Switch to main account")
            }
        }
        val pagingItems = viewModel.flow.collectAsLazyPagingItems()
        LaunchedEffect(Unit) {
            pagingItems.refresh()
        }
        FilledIconButton({
            scope.launch {
                if (globalDialogController.useResult {
                        mainSessionManager.addChildAccount()
                    }.isSuccess
                ) {
                    pagingItems.refresh()
                }
            }
        }) {
            Icon(Icons.Default.Add, "add")
        }
    }
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
                    ChildAccountCell(it) {
                        scope.launch {
                            globalDialogController.switchUser(it, uiViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChildAccountCell(childAccountInfo: ChildAccountInfo, onClick: () -> Unit) {
    val userInfo = childAccountInfo.userInfo
    Row(
        modifier =
        Modifier.padding(8.dp)
            .appiumSemantics(text = userInfo.address, onClick = onClick)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BadgedBox(badge = {
            if (childAccountInfo.hasUnreadRoomMessage) {
                Badge(containerColor = Color.Red)
            }
        }) {
            UserIcon(
                setClickEvent = false,
                avatarUrl = userInfo.avatar?.url,
            ) {}
        }
        Column {
            Text(userInfo.nickname, style = MaterialTheme.typography.titleMedium)
            val aid = userInfo.aid
            if (aid != null) {
                Text(CoreStrings.aid(aid), style = MaterialTheme.typography.labelSmall)
            } else {
                Text(CoreStrings.ad(userInfo.address), style = MaterialTheme.typography.labelSmall)
            }
            Text(childAccountInfo.algoType.name)
        }
        CustomIcon(Icons.AutoMirrored.Filled.Input)
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
