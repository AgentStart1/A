/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.storyteller_f.alphaheart.api.NewUser
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.AlgoTypeSelector
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialogContext
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.copyText
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.addUser
import com.storyteller_f.alphaheart.panel.CustomPanelSessionManager
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.add
import com.storyteller_f.alphaheart.panel.add_user
import com.storyteller_f.alphaheart.panel.all_users
import com.storyteller_f.alphaheart.panel.common.AddUserViewModel
import com.storyteller_f.alphaheart.panel.common.AllUsersViewModel
import com.storyteller_f.alphaheart.panel.common.OnUserAdded
import com.storyteller_f.alphaheart.panel.common.createPanelAllUserViewModel
import com.storyteller_f.alphaheart.panel.common.submitPanelRequest
import com.storyteller_f.alphaheart.panel.components.UserCell
import com.storyteller_f.alphaheart.panel.components.selectedPanelEntityId
import com.storyteller_f.alphaheart.panel.edit_private_key
import com.storyteller_f.alphaheart.panel.encryption_private_key
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.nickname
import com.storyteller_f.alphaheart.panel.private_key_required
import com.storyteller_f.alphaheart.panel.random
import com.storyteller_f.alphaheart.panel.review_address_empty
import com.storyteller_f.alphaheart.panel.review_alias_input
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.replaceCrlf
import com.storyteller_f.shared.type.ObjectType
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog as BasicAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedButton as OutlinedButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField

@Serializable
private data object AddUserStartScreen : NavKey

@Serializable
private data object AddUserPrivateKeyScreen : NavKey

