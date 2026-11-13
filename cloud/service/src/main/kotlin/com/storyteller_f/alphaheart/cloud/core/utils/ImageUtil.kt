/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.utils

import io.github.aakira.napier.Napier
import org.apache.commons.imaging.ImageFormats
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.io.FileUtils.copyFile
import java.awt.image.BufferedImage
import java.io.BufferedOutputStream
import java.io.File

fun cleanImageMeta(input: File, output: BufferedOutputStream, mimeType: String) {
    when (mimeType.lowercase()) {
        "image/jpeg", "image/jpg" -> {
            Napier.i("🧹 清除 $mimeType 元数据...", tag = "ImageUtil")
            ExifRewriter().removeExifMetadata(input, output)
        }

        "image/png", "image/bmp", "image/gif", "image/webp" -> {
            Napier.i("🧹 重写 $mimeType 图片以移除元数据...", tag = "ImageUtil")
            val image: BufferedImage = Imaging.getBufferedImage(input)
            Imaging.writeImage(image, output, Imaging.guessFormat(input))
        }

        "image/tif", "image/tiff" -> {
            Napier.i("🧹 清除 $mimeType 元数据...", tag = "ImageUtil")
            val image: BufferedImage = Imaging.getBufferedImage(input)
            Imaging.writeImage(image, output, ImageFormats.TIFF)
        }

        else -> {
            Napier.i("⚠️ 暂不支持该格式（$mimeType），仅复制文件。", tag = "ImageUtil")
            copyFile(input, output)
        }
    }
}
