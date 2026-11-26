/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.AlgoTypeSelector
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.paging.pagingItems
import com.storyteller_f.alphaheart.client.compose_core.theme.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.copyText
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelNav
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.add
import com.storyteller_f.alphaheart.panel.add_user
import com.storyteller_f.alphaheart.panel.all_users
import com.storyteller_f.alphaheart.panel.common.AddUserViewModel
import com.storyteller_f.alphaheart.panel.common.AllUsersViewModel
import com.storyteller_f.alphaheart.panel.common.createPanelAllUserViewModel
import com.storyteller_f.alphaheart.panel.components.UserCell
import com.storyteller_f.alphaheart.panel.copy_private_key
import com.storyteller_f.alphaheart.panel.edit_private_key
import com.storyteller_f.alphaheart.panel.encryption_private_key
import com.storyteller_f.alphaheart.panel.generate_private_key
import com.storyteller_f.alphaheart.panel.import_encryption_key
import com.storyteller_f.alphaheart.panel.import_private_key
import com.storyteller_f.alphaheart.panel.import_signing_key
import com.storyteller_f.alphaheart.panel.menu
import com.storyteller_f.alphaheart.panel.navigation.selectedPanelEntityId
import com.storyteller_f.alphaheart.panel.nickname
import com.storyteller_f.alphaheart.panel.private_key_required
import com.storyteller_f.alphaheart.panel.review_address_empty
import com.storyteller_f.alphaheart.panel.review_alias_input
import com.storyteller_f.alphaheart.panel.select_private_key
import com.storyteller_f.alphaheart.panel.signing_private_key
import com.storyteller_f.alphaheart.panel.user.AddUserPrivateKeyScreen
import com.storyteller_f.alphaheart.panel.user.AddUserStartScreen
import com.storyteller_f.alphaheart.panel.user.addUser
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.replaceCrlf
import com.storyteller_f.shared.type.ObjectType
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
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
                IconButton({ panelNav.open() }) { Icon(actionIcon(ActionGlyph.MENU), stringResource(Res.string.menu)) }
            }, actions = {
                IconButton({
                    showDialog = true
                }) {
                    Icon(actionIcon(ActionGlyph.PLUS), stringResource(Res.string.add_user))
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
            if (addUserBackStack.last() == AddUserStartScreen) Res.string.add_user else Res.string.select_private_key
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

@Composable
internal fun AddUserPrivateKeyPage(
    addUserViewModel: AddUserViewModel,
    readKeyFile: suspend () -> String? = {
        FileKit.openFilePicker()?.readBytes()?.decodeToString()?.replaceCrlf()
    },
    goBack: () -> Unit,
) {
    val privateKey by addUserViewModel.privateKey.collectAsState()
    val encryptionPrivateKey by addUserViewModel.encryptionPrivateKey.collectAsState()
    val algoType by addUserViewModel.algoType.collectAsState()
    val signingAndEncryption = algoType == AlgoType.DILITHIUM
    val signingImport = if (signingAndEncryption) Res.string.import_signing_key else Res.string.import_private_key
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AlgoTypeSelector(algoType, mlDsaLabel = "ML-DSA", onSelected = addUserViewModel::updateAlgoType)
        if (algoType == AlgoType.P256) {
            Text(CoreStrings.p256SecurityWarning(), color = MaterialTheme.colorScheme.error)
        }
        key(algoType) {
            AddUserKeyField(
                if (signingAndEncryption) stringResource(Res.string.signing_private_key) else CoreStrings.privateKey(),
                stringResource(signingImport),
                privateKey,
                addUserViewModel::updatePrivateKey,
                readKeyFile,
            )
            if (signingAndEncryption) {
                AddUserKeyField(
                    stringResource(Res.string.encryption_private_key),
                    stringResource(Res.string.import_encryption_key),
                    encryptionPrivateKey,
                    addUserViewModel::updateEncryptionPrivateKey,
                    readKeyFile,
                )
            }
        }
        AddUserKeyActions(privateKey, addUserViewModel::autoGeneratePrivateKey, goBack)
    }
}

@Composable
private fun AddUserKeyField(
    label: String,
    importLabel: String,
    value: String,
    update: (String) -> Unit,
    readKeyFile: suspend () -> String?,
) {
    val scope = rememberCoroutineScope()
    val importKey: () -> Unit = {
        scope.launch { readKeyFile()?.let(update) }
    }
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f))
            IconButton(importKey, Modifier.appiumSemantics(description = importLabel, onClick = importKey)) {
                Icon(actionIcon(ActionGlyph.FOLDER_OPEN), importLabel)
            }
        }
        val fieldModifier =
            Modifier.fillMaxWidth().appiumSemantics(
                description = label,
                input = true,
                inputValue = value,
                onInputValueChange = update,
            )
        OutlinedTextField(value, update, modifier = fieldModifier, minLines = 3, maxLines = 5)
    }
}

@Composable
private fun AddUserKeyActions(privateKey: String, generate: () -> Unit, goBack: () -> Unit) {
    val clipboard = LocalClipboard.current
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
    val copyLabel = stringResource(Res.string.copy_private_key)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReadingTextButton(generate) { Text(stringResource(Res.string.generate_private_key)) }
        IconButton({ scope.launch { clipboard.copyText(privateKey, toast) } }, enabled = privateKey.isNotBlank()) {
            Icon(actionIcon(ActionGlyph.COPY), copyLabel)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Button(goBack) { Text(CoreStrings.ok()) }
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
                Icon(actionIcon(ActionGlyph.PEN_LINE), stringResource(Res.string.edit_private_key))
            }
        }
    }
}