@Composable
fun AllUsersPage() {
    val viewModel = createPanelAllUserViewModel()
    AllUsersPageInternal(viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllUsersPageInternal(viewModel: AllUsersViewModel) {
    val panelNav = LocalPanelNav.current
    val selectedId = selectedPanelEntityId(panelNav.backStack.lastOrNull(), ObjectType.USER)
    Scaffold(
        topBar = {
            var showDialog by remember {
                mutableStateOf(false)
            }
            TopAppBar(title = {
                Text(
                    stringResource(Res.string.all_users),
                    modifier = Modifier.appiumSemantics(text = stringResource(Res.string.all_users)),
                )
            }, navigationIcon = {
                IconButton({ panelNav.open() }) { Icon(Icons.Default.Menu, stringResource(Res.string.menu)) }
            }, actions = {
                IconButton({
                    showDialog = true
                }) {
                    Icon(Icons.Default.Add, stringResource(Res.string.add_user))
                }
            })
            AddUserDialog(showDialog) {
                showDialog = false
            }
        },
    ) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Box(Modifier.safeArea(paddingValues, direction)) {
            StateView(viewModel) { items ->
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                    pagingItems(items, key = {
                        it.id
                    }) { index ->
                        val info = items[index]
                        Column(Modifier.padding(vertical = 7.dp)) {
                            UserCell(info, selected = info != null && info.id == selectedId) {
                                val uid = info?.id
                                if (uid != null) {
                                    panelNav.gotoUserDetail(uid)
                                }
                            }
                        }
                        if (index < items.itemCount - 1) HorizontalDivider()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(showDialog: Boolean, dismiss: () -> Unit) {
    if (showDialog) {
        BasicAlertDialog(
            {
                dismiss()
            },
        ) {
            AddUserInternal(dismiss)
        }
    }
}

@Composable
fun AddUserInternal(dismiss: () -> Unit) {
    val addUserViewModel =
        viewModel {
            AddUserViewModel()
        }
    val module =
        SerializersModule {
            polymorphic(NavKey::class) {
                subclass(AddUserStartScreen::class)
                subclass(AddUserPrivateKeyScreen::class)
            }
        }
    val config =
        remember {
            SavedStateConfiguration {
                serializersModule = module
            }
        }
    val addUserBackStack = rememberNavBackStack(config, AddUserStartScreen)
    DialogContainer(verticalArrangement = Arrangement.spacedBy(20.dp), onDismissRequest = dismiss) {
        val title =
            if (addUserBackStack.last() == AddUserStartScreen) Res.string.add_user else Res.string.edit_private_key
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(end = 40.dp),
        )
        NavDisplay(
            addUserBackStack,
            entryProvider =
            entryProvider {
                entry<AddUserStartScreen> {
                    AddUserProfilePage(
                        addUserViewModel,
                        { addUserBackStack.add(AddUserPrivateKeyScreen) },
                        dismiss,
                    )
                }
                entry<AddUserPrivateKeyScreen> {
                    AddUserPrivateKeyPage(addUserViewModel) {
                        addUserBackStack.removeLastOrNull()
                    }
                }
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun AddUserPrivateKeyPage(addUserViewModel: AddUserViewModel, goBack: () -> Unit) {
    val toast = LocalToaster.current
    val privateKey by addUserViewModel.privateKey.collectAsState()
    val encryptionPrivateKey by addUserViewModel.encryptionPrivateKey.collectAsState()
    val scope = rememberCoroutineScope()
    val algoType by addUserViewModel.algoType.collectAsState()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AlgoTypeSelector(algoType) {
            addUserViewModel.updateAlgoType(it)
        }
        // 警告提示紧跟算法选择器显示
        if (algoType == AlgoType.P256) {
            Text(
                CoreStrings.p256SecurityWarning(),
                color = androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row {
            IconButton({
                scope.launch {
                    val f = FileKit.openFilePicker()
                    if (f != null) {
                        val privateKey = f.readBytes().decodeToString().replaceCrlf()
                        addUserViewModel.updatePrivateKey(privateKey)
                    }
                }
            }) {
                Icon(Icons.Default.FileOpen, CoreStrings.selectFile())
            }
            IconButton({
                addUserViewModel.autoGeneratePrivateKey()
            }) {
                Icon(Icons.Default.Casino, stringResource(Res.string.random))
            }
            val clipboard = LocalClipboard.current
            IconButton({
                scope.launch {
                    clipboard.copyText(privateKey, toast)
                }
            }) {
                Icon(Icons.Default.ContentCopy, "copy")
            }
            Spacer(modifier = Modifier.weight(1f))
        }
        OutlinedTextField(privateKey, {
            addUserViewModel.updatePrivateKey(it)
        }, label = {
            Text(CoreStrings.privateKey())
        }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
        if (algoType == AlgoType.DILITHIUM) {
            OutlinedTextField(encryptionPrivateKey, {
                addUserViewModel.updateEncryptionPrivateKey(it)
            }, label = {
                Text(stringResource(Res.string.encryption_private_key))
            }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 5)
        }
        Button(goBack) {
            Text(CoreStrings.ok())
        }
    }
}

@Composable
private fun AddUserProfilePage(addUserViewModel: AddUserViewModel, gotoPrivateKey: () -> Unit, dismiss: () -> Unit) {
    val nickname by addUserViewModel.nickname.collectAsState()
    val aid by addUserViewModel.aid.collectAsState()
    val address by addUserViewModel.address.collectAsState()
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(nickname, {
            addUserViewModel.updateNickname(it)
        }, label = {
            Text(stringResource(Res.string.nickname))
        }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(aid, {
            addUserViewModel.updateAid(it)
        }, label = {
            Text(stringResource(Res.string.review_alias_input))
        }, modifier = Modifier.fillMaxWidth())

        AddressField(address, gotoPrivateKey)

        AddUserDialogButtons(dismiss, addUserViewModel)
    }
}

@Composable
private fun AddUserDialogButtons(dismiss: () -> Unit, addUserViewModel: AddUserViewModel) {
    val failureDialog = rememberAlertDialogController()
    CustomAlertDialog(failureDialog, failureDialog::close, failureDialog::close)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
        OutlinedButton(dismiss) {
            Text(CoreStrings.cancel())
        }
        val globalDialogController = LocalPanelGlobalDialog.current
        val requiredPrivateKeyMessage = stringResource(Res.string.private_key_required)
        Button({
            globalDialogController.addUser(
                addUserViewModel = addUserViewModel,
                showFailure = failureDialog::showTitle,
                dismiss = dismiss,
                requiredPrivateKeyMessage = requiredPrivateKeyMessage,
            )
        }) {
            Text(stringResource(Res.string.add))
        }
    }
}

@Composable
private fun AddressField(address: String?, gotoPrivateKey: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(10.dp)
        Row(
            modifier =
            Modifier.weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, shape)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                address ?: stringResource(Res.string.review_address_empty),
                modifier = Modifier.weight(1f),
            )
            IconButton(gotoPrivateKey) {
                Icon(Icons.Default.Edit, stringResource(Res.string.edit_private_key))
            }
        }
    }
}

private fun CustomGlobalDialogController<GlobalDialogContext<CustomPanelSessionManager>>.addUser(
    addUserViewModel: AddUserViewModel,
    showFailure: (String) -> Unit,
    dismiss: () -> Unit,
    requiredPrivateKeyMessage: String,
) {
    val nickname = addUserViewModel.nickname.value.takeIf { it.isNotBlank() }
    val aid = addUserViewModel.aid.value.takeIf { it.isNotBlank() }
    val publicKey = addUserViewModel.publicKey.value?.takeIf { it.isNotBlank() }
    if (publicKey == null) {
        showFailure(requiredPrivateKeyMessage)
        return
    }
    val algoType = addUserViewModel.algoType.value
    val authKey =
        if (algoType == AlgoType.DILITHIUM) {
            val encryptionPublicKey = addUserViewModel.encryptionPublicKey.value
            if (encryptionPublicKey == null) {
                showFailure(requiredPrivateKeyMessage)
                return
            }
            com.storyteller_f.alphaheart.api.TransferAuthKey.Dilithium(
                derPublicKey = publicKey,
                derEncryptionPublicKey = encryptionPublicKey,
            )
        } else {
            com.storyteller_f.alphaheart.api.TransferAuthKey.P256(
                derPublicKey = publicKey,
            )
        }
    val newUser = NewUser(nickname, aid, authKey)
    submitPanelRequest(
        request = {
            addUser(newUser).onSuccess {
                this@addUser.context.emitEvent(OnUserAdded(it))
            }
        },
        onSuccess = { dismiss() },
    )
}
