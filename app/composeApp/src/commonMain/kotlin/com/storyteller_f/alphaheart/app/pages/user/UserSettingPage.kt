/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import com.attafitamim.krop.core.crop.AspectRatio
import com.attafitamim.krop.core.crop.CropError
import com.attafitamim.krop.core.crop.CropResult
import com.attafitamim.krop.core.crop.ImageCropper
import com.attafitamim.krop.core.crop.RectCropShape
import com.attafitamim.krop.core.crop.crop
import com.attafitamim.krop.core.crop.cropperStyle
import com.attafitamim.krop.core.crop.rememberImageCropper
import com.attafitamim.krop.core.images.ImageBitmapSrc
import com.attafitamim.krop.ui.ImageCropperDialog
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.common.OnUserUpdated
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.components.SettingOptionView
import com.storyteller_f.alphaheart.app.error.AvatarUpdateError
import com.storyteller_f.alphaheart.app.pages.AdaptiveFormLayout
import com.storyteller_f.alphaheart.app.pages.topic.FilePicker
import com.storyteller_f.alphaheart.app.pages.topic.uploadPath
import com.storyteller_f.alphaheart.app.utils.saveTextToFile
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.copyText
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.ImageFormat
import com.storyteller_f.alphaheart.client.compose_core.utils.getRemoteImageBitmap
import com.storyteller_f.alphaheart.client.compose_core.utils.saveImageBitmap
import com.storyteller_f.alphaheart.client.core.disableTwoFactor
import com.storyteller_f.alphaheart.client.core.enableTotp
import com.storyteller_f.alphaheart.client.core.generateRecoveryCodes
import com.storyteller_f.alphaheart.client.core.getTwoFactorSettings
import com.storyteller_f.alphaheart.client.core.setupTotp
import com.storyteller_f.alphaheart.client.core.updateUserInfo
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.TotpSetupInfo
import com.storyteller_f.shared.model.TwoFactorSettingsInfo
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.checkMediaFileDimensionRatioMatch
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.UpdateUserBody
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class SettingOption(open val value: String?) {
    data class Name(override val value: String?) : SettingOption(value)
    data class Aid(override val value: String?) : SettingOption(value)
    data class Icon(override val value: String?) : SettingOption(value)
    data class Poster(override val value: String?) : SettingOption(value)
    data class RoomIcon(override val value: String?, val roomId: PrimaryKey?) : SettingOption(value)
    data class ContentFont(override val value: String?) : SettingOption(value)
    data class CodeFont(override val value: String?) : SettingOption(value)
    data class JsonPreview(override val value: String?) : SettingOption(value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSettingPage() {
    var currentOption by remember {
        mutableStateOf<SettingOption?>(null)
    }
    var showTwoFactor by remember {
        mutableStateOf(false)
    }
    val sheetState = rememberModalBottomSheetState()
    val my = LocalUserInfo.current ?: return
    val showDialog = { option: SettingOption ->
        currentOption = option
    }
    val closeDialog = {
        currentOption = null
    }
    val globalDialogController = LocalGlobalDialog.current
    Scaffold { padding ->
        UserSettingInternal(padding, showDialog, {
            showTwoFactor = true
        }, my)
        if (showTwoFactor) {
            TwoFactorDialog {
                showTwoFactor = false
            }
        }
        val scope = rememberCoroutineScope()
        ObjectSettingDialog(closeDialog, currentOption, sheetState, {
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
    sheetState: SheetState,
    onInputMedia: (FileInfo) -> Unit,
    onInputString: (String) -> Unit,
) {
    val context = LocalPlatformContext.current
    val scope = rememberCoroutineScope()

    val ratio = getRatio(currentOption)
    val imageCropper = rememberImageCropper()
    val cropState = imageCropper.cropState
    if (cropState != null) {
        ImageCropperDialog(state = cropState, cropperStyle(shapes = listOf(RectCropShape), aspects = listOf(ratio)))
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
        sheetState,
        mediaTarget,
        listOf("files"),
        requiredDimension = Dimension(ratio.x, ratio.y),
        { mediaList ->
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

    currentOption?.let {
        val showInput = !showSheet
        InputDialog(showInput, it.value.orEmpty(), {
            closeDialog()
        }, onInputString)
    }
    CustomAlertDialog(alertDialogController, {
        alertDialogController.close()
    }) {
    }
}

private fun getMediaTarget(currentOption: SettingOption?, my: UserInfo?): ObjectTuple =
    if (currentOption is SettingOption.RoomIcon && currentOption.roomId != null) {
        ObjectTuple(currentOption.roomId, ObjectType.ROOM)
    } else {
        ObjectTuple(my?.id ?: 0, ObjectType.USER)
    }

private fun getRatio(currentOption: SettingOption?): AspectRatio {
    val dimension =
        when (currentOption) {
            is SettingOption.RoomIcon -> Dimension.ROOM_DIMENSION
            is SettingOption.Poster -> Dimension.COMMUNITY_POSTER
            else -> Dimension.DEFAULT_DIMENSION
        }
    return AspectRatio(dimension.width, dimension.height)
}

private fun showFilePicker(currentOption: SettingOption?): Boolean =
    currentOption is SettingOption.Icon ||
        currentOption is SettingOption.Poster ||
        currentOption is SettingOption.RoomIcon ||
        currentOption is SettingOption.ContentFont ||
        currentOption is SettingOption.CodeFont

private fun processSelectedMedia(mediaList: List<FileInfo>, ratio: AspectRatio, context: SelectedMediaContext) {
    val info = mediaList.first()
    val dimension = info.dimension
    if (dimension == null || !info.contentType.startsWith("image/")) {
        context.alertDialogController.showTitle("invalid image: ${info.contentType} ${dimension ?: "<none>"}")
        return
    }
    if (checkMediaFileDimensionRatioMatch(
            dimension,
            Dimension(ratio.x, ratio.y),
        )
    ) {
        context.onInputMedia(info)
        return
    }
    context.scope.launch {
        context.globalDialogController.useResult {
            cropImage(context.platformContext, info, context.imageCropper, context.mediaTarget)
        }.onSuccess {
            if (it != null) {
                context.onInputMedia(it)
            }
        }
    }
}

private data class SelectedMediaContext(
    val scope: CoroutineScope,
    val platformContext: PlatformContext,
    val imageCropper: ImageCropper,
    val mediaTarget: ObjectTuple,
    val globalDialogController: AppGlobalDialogController,
    val alertDialogController: CustomAlertDialogController,
    val onInputMedia: (FileInfo) -> Unit,
)

private suspend fun AppGlobalDialogController.cropImage(
    context: PlatformContext,
    info: FileInfo,
    imageCropper: ImageCropper,
    mediaTarget: ObjectTuple,
): CustomResult<FileInfo?, AvatarUpdateError> {
    val image =
        useResult {
            getRemoteImageBitmap(this.context.sessionManager, context, info)
                ?: CustomResult.Failure(AvatarUpdateError.from(Exception("download")))
        }
    return image.andThen(AvatarUpdateError::fromError) {
        when (val result = imageCropper.crop(ImageBitmapSrc(it))) {
            CropResult.Cancelled -> {
                CustomResult.success(null)
            }

            is CropError -> {
                CustomResult.Failure(AvatarUpdateError.from(Exception(result.name)))
            }

            is CropResult.Success -> {
                useResult {
                    val name = info.name.substringBeforeLast(".")
                    saveImageBitmap(
                        result.bitmap,
                        "tmpImage/$name-cropped.png",
                        when (info.contentType) {
                            "image/webp" -> ImageFormat.WEBP
                            "image/jpeg", "image/jpg" -> ImageFormat.JPEG
                            else -> ImageFormat.PNG
                        },
                    )
                }
            }
        }
    }.mapPresent {
        uploadPath(it, mediaTarget).getOrThrow()?.first()
    }.mapError(AvatarUpdateError::fromError)
}

@Composable
private fun UserSettingInternal(
    values: PaddingValues,
    showDialog: (SettingOption) -> Unit,
    showTwoFactor: () -> Unit,
    m: UserInfo,
) {
    val toasterState = LocalToaster.current
    val scope = rememberCoroutineScope()
    val globalDialogController = LocalGlobalDialog.current
    AdaptiveFormLayout(Modifier.padding(values)) {
        SettingOptionResettableView(
            "Icon",
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
        SettingOptionView("Name", {
            showDialog(SettingOption.Name(m.nickname))
        }, {
            Text(m.nickname, textDecoration = TextDecoration.Underline)
        })
        val aid = m.aid
        SettingOptionView("Aid", {
            if (aid == null) {
                showDialog(SettingOption.Aid(aid))
            } else {
                toasterState.showMessage("forbid")
            }
        }, {
            if (aid == null) {
                Text("undefined", textDecoration = TextDecoration.Underline)
            } else {
                Text(aid)
            }
        })
        SettingOptionView("2FA", showTwoFactor) {
            Text("Two-factor authentication", textDecoration = TextDecoration.Underline)
        }
    }
}

@Composable
private fun TwoFactorDialog(dismiss: () -> Unit) {
    val globalDialogController = LocalGlobalDialog.current
    val toaster = LocalToaster.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<TwoFactorSettingsInfo?>(null) }
    var setupInfo by remember { mutableStateOf<TotpSetupInfo?>(null) }
    var code by remember { mutableStateOf("") }

    LoadTwoFactorSettings(globalDialogController) { settings = it }
    val actions =
        scope.rememberTwoFactorActions(
            globalDialogController,
            toaster,
            clipboard,
            code,
            TwoFactorStateUpdates(
                setSettings = { settings = it },
                setSetupInfo = { setupInfo = it },
                setCode = { code = it },
            ),
        )

    AlertDialog(dismiss, {
        Button(dismiss) {
            Text(CoreStrings.ok())
        }
    }, title = {
        Text("Two-factor authentication")
    }, text = {
        TwoFactorDialogContent(
            settings,
            setupInfo,
            code,
            { code = it },
            actions.setupTotp,
            actions.disableTwoFactor,
            actions.copyUri,
            actions.enableTotp,
            actions.downloadRecoveryCodes,
        )
    })
}

@Composable
private fun LoadTwoFactorSettings(
    globalDialogController: AppGlobalDialogController,
    onSuccess: (TwoFactorSettingsInfo) -> Unit,
) {
    LaunchedEffect(Unit) {
        globalDialogController.useResult {
            request {
                getTwoFactorSettings()
            }
        }.onSuccess(onSuccess)
    }
}

private data class TwoFactorActions(
    val setupTotp: () -> Unit,
    val disableTwoFactor: () -> Unit,
    val copyUri: (String) -> Unit,
    val enableTotp: () -> Unit,
    val downloadRecoveryCodes: () -> Unit,
)

private data class TwoFactorStateUpdates(
    val setSettings: (TwoFactorSettingsInfo) -> Unit,
    val setSetupInfo: (TotpSetupInfo?) -> Unit,
    val setCode: (String) -> Unit,
)

private fun kotlinx.coroutines.CoroutineScope.rememberTwoFactorActions(
    globalDialogController: AppGlobalDialogController,
    toaster: com.storyteller_f.alphaheart.client.compose_core.components.Toast,
    clipboard: androidx.compose.ui.platform.Clipboard,
    code: String,
    stateUpdates: TwoFactorStateUpdates,
): TwoFactorActions {
    val (setSettings, setSetupInfo, setCode) = stateUpdates
    return TwoFactorActions(
        setupTotp = {
            setupTotp(globalDialogController) { setSetupInfo(it) }
        },
        disableTwoFactor = {
            disableTwoFactor(globalDialogController) {
                setSettings(it)
                setSetupInfo(null)
                setCode("")
            }
        },
        copyUri = { uri ->
            launch {
                clipboard.copyText(uri, toaster)
            }
        },
        enableTotp = {
            enableTotp(globalDialogController, code) {
                setSettings(it)
                setSetupInfo(null)
                setCode("")
            }
        },
        downloadRecoveryCodes = {
            downloadRecoveryCodes(globalDialogController)
        },
    )
}

@Composable
private fun TwoFactorDialogContent(
    settings: TwoFactorSettingsInfo?,
    setupInfo: TotpSetupInfo?,
    code: String,
    onCodeChange: (String) -> Unit,
    onSetup: () -> Unit,
    onDisable: () -> Unit,
    onCopyUri: (String) -> Unit,
    onEnable: () -> Unit,
    onDownloadCodes: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val enabled = settings?.enabled == true
        TwoFactorSwitchRow(enabled, onSetup, onDisable)
        setupInfo?.let {
            TotpSetupView(it, code, onCodeChange, onCopyUri, onEnable)
        }
        if (enabled) {
            Button(onDownloadCodes) {
                Text("Download recovery codes")
            }
        }
    }
}

@Composable
private fun TwoFactorSwitchRow(enabled: Boolean, onSetup: () -> Unit, onDisable: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (enabled) "Enabled" else "Disabled", modifier = Modifier.weight(1f))
        Switch(enabled, { checked ->
            if (checked) {
                onSetup()
            } else {
                onDisable()
            }
        })
    }
}

@Composable
private fun ColumnScope.TotpSetupView(
    info: TotpSetupInfo,
    code: String,
    onCodeChange: (String) -> Unit,
    onCopyUri: (String) -> Unit,
    onEnable: () -> Unit,
) {
    Image(
        rememberQrCodePainter(info.otpauthUri),
        "TOTP QR code",
        modifier = Modifier.size(220.dp).align(Alignment.CenterHorizontally),
    )
    Text("Secret: ${info.secret}")
    Button({
        onCopyUri(info.otpauthUri)
    }) {
        Text("Copy URI")
    }
    OutlinedTextField(code, onCodeChange, label = {
        Text("TOTP code")
    })
    Button(onEnable) {
        Text("Enable")
    }
}

private fun kotlinx.coroutines.CoroutineScope.setupTotp(
    globalDialogController: AppGlobalDialogController,
    onSuccess: (TotpSetupInfo) -> Unit,
) {
    launch {
        globalDialogController.useResult {
            request { setupTotp() }
        }.onSuccess(onSuccess)
    }
}

private fun kotlinx.coroutines.CoroutineScope.disableTwoFactor(
    globalDialogController: AppGlobalDialogController,
    onSuccess: (TwoFactorSettingsInfo) -> Unit,
) {
    launch {
        globalDialogController.useResult {
            request { disableTwoFactor() }
        }.onSuccess(onSuccess)
    }
}

private fun kotlinx.coroutines.CoroutineScope.enableTotp(
    globalDialogController: AppGlobalDialogController,
    code: String,
    onSuccess: (TwoFactorSettingsInfo) -> Unit,
) {
    launch {
        globalDialogController.useResult {
            request { enableTotp(code) }
        }.onSuccess(onSuccess)
    }
}

private fun kotlinx.coroutines.CoroutineScope.downloadRecoveryCodes(
    globalDialogController: AppGlobalDialogController,
) {
    launch {
        globalDialogController.useResult {
            request { generateRecoveryCodes() }
        }.onSuccess { response ->
            val text = response.recoveryCodes.joinToString("\n")
            saveTextToFile("recovery-codes", "txt", setOf("txt"), text)
        }
    }
}

private suspend fun updateUser(
    showInputDialog: SettingOption?,
    string: String,
    globalDialogController: AppGlobalDialogController,
    closeDialog: () -> Unit,
) {
    val body =
        when (showInputDialog) {
            is SettingOption.Name -> UpdateUserBody(nickname = string)
            is SettingOption.Aid -> UpdateUserBody(aid = string)
            else -> null
        } ?: return
    globalDialogController.useResult {
        request {
            updateUserInfo(body)
        }
    }.onSuccess { newInfo ->
        globalDialogController.context.sessionManager.model.updateUser(newInfo)
        globalDialogController.emitEvent(OnUserUpdated(newInfo))
        closeDialog()
    }
}

@Composable
fun InputDialog(show: Boolean, init: String, dismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var input by remember {
        mutableStateOf(init)
    }
    if (show) {
        AlertDialog(dismiss, {
            Button({
                onConfirm(input)
            }) {
                Text(CoreStrings.ok())
            }
        }, text = {
            OutlinedTextField(input, {
                input = it
            })
        })
    }
}
