/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import coil3.Image
import coil3.request.ImageRequest
import coil3.toBitmap
import com.storyteller_f.alphaheart.client.compose_core.error.ImageError
import com.storyteller_f.alphaheart.client.compose_core.utils.ImageFormat.JPEG
import com.storyteller_f.alphaheart.client.compose_core.utils.ImageFormat.PNG
import com.storyteller_f.alphaheart.client.compose_core.utils.ImageFormat.WEBP
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import kotlinx.io.files.Path
import org.jetbrains.skia.EncodedImageFormat

actual fun Image.coilImageToImageBitmap(): CustomResult<ImageBitmap, ImageError> =
    captureFailure(ImageError::from) {
        toBitmap().asComposeImageBitmap()
    }

actual fun saveImageBitmap(
    imageBitmap: ImageBitmap,
    path: String,
    format: ImageFormat,
    quality: Int,
): CustomResult<Path, ImageError> =
    captureFailure(ImageError::from) {
        val data = imageBitmapToByteArray(imageBitmap, format, quality)
        writeImageFile(path, data)
    }

fun imageBitmapToByteArray(imageBitmap: ImageBitmap, format: ImageFormat = PNG, quality: Int = 100): ByteArray {
    // format.skiaEncodedFormat
    val skiaImage = org.jetbrains.skia.Image.makeFromBitmap(imageBitmap.asSkiaBitmap())
    return skiaImage.encodeToData(
        when (format) {
            PNG -> EncodedImageFormat.PNG
            JPEG -> EncodedImageFormat.JPEG
            WEBP -> EncodedImageFormat.WEBP
        },
        quality,
    )?.bytes ?: ByteArray(0)
}

actual fun ImageRequest.Builder.androidAllowHardware(b: Boolean): ImageRequest.Builder = this
