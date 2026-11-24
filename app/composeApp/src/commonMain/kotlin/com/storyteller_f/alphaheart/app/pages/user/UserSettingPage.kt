/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import com.attafitamim.krop.core.crop.RectCropShape
import com.attafitamim.krop.core.crop.cropperStyle
import com.attafitamim.krop.core.crop.rememberImageCropper
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnUserUpdated
import com.storyteller_f.alphaheart.app.components.ReadingImageCropperDialog
import com.storyteller_f.alphaheart.app.components.ReadingSettingsLayout
import com.storyteller_f.alphaheart.app.components.ReadingSettingsSection
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.components.SettingOptionView
import com.storyteller_f.alphaheart.app.components.SettingValueRow
import com.storyteller_f.alphaheart.app.edit
import com.storyteller_f.alphaheart.app.field_alias
import com.storyteller_f.alphaheart.app.field_avatar
import com.storyteller_f.alphaheart.app.field_name
import com.storyteller_f.alphaheart.app.field_not_set
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.topic.FilePicker
import com.storyteller_f.alphaheart.app.settings_aid
import com.storyteller_f.alphaheart.app.settings_nickname
import com.storyteller_f.alphaheart.app.settings_personal
import com.storyteller_f.alphaheart.app.settings_profile
import com.storyteller_f.alphaheart.app.settings_security
import com.storyteller_f.alphaheart.app.two_factor_authentication
import com.storyteller_f.alphaheart.app.user.SelectedMediaContext
import com.storyteller_f.alphaheart.app.user.SettingOption
import com.storyteller_f.alphaheart.app.user.getMediaTarget
import com.storyteller_f.alphaheart.app.user.getRatio
import com.storyteller_f.alphaheart.app.user.processSelectedMedia
import com.storyteller_f.alphaheart.app.user.settingPickerTitle
import com.storyteller_f.alphaheart.app.user.showFilePicker
import com.storyteller_f.alphaheart.app.user.updateUser
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.core.updateUserInfo
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.UpdateUserBody
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog as BasicAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingPage() {
    var currentOption by remember {
        mutableStateOf<SettingOption?>(null)
    }
    val nav = LocalAppNavFactory.current
    val my = LocalUserInfo.current ?: return
    val showDialog = { option: SettingOption ->
        currentOption = option
    }
    val closeDialog = {
        currentOption = null
    }
    val globalDialogController = LocalGlobalDialog.current
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.settings_personal)) }) { padding ->
        UserSettingInternal(padding, showDialog, { nav.newAppNav().gotoTwoFactorSettings() }, my)
        val scope = rememberCoroutineScope()
        ObjectSettingDialog(closeDialog, currentOption, {
            scope.launch {
                globalDialogController.useResult {
                    request {
                        updateUserInfo(UpdateUserBody(avatar = it.id))
                    }
                }.onSuccess { newInfo ->
                    globalDialogController.context.sessionManager.model.updateUser(newInfo)
                    closeDialog()
                }
            }
        }) {
            scope.launch {
                updateUser(currentOption, it, globalDialogController, closeDialog)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectSettingDialog(
    closeDialog: () -> Unit,
    currentOption: SettingOption?,
    onInputMedia: (FileInfo) -> Unit,
    onInputString: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()

    val ratio = getRatio(currentOption)
    val imageCropper = rememberImageCropper()
    val cropState = imageCropper.cropState
    if (cropState != null) {
        ReadingImageCropperDialog(
            state = cropState,
            style = cropperStyle(shapes = listOf(RectCropShape), aspects = listOf(ratio)),
        )
    }

    val myInfo = LocalUserInfo.current
    val mediaTarget = getMediaTarget(currentOption, myInfo)
    val globalDialogController = LocalGlobalDialog.current
    val alertDialogController = rememberAlertDialogController()
    val showSheet = showFilePicker(currentOption)
    val isFontOption =
        currentOption is SettingOption.ContentFont ||
            currentOption is SettingOption.CodeFont
    FilePicker(
        showSheet,
        mediaTarget,
        listOf("files"),
        requiredDimension = if (isFontOption) null else Dimension(ratio.x, ratio.y),
        imageOnly = !isFontOption,
        allowUpload = !isFontOption,
        title = stringResource(settingPickerTitle(currentOption)),
        onClickItems = { mediaList ->
            if (isFontOption) {
                onInputMedia(mediaList.first())
            } else {
                processSelectedMedia(
                    mediaList,
                    ratio,
                    SelectedMediaContext(
                        scope,
                        context,
                        imageCropper,
                        mediaTarget,
                        globalDialogController,
                        alertDialogController,
                        onInputMedia,
                    ),
                )
            }
        },
    ) {
        closeDialog()
    }

    SettingTextInput(currentOption, showSheet, closeDialog, onInputString)

    CustomAlertDialog(alertDialogController, {
        alertDialogController.close()
    }) {
    }
}

@Composable
private fun SettingTextInput(
    currentOption: SettingOption?,
    showSheet: Boolean,
    closeDialog: () -> Unit,
    onInputString: (String) -> Unit,
) {
    currentOption?.let {
        val showInput = !showSheet
        val title =
            stringResource(
                when (it) {
                    is SettingOption.Name -> Res.string.field_name
                    is SettingOption.Aid -> Res.string.field_alias
                    else -> Res.string.edit
                },
            )
        InputDialog(showInput, it.value.orEmpty(), title, closeDialog, onConfirm = onInputString)
    }
}

@Composable
private fun UserSettingInternal(
    values: PaddingValues,
    showDialog: (SettingOption) -> Unit,
    showTwoFactor: () -> Unit,
    m: UserInfo,
) {
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    ReadingSettingsLayout(Modifier.padding(values)) {
        ReadingSettingsSection(stringResource(Res.string.settings_profile)) {
            SettingOptionResettableView(
                stringResource(Res.string.field_avatar),
                m.avatar != null,
                {
                    if (it) {
                        scope.launch {
                            globalDialogController.useResult {
                                val body = UpdateUserBody(avatar = 0)
                                request { updateUserInfo(body) }
                            }.onSuccess { newInfo ->
                                globalDialogController.emitEvent(OnUserUpdated(newInfo))
                            }
                        }
                    } else {
                        showDialog(SettingOption.Icon(m.avatar?.fullName))
                    }
                },
                {
                    UserIconWithDialog(m, setClickEvent = false)
                },
            )
            SettingValueRow(stringResource(Res.string.settings_nickname), m.nickname, onEdit = {
                showDialog(SettingOption.Name(m.nickname))
            })
            val aid = m.aid
            val setAid = { showDialog(SettingOption.Aid(null)) }
            SettingValueRow(
                stringResource(Res.string.settings_aid),
                aid ?: stringResource(Res.string.field_not_set),
                onEdit = setAid.takeIf { aid == null },
            )
        }
        ReadingSettingsSection(stringResource(Res.string.settings_security)) {
            SettingOptionView(
                stringResource(Res.string.two_factor_authentication),
                showTwoFactor,
                showDivider = false,
            ) {
                Icon(actionIcon(ActionGlyph.CHEVRON_RIGHT), null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDialog(
    show: Boolean,
    init: String,
    title: String,
    dismiss: () -> Unit,
    minLines: Int = 1,
    showCancel: Boolean = true,
    inputLabel: String? = null,
    onConfirm: (String) -> Unit,
) {
    if (show) {
        var input by rememberSaveable { mutableStateOf(init) }
        BasicAlertDialog(dismiss) {
            DialogContainer(onDismissRequest = dismiss) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 40.dp, bottom = 20.dp),
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = inputLabel?.let { label -> { Text(label) } },
                    minLines = minLines,
                    maxLines = 10,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    if (showCancel) {
                        TextButton(dismiss) { Text(CoreStrings.cancel()) }
                    }
                    Button({ onConfirm(input) }) { Text(CoreStrings.ok()) }
                }
            }
        }
    }
}
