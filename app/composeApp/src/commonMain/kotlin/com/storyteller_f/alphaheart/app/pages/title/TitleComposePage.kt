/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.title

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalRefCellHandlerProvider
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.cancel_action
import com.storyteller_f.alphaheart.app.clear_selection
import com.storyteller_f.alphaheart.app.common.TitleComposeSheetType
import com.storyteller_f.alphaheart.app.common.TitleComposeViewModel
import com.storyteller_f.alphaheart.app.common.createMemberSearchViewModel
import com.storyteller_f.alphaheart.app.common.createRoomSearchViewModel
import com.storyteller_f.alphaheart.app.common.createSearchCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createTitleComposeViewModel
import com.storyteller_f.alphaheart.app.components.ReadingSheet
import com.storyteller_f.alphaheart.app.confirm_action
import com.storyteller_f.alphaheart.app.create
import com.storyteller_f.alphaheart.app.create_title_action
import com.storyteller_f.alphaheart.app.creation_title_name
import com.storyteller_f.alphaheart.app.description_label
import com.storyteller_f.alphaheart.app.edit_title_description
import com.storyteller_f.alphaheart.app.error.TitleActionError
import com.storyteller_f.alphaheart.app.expiry_date_label
import com.storyteller_f.alphaheart.app.expiry_label
import com.storyteller_f.alphaheart.app.expiry_picker_title
import com.storyteller_f.alphaheart.app.expiry_time_label
import com.storyteller_f.alphaheart.app.invalid_date_time
import com.storyteller_f.alphaheart.app.name
import com.storyteller_f.alphaheart.app.pages.CreateFormLayout
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.community.CommunityRows
import com.storyteller_f.alphaheart.app.pages.room.RoomList
import com.storyteller_f.alphaheart.app.pages.user.InputDialog
import com.storyteller_f.alphaheart.app.pages.user.MemberList
import com.storyteller_f.alphaheart.app.receiver_label
import com.storyteller_f.alphaheart.app.scope_community
import com.storyteller_f.alphaheart.app.scope_label
import com.storyteller_f.alphaheart.app.scope_room
import com.storyteller_f.alphaheart.app.search
import com.storyteller_f.alphaheart.app.search_scope
import com.storyteller_f.alphaheart.app.search_start_hint
import com.storyteller_f.alphaheart.app.search_users
import com.storyteller_f.alphaheart.app.select_expiry_time
import com.storyteller_f.alphaheart.app.select_title_receiver
import com.storyteller_f.alphaheart.app.select_title_scope
import com.storyteller_f.alphaheart.app.title.ExpiryDraft
import com.storyteller_f.alphaheart.app.title.titleComposePageCreateTitle
import com.storyteller_f.alphaheart.app.title_form_choose_scope
import com.storyteller_f.alphaheart.app.title_form_description
import com.storyteller_f.alphaheart.app.title_form_description_empty
import com.storyteller_f.alphaheart.app.title_form_permanent
import com.storyteller_f.alphaheart.app.title_join
import com.storyteller_f.alphaheart.app.title_permanent
import com.storyteller_f.alphaheart.app.title_regular
import com.storyteller_f.alphaheart.app.title_type_label
import com.storyteller_f.alphaheart.app.unselected_reference
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.ReadingOverlaySize
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAlertDialog as AlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab as Tab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow as TabRow

