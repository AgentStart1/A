/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.user

import coil3.PlatformContext
import com.attafitamim.krop.core.crop.AspectRatio
import com.attafitamim.krop.core.crop.CropError
import com.attafitamim.krop.core.crop.CropResult
import com.attafitamim.krop.core.crop.ImageCropper
import com.attafitamim.krop.core.crop.crop
import com.attafitamim.krop.core.images.ImageBitmapSrc
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.OnUserUpdated
import com.storyteller_f.alphaheart.app.error.AvatarUpdateError
import com.storyteller_f.alphaheart.app.media.prepareCroppedImageUpload
import com.storyteller_f.alphaheart.app.media.runImageCropStages
import com.storyteller_f.alphaheart.app.media.upload
import com.storyteller_f.alphaheart.app.select_code_font
import com.storyteller_f.alphaheart.app.select_content_font
import com.storyteller_f.alphaheart.app.select_image
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogState
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogStateProgress
import com.storyteller_f.alphaheart.client.compose_core.tasks.emitEvent
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.utils.ImageFormat
import com.storyteller_f.alphaheart.client.compose_core.utils.encodeImageBitmap
import com.storyteller_f.alphaheart.client.compose_core.utils.getRemoteImageBitmap
import com.storyteller_f.alphaheart.client.core.updateUserInfo
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.checkMediaFileDimensionRatioMatch
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.UpdateUserBody
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

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

internal fun settingPickerTitle(option: SettingOption?) =
    when (option) {
        is SettingOption.ContentFont -> Res.string.select_content_font
        is SettingOption.CodeFont -> Res.string.select_code_font
        else -> Res.string.select_image
    }

internal fun getMediaTarget(currentOption: SettingOption?, my: UserInfo?): ObjectTuple =
    if (currentOption is SettingOption.RoomIcon && currentOption.roomId != null) {
        ObjectTuple(currentOption.roomId, ObjectType.ROOM)
    } else {
        ObjectTuple(my?.id ?: 0, ObjectType.USER)
    }

internal fun getRatio(currentOption: SettingOption?): AspectRatio {
    val dimension =
        when (currentOption) {
            is SettingOption.RoomIcon -> Dimension.ROOM_DIMENSION
            is SettingOption.Poster -> Dimension.COMMUNITY_POSTER
            else -> Dimension.DEFAULT_DIMENSION
        }
    return AspectRatio(dimension.width, dimension.height)
}

internal fun showFilePicker(currentOption: SettingOption?): Boolean =
    currentOption is SettingOption.Icon ||
        currentOption is SettingOption.Poster ||
        currentOption is SettingOption.RoomIcon ||
        currentOption is SettingOption.ContentFont ||
        currentOption is SettingOption.CodeFont

internal fun processSelectedMedia(mediaList: List<FileInfo>, ratio: AspectRatio, context: SelectedMediaContext) {
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

internal data class SelectedMediaContext(
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

internal suspend fun updateUser(
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
