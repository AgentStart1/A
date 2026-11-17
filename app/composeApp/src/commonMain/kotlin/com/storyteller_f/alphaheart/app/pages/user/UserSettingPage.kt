/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.close
import com.storyteller_f.alphaheart.app.common.OnUserUpdated
import com.storyteller_f.alphaheart.app.components.SettingOptionResettableView
import com.storyteller_f.alphaheart.app.components.SettingOptionView
import com.storyteller_f.alphaheart.app.copy_auth_uri
import com.storyteller_f.alphaheart.app.download_recovery_codes
import com.storyteller_f.alphaheart.app.edit
import com.storyteller_f.alphaheart.app.enable_action
import com.storyteller_f.alphaheart.app.error.AvatarUpdateError
import com.storyteller_f.alphaheart.app.field_alias
import com.storyteller_f.alphaheart.app.field_avatar
import com.storyteller_f.alphaheart.app.field_name
import com.storyteller_f.alphaheart.app.field_not_set
import com.storyteller_f.alphaheart.app.pages.AdaptiveFormLayout
import com.storyteller_f.alphaheart.app.pages.ReadingPageToolbar
import com.storyteller_f.alphaheart.app.pages.topic.FilePicker
import com.storyteller_f.alphaheart.app.pages.topic.upload
import com.storyteller_f.alphaheart.app.parent_topic_load_failed_retry
import com.storyteller_f.alphaheart.app.select_font_file
import com.storyteller_f.alphaheart.app.select_image
import com.storyteller_f.alphaheart.app.settings
import com.storyteller_f.alphaheart.app.totp_code
import com.storyteller_f.alphaheart.app.totp_secret
import com.storyteller_f.alphaheart.app.two_factor_authentication
import com.storyteller_f.alphaheart.app.two_factor_disabled
import com.storyteller_f.alphaheart.app.two_factor_enabled
import com.storyteller_f.alphaheart.app.utils.saveTextToFile
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.DialogContainer
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialogState
import com.storyteller_f.alphaheart.client.compose_core.components.GlobalDialogStateProgress
import com.storyteller_f.alphaheart.client.compose_core.components.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingImageCropperDialog
import com.storyteller_f.alphaheart.client.compose_core.components.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.components.copyText
import com.storyteller_f.alphaheart.client.compose_core.components.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.ImageFormat
import com.storyteller_f.alphaheart.client.compose_core.utils.encodeImageBitmap
import com.storyteller_f.alphaheart.client.compose_core.utils.getRemoteImageBitmap
import com.storyteller_f.alphaheart.client.core.disableTwoFactor
import com.storyteller_f.alphaheart.client.core.enableTotp
import com.storyteller_f.alphaheart.client.core.generateRecoveryCodes
import com.storyteller_f.alphaheart.client.core.getTwoFactorSettings
import com.storyteller_f.alphaheart.client.core.setupTotp
import com.storyteller_f.alphaheart.client.core.updateUserInfo
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
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
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingBasicAlertDialog as BasicAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

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
    Scaffold(topBar = { ReadingPageToolbar(stringResource(Res.string.settings)) }) { padding ->
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
        sheetState,
        mediaTarget,
        listOf("files"),
        requiredDimension = if (isFontOption) null else Dimension(ratio.x, ratio.y),
        imageOnly = !isFontOption,
        title = stringResource(if (isFontOption) Res.string.select_font_file else Res.string.select_image),
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
        context.globalDialogController.cropImage(
            context.platformContext,
            info,
            context.imageCropper,
            context.mediaTarget,
        ).onSuccess {
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
): CustomResult<FileInfo?, AvatarUpdateError> =
    runImageCropStages(
        load = {
            getRemoteImageBitmap(this.context.sessionManager, context, info)
                ?.mapError(AvatarUpdateError::fromError)
                ?: CustomResult.Failure(AvatarUpdateError.from(Exception("download")))
        },
        crop = { bitmap ->
            when (val result = imageCropper.crop(ImageBitmapSrc(bitmap))) {
                CropResult.Cancelled -> null
                is CropError -> error(result.name)
                is CropResult.Success -> result.bitmap
            }
        },
        save = { bitmap ->
            encodeImageBitmap(bitmap, ImageFormat.PNG).mapError(AvatarUpdateError::fromError)
        },
        upload = { bytes ->
            useResult {
                val data = prepareCroppedImageUpload(info.name, bytes)
                upload(mediaTarget, data) { position, total ->
                    emitProgress { GlobalDialogState.Loading(progress = GlobalDialogStateProgress(position, total)) }
                }
            }.map { it.first() }.mapError(AvatarUpdateError::fromError)
        },
    )

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
        SettingOptionView(stringResource(Res.string.field_name), {
            showDialog(SettingOption.Name(m.nickname))
        }, content = {
            Text(m.nickname, textDecoration = TextDecoration.Underline)
        })
        val aid = m.aid
        SettingOptionView(stringResource(Res.string.field_alias), {
            if (aid == null) {
                showDialog(SettingOption.Aid(aid))
            } else {
                toasterState.showMessage("forbid")
            }
        }, content = {
            if (aid == null) {
                Text(stringResource(Res.string.field_not_set), textDecoration = TextDecoration.Underline)
            } else {
                Text(aid)
            }
        })
        SettingOptionView(stringResource(Res.string.two_factor_authentication), showTwoFactor, showDivider = false) {
            Icon(Icons.Default.ChevronRight, null)
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

    var loadFailure by remember { mutableStateOf<Throwable?>(null) }
    var loadAttempt by remember { mutableStateOf(0) }
    LaunchedEffect(loadAttempt) {
        loadFailure = null
        globalDialogController.request { getTwoFactorSettings() }
            .onSuccess { settings = it }.onFailure { loadFailure = it.cause }
    }
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

    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    BasicAlertDialog(dismiss) {
        TwoFactorDialogSurface(
            compact = windowWidth <= 600.dp,
            settings = settings,
            setupInfo = setupInfo,
            code = code,
            onCodeChange = { code = it },
            actions = actions,
            loadFailed = loadFailure != null,
            onRetry = { loadAttempt += 1 },
            dismiss = dismiss,
        )
    }
}

@Composable
internal fun TwoFactorDialogSurface(
    compact: Boolean,
    settings: TwoFactorSettingsInfo?,
    setupInfo: TotpSetupInfo?,
    code: String,
    onCodeChange: (String) -> Unit,
    actions: TwoFactorActions,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    dismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth().testTag("two-factor-surface"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(1.dp).padding(if (compact) 18.dp else 20.dp).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().height(29.dp)) {
                Text(
                    stringResource(Res.string.two_factor_authentication),
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(end = 44.dp).align(Alignment.CenterStart),
                )
                ReadingIconButton(dismiss, Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        actionIcon(ActionGlyph.X),
                        stringResource(Res.string.close),
                        Modifier.size(17.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (settings == null) {
                Box(
                    Modifier.fillMaxWidth().height(44.dp).testTag("two-factor-status"),
                    contentAlignment = Alignment.Center,
                ) {
                    if (loadFailed) {
                        TextButton(onRetry) { Text(stringResource(Res.string.parent_topic_load_failed_retry)) }
                    } else {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            } else {
                TwoFactorDialogContent(
                    settings, setupInfo, code, onCodeChange, actions.setupTotp, actions.disableTwoFactor,
                    actions.copyUri, actions.enableTotp, actions.downloadRecoveryCodes,
                )
            }
        }
    }
}

internal data class TwoFactorActions(
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        val enabled = settings?.enabled == true
        TwoFactorSwitchRow(enabled, onSetup, onDisable)
        setupInfo?.let {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                TotpSetupView(it, code, onCodeChange, onCopyUri, onEnable)
            }
        }
        if (enabled) {
            Button(onDownloadCodes, Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.download_recovery_codes))
            }
        }
    }
}