@Composable
fun TitleComposePage() {
    LocalUserInfo.current ?: return
    TitleComposeInternal()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleComposeInternal() {
    val vm = createTitleComposeViewModel()
    val appNavFactory = LocalAppNavFactory.current
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    CommonComposePage({
        scope.launch {
            globalDialogController.useResult {
                titleComposePageCreateTitle(vm.buildNewTitle().getOrThrow())
            }.onSuccess {
                appNavFactory.newAppNav().back()
            }
        }
    }) { actions ->
        TitleComposeInternalEdit(vm, actions)
    }
    TitleComposeSheet(vm)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TitleComposeSheet(vm: TitleComposeViewModel) {
    val isSheetVisible by vm.isSheetVisibleFlow.collectAsState()
    val supportedObjectTypes by vm.supportedObjectTypesFlow.collectAsState()
    ObjectPicker(
        isSheetVisible,
        supportedObjectTypes,
        vm::clearShowSheetType,
        title =
        stringResource(
            if (vm.showSheetType.value == TitleComposeSheetType.SCOPE) {
                Res.string.select_title_scope
            } else {
                Res.string.select_title_receiver
            },
        ),
    ) {
        when (vm.showSheetType.value) {
            TitleComposeSheetType.SCOPE -> vm.setTitleScope(it)
            TitleComposeSheetType.RECEIVER -> vm.setReceiver(it.objectId)
            TitleComposeSheetType.NONE -> {}
        }
        vm.clearShowSheetType()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonComposePage(
    onCheck: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    leading: @Composable () -> Unit = {},
    content: @Composable (@Composable () -> Unit) -> Unit,
) {
    val nav = LocalAppNavFactory.current
    Scaffold(topBar = {
        ReadingPageToolbar(title ?: stringResource(Res.string.create_title_action), subtitle, leading)
    }) { padding ->
        Box(Modifier.padding(padding).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            content {
                Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        ReadingTextButton({ nav.newAppNav().back() }) {
                            Text(com.storyteller_f.alphaheart.client.compose_core.CoreStrings.cancel())
                        }
                        com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton(
                            onCheck,
                            Modifier.appiumSemantics(description = "submit", onClick = onCheck),
                        ) {
                            Icon(
                                com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon(
                                    com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph.CHECK,
                                ),
                                null,
                            )
                            Text(stringResource(Res.string.create), Modifier.padding(start = 7.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TitleComposeInternalEdit(vm: TitleComposeViewModel, footer: @Composable () -> Unit = {}) {
    val name by vm.name.collectAsState()
    val titleType by vm.titleType.collectAsState()
    val titleScope by vm.titleScope.collectAsState()
    val receiver by vm.receiver.collectAsState()
    val isScopeLocked by vm.isScopeLocked.collectAsState()
    val isTypeLocked by vm.isTypeLocked.collectAsState()
    CreateFormLayout {
        OutlinedTextField(name, vm::setName, singleLine = true, label = {
            Text(stringResource(Res.string.creation_title_name))
        }, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))

        if (isTypeLocked) {
            TitleLockedField(
                stringResource(Res.string.title_type_label),
                stringResource(
                    if (titleType == TitleType.JOIN) Res.string.title_join else Res.string.title_regular,
                ),
            )
        } else {
            TitleTypeSelector(titleType, vm::setTitleType)
        }
        if (isScopeLocked) {
            TitleLockedField(stringResource(Res.string.scope_label), TitleScopeName(titleScope))
        }
        ReceiverEditor(vm::openReceiverSheet, receiver)
        if (!isScopeLocked) {
            TitleScopeEditor(vm::openScopeSheet, titleScope, vm::clearTitleScope)
        }

        ExpireTimeEditor(vm)

        DescriptionEditor(vm)
        footer()
    }
}

@Composable
private fun ExpireTimeEditor(vm: TitleComposeViewModel) {
    val expiresAtText by vm.expiresAtText.collectAsState()
    var showExpireDialog by remember { mutableStateOf(false) }
    val value =
        if (expiresAtText.isBlank()) {
            stringResource(
                Res.string.title_form_permanent,
            )
        } else {
            expiresAtText.replace('T', ' ')
        }
    TitleFormRow(stringResource(Res.string.expiry_label), value) {
        ReadingIconButton({ showExpireDialog = true }) {
            Icon(actionIcon(ActionGlyph.CALENDAR), stringResource(Res.string.select_expiry_time))
        }
    }
    ExpireTimePickerDialog(
        show = showExpireDialog,
        init = expiresAtText,
        dismiss = { showExpireDialog = false },
        onConfirm = { newValue ->
            vm.setExpiresAtText(newValue)
            showExpireDialog = false
        },
        onClear = {
            vm.setExpiresAtText("")
            showExpireDialog = false
        },
    )
}

@Composable
internal fun ExpireTimePickerDialog(
    show: Boolean,
    init: String,
    dismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (!show) return
    var draft by remember(init) {
        mutableStateOf(ExpiryDraft(init.substringBefore('T', ""), init.substringAfter('T', ""), init.isBlank()))
    }
    val invalidDateMessage = stringResource(Res.string.invalid_date_time)
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(Res.string.expiry_picker_title)) },
        confirmButton = {
            com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton({
                if (draft.permanent) {
                    onClear()
                } else {
                    captureFailure(
                        TitleActionError::from,
                    ) { LocalDateTime.parse("${draft.date}T${draft.time}").toString() }
                        .onSuccess(onConfirm)
                        .onFailure { draft = draft.copy(error = invalidDateMessage) }
                }
            }) {
                Icon(
                    com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon(
                        com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph.CHECK,
                    ),
                    null,
                )
                Text(stringResource(Res.string.confirm_action), Modifier.padding(start = 7.dp))
            }
        },
        dismissButton = {
            com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedButton(dismiss) {
                Text(stringResource(Res.string.cancel_action))
            }
        },
        text = { ExpiryInputFields(draft) { draft = it } },
    )
}

@Composable
private fun ExpiryInputFields(draft: ExpiryDraft, update: (ExpiryDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        OutlinedTextField(
            draft.date,
            { update(draft.copy(date = it, permanent = false, error = null)) },
            Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.expiry_date_label)) },
            placeholder = { Text("YYYY-MM-DD") },
        )
        OutlinedTextField(
            draft.time,
            { update(draft.copy(time = it, permanent = false, error = null)) },
            Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.expiry_time_label)) },
            placeholder = { Text("HH:mm") },
        )
        Row(
            Modifier.toggleable(draft.permanent, role = Role.Checkbox) {
                update(draft.copy(permanent = it, error = null))
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(draft.permanent, null, Modifier.size(18.dp))
            Text(stringResource(Res.string.title_permanent))
        }
        draft.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}

@Composable
private fun DescriptionEditor(vm: TitleComposeViewModel) {
    val content by vm.content.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val summary = content.ifBlank { stringResource(Res.string.title_form_description_empty) }
    TitleFormRow(stringResource(Res.string.title_form_description), summary) {
        ReadingIconButton({ showDialog = true }) {
            Icon(actionIcon(ActionGlyph.PEN_LINE), stringResource(Res.string.edit_title_description))
        }
    }
    InputDialog(
        showDialog,
        content,
        stringResource(Res.string.edit_title_description),
        { showDialog = false },
        minLines = 4,
        showCancel = false,
        inputLabel = stringResource(Res.string.description_label),
    ) {
        vm.setContent(it)
        showDialog = false
    }
}

@Composable
private fun ReceiverEditor(showReceiverSheet: () -> Unit, receiver: PrimaryKey?) {
    val name =
        if (receiver == null) {
            stringResource(Res.string.unselected_reference)
        } else {
            val user by LocalRefCellHandlerProvider.current.userHandler(receiver).data.collectAsState()
            user?.nickname.orEmpty()
        }
    TitleFormRow(stringResource(Res.string.receiver_label), name) {
        ReadingIconButton(showReceiverSheet) {
            Icon(actionIcon(ActionGlyph.USER_ROUND), stringResource(Res.string.select_title_receiver))
        }
    }
}

@Composable
private fun TitleScopeEditor(
    showScopeSheet: () -> Unit,
    titleScope: ObjectTuple?,
    clearScope: () -> Unit,
    readOnly: Boolean = false,
) {
    val name = TitleScopeName(titleScope)
    TitleFormRow(stringResource(Res.string.scope_label), name) {
        if (!readOnly) {
            ReadingIconButton(showScopeSheet) {
                Icon(actionIcon(ActionGlyph.SCAN), stringResource(Res.string.title_form_choose_scope))
            }
            if (titleScope != null) {
                ReadingIconButton(clearScope) {
                    Icon(actionIcon(ActionGlyph.X), stringResource(Res.string.clear_selection))
                }
            }
        }
    }
}

@Composable
private fun TitleScopeName(scope: ObjectTuple?): String {
    val provider = LocalRefCellHandlerProvider.current
    return when (scope?.objectType) {
        null -> stringResource(Res.string.unselected_reference)

        ObjectType.COMMUNITY -> {
            val info by provider.communityHandler(scope.objectId).data.collectAsState()
            info?.name.orEmpty()
        }

        ObjectType.ROOM -> {
            val info by provider.roomHandler(scope.objectId).data.collectAsState()
            info?.name.orEmpty()
        }

        ObjectType.USER -> {
            val info by provider.userHandler(scope.objectId).data.collectAsState()
            info?.nickname.orEmpty()
        }

        else -> scope.objectId.toString()
    }
}

@Composable
private fun TitleLockedField(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        com.storyteller_f.alphaheart.client.compose_core.components.SimpleMessageWithButton(value, label)
    }
    androidx.compose.material3.HorizontalDivider()
}

@Composable
private fun TitleFormRow(label: String, value: String, actions: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value)
        }
        actions()
    }
    HorizontalDivider()
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TitleTypeSelector(titleType: TitleType, updateTitleType: (TitleType) -> Unit, readOnly: Boolean = false) {
    com.storyteller_f.alphaheart.app.components.ReadingSelect(
        stringResource(Res.string.title_type_label),
        titleType,
        listOf(
            TitleType.REGULAR to stringResource(Res.string.title_regular),
            TitleType.JOIN to stringResource(Res.string.title_join),
        ),
        updateTitleType,
        Modifier.fillMaxWidth().padding(bottom = 20.dp),
        enabled = !readOnly,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectPicker(
    showSheet: Boolean,
    supportObjectType: List<ObjectType>,
    hideSheet: () -> Unit,
    title: String,
    onCheck: (ObjectTuple) -> Unit,
) {
    if (!showSheet || supportObjectType.isEmpty()) return
    ReadingSheet(title, hideSheet, ReadingOverlaySize.FORM) {
        var input by remember {
            mutableStateOf("")
        }
        Column(modifier = Modifier.height(360.dp)) {
            var currentType by remember(supportObjectType) {
                mutableStateOf(supportObjectType.first())
            }
            TypeSelector(supportObjectType, currentType, {
                currentType = it
            }, input) {
                input = it
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
                if (input.isBlank()) {
                    Text(
                        stringResource(Res.string.search_start_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    ObjectList(input, currentType, {
                        onCheck(it.id ob ObjectType.COMMUNITY)
                    }, {
                        onCheck(it.id ob ObjectType.ROOM)
                    }, {
                        onCheck(it.id ob ObjectType.USER)
                    })
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TypeSelector(
    supportObjectType: List<ObjectType>,
    currentType: ObjectType,
    setType: (ObjectType) -> Unit,
    input: String,
    setInput: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (supportObjectType.size > 1) {
            TabRow(contentPadding = PaddingValues(top = 8.dp)) {
                supportObjectType.forEach { type ->
                    Tab(selected = currentType == type, onClick = { setType(type) }) {
                        Text(objectTypeLabel(type))
                    }
                }
            }
        }
        OutlinedTextField(
            input,
            setInput,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(Res.string.search)) },
        )
    }
}

@Composable
private fun objectTypeLabel(type: ObjectType): String =
    stringResource(
        when (type) {
            ObjectType.USER -> Res.string.search_users
            ObjectType.COMMUNITY -> Res.string.scope_community
            ObjectType.ROOM -> Res.string.scope_room
            else -> Res.string.search_scope
        },
    )

@Composable
fun ObjectList(
    input: String,
    currentType: ObjectType?,
    onClickCommunity: (CommunityInfo) -> Unit,
    onClickRoom: (RoomInfo) -> Unit,
    onClickUser: (UserInfo) -> Unit,
) {
    if (input.isNotBlank() && currentType != null) {
        when (currentType) {
            ObjectType.COMMUNITY -> {
                val communitiesViewModel = createSearchCommunitiesViewModel(JoinStatusSearch.JOINED, input)
                CommunityRows(communitiesViewModel, onClickCommunity)
            }

            ObjectType.ROOM -> {
                val roomsViewModel = createRoomSearchViewModel(JoinStatusSearch.JOINED, input)
                RoomList(roomsViewModel, onClickRoom)
            }

            ObjectType.TOPIC -> Unit

            ObjectType.USER -> {
                val membersViewModel = createMemberSearchViewModel(input)
                MemberList(membersViewModel, onClickUser)
            }

            ObjectType.TITLE -> Unit

            ObjectType.FILE -> Unit

            ObjectType.PANEL_ACCOUNT -> Unit
        }
    }
}
