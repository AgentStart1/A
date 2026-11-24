/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.media

import com.storyteller_f.alphaheart.app.error.AvatarUpdateError
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.mapError
import kotlin.coroutines.cancellation.CancellationException

/** Loading belongs to I/O stages, never to the interactive cropper or an already managed upload. */
internal suspend fun <C, Image, Cropped, Saved, Uploaded> CustomGlobalDialogController<C>.runImageCropStages(
    load: suspend () -> CustomResult<Image, AvatarUpdateError>,
    crop: suspend (Image) -> Cropped?,
    save: suspend (Cropped) -> CustomResult<Saved, AvatarUpdateError>,
    upload: suspend (Saved) -> CustomResult<Uploaded?, AvatarUpdateError>,
): CustomResult<Uploaded?, AvatarUpdateError> {
    try {
        val loaded = useResult { load() }.mapError(AvatarUpdateError::fromError)
        val image =
            when (loaded) {
                is CustomResult.Success -> loaded.value
                is CustomResult.Failure -> return loaded
            }
        val cropped = crop(image) ?: return CustomResult.success(null)
        val saved = useResult { save(cropped) }.mapError(AvatarUpdateError::fromError)
        return when (saved) {
            is CustomResult.Success -> upload(saved.value)
            is CustomResult.Failure -> saved
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        // Preprocessing failures still receive an actionable dialog.
        val error = AvatarUpdateError.from(failure)
        return useResult<Uploaded?, AvatarUpdateError> {
            CustomResult.Failure(error)
        }.mapError(AvatarUpdateError::fromError)
    }
}