@Composable
private fun TwoFactorSwitchRow(enabled: Boolean, onSetup: () -> Unit, onDisable: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp).testTag("two-factor-status"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val label = stringResource(if (enabled) Res.string.two_factor_enabled else Res.string.two_factor_disabled)
        Text(label, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(44.dp).semantics { contentDescription = label }.toggleable(
                value = enabled,
                role = Role.Switch,
                onValueChange = { checked -> if (checked) onSetup() else onDisable() },
            ).testTag("two-factor-toggle"),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                Modifier.size(20.dp).testTag("two-factor-checkbox")
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                        RoundedCornerShape(2.dp),
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (enabled) {
                    Icon(Icons.Default.Check, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
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
    Text(stringResource(Res.string.totp_secret, info.secret))
    Button({
        onCopyUri(info.otpauthUri)
    }) {
        Text(stringResource(Res.string.copy_auth_uri))
    }
    OutlinedTextField(code, onCodeChange, label = {
        Text(stringResource(Res.string.totp_code))
    })
    Button(onEnable) {
        Text(stringResource(Res.string.enable_action))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputDialog(
    show: Boolean,
    init: String,
    title: String,
    dismiss: () -> Unit,
    minLines: Int = 1,
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
                    minLines = minLines,
                    maxLines = 10,
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    TextButton(dismiss) { Text(CoreStrings.cancel()) }
                    Button({ onConfirm(input) }) { Text(CoreStrings.ok()) }
                }
            }
        }
    }
}
