/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.ui.graphics.ImageBitmap
import coil3.Image
import coil3.request.ImageRequest
import com.storyteller_f.alphaheart.client.compose_core.error.ImageError
import com.storyteller_f.result.CustomResult
import io.ktor.utils.io.core.*
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemTemporaryDirectory

expect fun Image.coilImageToImageBitmap(): CustomResult<ImageBitmap, ImageError>

expect fun ImageRequest.Builder.androidAllowHardware(b: Boolean): ImageRequest.Builder

enum class ImageFormat {
    PNG,
    JPEG,
    WEBP,
}

expect fun saveImageBitmap(
    imageBitmap: ImageBitmap,
    path: String,
    format: ImageFormat = ImageFormat.PNG,
    quality: Int = 90,
): CustomResult<Path, ImageError>

fun writeImageFile(filePath: String, data: ByteArray): Path {
    val path = Path(SystemTemporaryDirectory, filePath)
    SystemFileSystem.sink(path).buffered().use {
        it.writeFully(data)
    }
    return path
}
