/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image
import coil3.request.ImageRequest
import com.storyteller_f.alphaheart.client.compose_core.error.ImageError
import com.storyteller_f.result.CustomResult
import kotlinx.io.files.Path

actual fun Image.coilImageToImageBitmap(): CustomResult<ImageBitmap, ImageError> =
    CustomResult.Failure(ImageError.from(UnsupportedOperationException("not supported on wasm")))

actual fun ImageRequest.Builder.androidAllowHardware(b: Boolean): ImageRequest.Builder = this

actual fun saveImageBitmap(
    imageBitmap: ImageBitmap,
    path: String,
    format: ImageFormat,
    quality: Int,
): CustomResult<Path, ImageError> =
    CustomResult.Failure(ImageError.from(UnsupportedOperationException("not supported on wasm")))
