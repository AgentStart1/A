/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import coil3.Image
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.storyteller_f.alphaheart.client.compose_core.error.ImageError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import kotlinx.io.Buffer
import kotlinx.io.asOutputStream
import kotlinx.io.readByteArray

actual fun Image.coilImageToImageBitmap(): CustomResult<ImageBitmap, ImageError> =
    captureFailure(
        ImageError::from,
    ) { toBitmap().asImageBitmap() }

actual fun encodeImageBitmap(
    imageBitmap: ImageBitmap,
    format: ImageFormat,
    quality: Int,
): CustomResult<ByteArray, ImageError> =
    captureFailure(ImageError::from) {
        val buffer = Buffer()
        buffer.asOutputStream().buffered().use { outputStream ->
            check(
                imageBitmap.asAndroidBitmap().compress(
                    when (format) {
                        ImageFormat.PNG -> Bitmap.CompressFormat.PNG

                        ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG

                        ImageFormat.WEBP ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                Bitmap.CompressFormat.WEBP_LOSSY
                            } else {
                                Bitmap.CompressFormat.PNG
                            }
                    },
                    quality,
                    outputStream,
                ),
            ) { "Image encoding failed: $format" }
        }
        buffer.readByteArray().also { require(it.isNotEmpty()) }
    }

actual fun ImageRequest.Builder.androidAllowHardware(b: Boolean): ImageRequest.Builder = allowHardware(b)
