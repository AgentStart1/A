/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.title

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.common.OnTitleCreated
import com.storyteller_f.alphaheart.app.common.TitleComposeSheetType
import com.storyteller_f.alphaheart.app.common.TitleComposeViewModel
import com.storyteller_f.alphaheart.app.common.createMemberSearchViewModel
import com.storyteller_f.alphaheart.app.common.createRoomSearchViewModel
import com.storyteller_f.alphaheart.app.common.createSearchCommunitiesViewModel
import com.storyteller_f.alphaheart.app.common.createTitleComposeViewModel
import com.storyteller_f.alphaheart.app.components.AppTopicContentView
import com.storyteller_f.alphaheart.app.error.TitleActionError
import com.storyteller_f.alphaheart.app.pages.AdaptiveFormLayout
import com.storyteller_f.alphaheart.app.pages.community.CommunityList
import com.storyteller_f.alphaheart.app.pages.community.CommunityRefCell
import com.storyteller_f.alphaheart.app.pages.room.RoomList
import com.storyteller_f.alphaheart.app.pages.room.RoomRefCell
import com.storyteller_f.alphaheart.app.pages.user.InputDialog
import com.storyteller_f.alphaheart.app.pages.user.MemberList
import com.storyteller_f.alphaheart.app.pages.user.UserRefCell
import com.storyteller_f.alphaheart.client.compose_core.components.BaseSheet
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.components.safeArea
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrElse
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleDetail
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number

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
                createTitle(vm.buildNewTitle().getOrThrow())
            }.onSuccess {
                appNavFactory.newAppNav().back()
            }
        }
    }) {
        TitleComposeInternalEdit(vm)
    }
    TitleComposeSheet(vm)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun TitleComposeSheet(vm: TitleComposeViewModel) {
    val sheetState = rememberModalBottomSheetState()

    val isSheetVisible by vm.isSheetVisibleFlow.collectAsState()
    val supportedObjectTypes by vm.supportedObjectTypesFlow.collectAsState()
    ObjectPicker(
        isSheetVisible,
        sheetState,
        supportedObjectTypes,
        vm::clearShowSheetType,
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
fun CommonComposePage(onCheck: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(topBar = {
        TopAppBar({
        }, actions = {
            IconButton(
                onClick = onCheck,
                modifier =
                Modifier.appiumSemantics(
                    description = "submit",
                    onClick = onCheck,
                ),
            ) {
                Icon(Icons.Filled.Check, contentDescription = "submit")
            }
        })
    }) { paddingValues ->
        val direction = LocalLayoutDirection.current
        Box(
            Modifier.safeArea(paddingValues, direction).fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            content()
        }
    }
}

@Composable
fun TitleComposeInternalEdit(vm: TitleComposeViewModel) {
    val name by vm.name.collectAsState()
    val titleType by vm.titleType.collectAsState()
    val titleScope by vm.titleScope.collectAsState()
    val receiver by vm.receiver.collectAsState()
    val isScopeLocked by vm.isScopeLocked.collectAsState()
    val isTypeLocked by vm.isTypeLocked.collectAsState()
    AdaptiveFormLayout {
        OutlinedTextField(name, vm::setName, label = {
            Text("name")
        }, modifier = Modifier.fillMaxWidth())

        TitleTypeSelector(titleType, vm::setTitleType, readOnly = isTypeLocked)

        val shape = RoundedCornerShape(10.dp)
        TitleScopeEditor(shape, vm::openScopeSheet, titleScope, vm::clearTitleScope, readOnly = isScopeLocked)

        ReceiverEditor(shape, vm::openReceiverSheet, receiver)

        ExpireTimeEditor(vm)

        DescriptionEditor(vm)
    }
}

@Composable
private fun ExpireTimeEditor(vm: TitleComposeViewModel) {
    val expiresAtText by vm.expiresAtText.collectAsState()
    var showExpireDialog by remember { mutableStateOf(false) }
    val expireShape = RoundedCornerShape(12.dp)
    Column(
        modifier =
        Modifier.clip(expireShape)
            .background(MaterialTheme.colorScheme.primaryContainer, expireShape)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Expire Time (UTC)", modifier = Modifier.weight(1f))
            IconButton({ showExpireDialog = true }) {
                Icon(Icons.Default.Edit, "edit expire time")
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
        Spacer(modifier = Modifier.height(8.dp))
        if (expiresAtText.isBlank()) {
            Text("Permanent", fontStyle = FontStyle.Italic, fontSize = 10.sp)
        } else {
            Text(expiresAtText, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Pick a UTC date and time, or clear it for permanent", fontSize = 10.sp)
    }
}

@Suppress("LongMethod")
@Composable
private fun ExpireTimePickerDialog(
    show: Boolean,
    init: String,
    dismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onClear: () -> Unit,
) {
    if (!show) {
        return
    }
    val initialValue =
        remember(show, init) {
            captureFailure(TitleActionError::from) {
                LocalDateTime.parse(init)
            }.getOrElse {
                now()
            }
        }
    var year by remember(show, init) { mutableStateOf(initialValue.year.toString()) }
    var month by remember(show, init) { mutableStateOf(initialValue.month.number.toString()) }
    var day by remember(show, init) { mutableStateOf(initialValue.day.toString()) }
    var hour by remember(show, init) { mutableStateOf(initialValue.hour.toString()) }
    var minute by remember(show, init) { mutableStateOf(initialValue.minute.toString()) }
    var second by remember(show, init) { mutableStateOf(initialValue.second.toString()) }
    var errorMessage by remember(show, init) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            TextButton(onClick = {
                val candidate = buildExpireTimeText(year, month, day, hour, minute, second)
                captureFailure(TitleActionError::from) {
                    LocalDateTime.parse(candidate)
                }.onSuccess {
                    onConfirm(candidate)
                }.onFailure {
                    errorMessage = "Invalid date/time"
                }
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
                TextButton(onClick = dismiss) {
                    Text("Cancel")
                }
            }
        },
        text = {
            ExpireTimeInputFields(
                year = year,
                onYearChange = { year = it },
                month = month,
                onMonthChange = { month = it },
                day = day,
                onDayChange = { day = it },
                hour = hour,
                onHourChange = { hour = it },
                minute = minute,
                onMinuteChange = { minute = it },
                second = second,
                onSecondChange = { second = it },
                errorMessage = errorMessage,
            )
        },
    )
}

@Composable
private fun ExpireTimeInputFields(
    year: String,
    onYearChange: (String) -> Unit,
    month: String,
    onMonthChange: (String) -> Unit,
    day: String,
    onDayChange: (String) -> Unit,
    hour: String,
    onHourChange: (String) -> Unit,
    minute: String,
    onMinuteChange: (String) -> Unit,
    second: String,
    onSecondChange: (String) -> Unit,
    errorMessage: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pick expiration date and time (UTC)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateTimeNumberField("Year", year, 4, Modifier.weight(1.4f), onYearChange)
            DateTimeNumberField("Month", month, 2, Modifier.weight(1f), onMonthChange)
            DateTimeNumberField("Day", day, 2, Modifier.weight(1f), onDayChange)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DateTimeNumberField("Hour", hour, 2, Modifier.weight(1f), onHourChange)
            DateTimeNumberField("Minute", minute, 2, Modifier.weight(1f), onMinuteChange)
            DateTimeNumberField("Second", second, 2, Modifier.weight(1f), onSecondChange)
        }
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DateTimeNumberField(
    label: String,
    value: String,
    maxLength: Int,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {
            onValueChange(it.filter(Char::isDigit).take(maxLength))
        },
        label = {
            Text(label)
        },
        modifier = modifier,
        singleLine = true,
    )
}

private fun buildExpireTimeText(
    year: String,
    month: String,
    day: String,
    hour: String,
    minute: String,
    second: String,
): String {
    val safeYear = year.padStart(4, '0')
    val safeMonth = month.padStart(2, '0')
    val safeDay = day.padStart(2, '0')
    val safeHour = hour.padStart(2, '0')
    val safeMinute = minute.padStart(2, '0')
    val safeSecond = second.padStart(2, '0')
    return "$safeYear-$safeMonth-$safeDay" +
        "T$safeHour:$safeMinute:$safeSecond"
}

@Composable
private fun DescriptionEditor(vm: TitleComposeViewModel) {
    val content by vm.content.collectAsState()
    var showDescriptionDialog by remember { mutableStateOf(false) }
    val descriptionShape = RoundedCornerShape(12.dp)
    Column(
        modifier =
        Modifier.clip(descriptionShape)
            .background(MaterialTheme.colorScheme.primaryContainer, descriptionShape)
            .clickable {
            }
            .padding(10.dp),
    ) {
        Row {
            Text("Description", modifier = Modifier.weight(1f))
            IconButton({ showDescriptionDialog = true }) {
                Icon(Icons.Default.Edit, "edit")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        InputDialog(showDescriptionDialog, content, { showDescriptionDialog = false }) { newValue ->
            vm.setContent(newValue)
            showDescriptionDialog = false
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (content.isEmpty()) {
            Text("No Description", fontStyle = FontStyle.Italic, fontSize = 10.sp)
        } else {
            AppTopicContentView(
                TopicDetail.EMPTY.copy(
                    topicInfo = TopicDetail.EMPTY.topicInfo.copy(content = TopicContent.Plain(content)),
                ),
            )
        }
    }
}

@Composable
private fun ReceiverEditor(shape: RoundedCornerShape, showReceiverSheet: () -> Unit, receiver: PrimaryKey?) {
    Row(
        modifier =
        Modifier.height(78.dp).fillMaxWidth().clip(shape).clickable {
            showReceiverSheet()
        }.background(MaterialTheme.colorScheme.primaryContainer, shape)
            .padding(8.dp),
    ) {
        receiver.let {
            if (it != null) {
                UserRefCell(it)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("接受人")
                }
            }
        }
    }
}

@Composable
private fun TitleScopeEditor(
    shape: RoundedCornerShape,
    showScopeSheet: () -> Unit,
    titleScope: ObjectTuple?,
    updateTitleScope: () -> Unit,
    readOnly: Boolean = false,
) {
    Row(
        modifier =
        Modifier
            .height(90.dp)
            .fillMaxWidth()
            .clip(shape)
            .then(if (readOnly) Modifier else Modifier.clickable { showScopeSheet() })
            .background(MaterialTheme.colorScheme.primaryContainer, shape)
            .padding(8.dp),
    ) {
        if (titleScope != null) {
            when (titleScope.objectType) {
                ObjectType.COMMUNITY ->
                    CommunityRefCell(titleScope.objectId) {
                        if (!readOnly) updateTitleScope()
                    }

                ObjectType.ROOM ->
                    RoomRefCell(titleScope.objectId) {
                        if (!readOnly) updateTitleScope()
                    }

                ObjectType.TOPIC -> Unit

                ObjectType.USER -> UserRefCell(titleScope.objectId)

                ObjectType.TITLE -> Unit

                ObjectType.FILE -> Unit

                ObjectType.PANEL_ACCOUNT -> Unit
            }
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("作用范围")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TitleTypeSelector(titleType: TitleType, updateTitleType: (TitleType) -> Unit, readOnly: Boolean = false) {
    var expanded by remember {
        mutableStateOf(false)
    }
    if (readOnly) {
        TextField(
            titleType.name,
            {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text("Title Type") },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
    } else {
        ExposedDropdownMenuBox(expanded, {
            expanded = true
        }, modifier = Modifier) {
            TextField(
                // The `menuAnchor` modifier must be passed to the text field to handle
                // expanding/collapsing the menu on click. A read-only text field has
                // the anchor type `PrimaryNotEditable`.
                titleType.name,
                {
                },
                modifier =
                Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                readOnly = true,
                label = { Text("Title Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(expanded, {
                expanded = false
            }, modifier = Modifier) {
                DropdownMenuItem({
                    Text("Regular")
                }, {
                    expanded = false
                    updateTitleType(TitleType.REGULAR)
                })
                DropdownMenuItem({
                    Text("Join")
                }, {
                    expanded = false
                    updateTitleType(TitleType.JOIN)
                })
            }
        }
    }
}

private suspend fun AppGlobalDialogController.createTitle(
    newTitle: NewTitle,
): CustomResult<TitleDetail, TitleActionError> =
    request {
        createTitle(newTitle)
    }.onSuccess { title ->
        emitEvent(OnTitleCreated(title))
    }.mapError(TitleActionError::fromError)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectPicker(
    showSheet: Boolean,
    sheetState: SheetState,
    supportObjectType: List<ObjectType>,
    hideSheet: () -> Unit,
    onCheck: (ObjectTuple) -> Unit,
) {
    BaseSheet(showSheet, sheetState, hideSheet) {
        var input by remember {
            mutableStateOf("")
        }
        Column(modifier = Modifier.height(300.dp).padding(top = 20.dp)) {
            var currentType by remember {
                mutableStateOf(supportObjectType[0])
            }
            TypeSelector(supportObjectType, currentType, {
                currentType = it
            }, input) {
                input = it
            }
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TypeSelector(
    supportObjectType: List<ObjectType>,
    currentType: ObjectType,
    setType: (ObjectType) -> Unit,
    input: String,
    setInput: (String) -> Unit,
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    val closeTypeSelector = {
        expanded = false
    }
    Row(
        modifier = Modifier.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shape = RoundedCornerShape(10.dp)
        Row(
            modifier =
            Modifier.background(MaterialTheme.colorScheme.primaryContainer, shape)
                .clip(shape)
                .clickable {
                    expanded = true
                }.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                when (currentType) {
                    ObjectType.COMMUNITY -> Icons.Default.Diversity1
                    ObjectType.ROOM -> Icons.AutoMirrored.Default.Chat
                    ObjectType.TOPIC -> Icons.Default.Topic
                    ObjectType.USER -> Icons.Default.AccountBox
                    ObjectType.TITLE -> Icons.Default.Badge
                    ObjectType.FILE -> Icons.AutoMirrored.Default.InsertDriveFile
                    ObjectType.PANEL_ACCOUNT -> Icons.Default.AdminPanelSettings
                },
                "icon",
            )
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }
        OutlinedTextField(input, setInput, modifier = Modifier.weight(1f))
    }

    DropdownMenu(expanded, closeTypeSelector) {
        supportObjectType.forEach {
            DropdownMenuItem({
                Text(it.name)
            }, {
                closeTypeSelector()
                setType(it)
            })
        }
    }
}

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
                CommunityList(communitiesViewModel, onClickCommunity)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeMenu(showSheet: Boolean, sheetState: SheetState, hideSheet: () -> Unit, onCheck: (ObjectType) -> Unit) {
    BaseSheet(showSheet, sheetState, hideSheet) {
        Column(
            modifier = Modifier.height(300.dp).padding(top = 20.dp).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ComposeMenuItem(Icons.Default.Diversity3, "create community") {
                onCheck(ObjectType.COMMUNITY)
            }

            ComposeMenuItem(Icons.Default.ChatBubble, "create room") {
                onCheck(ObjectType.ROOM)
            }

            ComposeMenuItem(Icons.Default.Badge, "create title") {
                onCheck(ObjectType.TITLE)
            }
        }
    }
}

@Composable
fun ComposeMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier =
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, shape).clip(shape)
            .clickable(onClick = onClick).appiumSemantics(text = title, onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, title)
        Text(title)
    }
}
