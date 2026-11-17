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
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import org.jetbrains.skia.EncodedImageFormat

actual fun Image.coilImageToImageBitmap(): CustomResult<ImageBitmap, ImageError> =
    captureFailure(ImageError::from) { toBitmap().asComposeImageBitmap() }

actual fun ImageRequest.Builder.androidAllowHardware(b: Boolean): ImageRequest.Builder = this

actual fun encodeImageBitmap(
    imageBitmap: ImageBitmap,
    format: ImageFormat,
    quality: Int,
): CustomResult<ByteArray, ImageError> =
    captureFailure(ImageError::from) {
        val image = org.jetbrains.skia.Image.makeFromBitmap(imageBitmap.asSkiaBitmap())
        try {
            val encoded =
                requireNotNull(
                    image.encodeToData(
                        when (format) {
                            ImageFormat.PNG -> EncodedImageFormat.PNG
                            ImageFormat.JPEG -> EncodedImageFormat.JPEG
                            ImageFormat.WEBP -> EncodedImageFormat.WEBP
                        },
                        quality,
                    ),
                ) { "Image encoding failed: $format" }
            try {
                encoded.bytes.also { bytes -> require(bytes.isNotEmpty()) }
            } finally {
                encoded.close()
            }
        } finally {
            image.close()
        }
    }
