/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.utils

import java.io.BufferedInputStream

fun <T> BufferedInputStream.readFlacAlbumFromAudioStream(saveAlbum: (ByteArray, String) -> T): Pair<T, String>? {
    val signature = ByteArray(4)
    read(signature)

    if (String(signature) != "fLaC") {
        error("Not a valid FLAC file")
    }

    while (true) {
        val header = read()
        val blockType = header and 0x7F
        val lengthBytes = ByteArray(3)
        read(lengthBytes)
        val blockLength =
            lengthBytes[0].toInt() and 0xFF shl 16 or
                (lengthBytes[1].toInt() and 0xFF shl 8) or
                (lengthBytes[2].toInt() and 0xFF)

        if (blockType == 6) {
            val pictureData = ByteArray(blockLength)
            var count = 0
            while (count < blockLength) {
                val r = read(pictureData, count, blockLength - count)
                if (r < 0) break
                count += r
            }
            if (count != blockLength) return null

            return pictureData.inputStream().buffered().use { dataInput ->
                fun readInt(): Int =
                    dataInput.read() shl 24 or (dataInput.read() shl 16) or
                        (dataInput.read() shl 8) or dataInput.read()

                readInt() // picture type
                val mimeLength = readInt()
                val mimeBytes = ByteArray(mimeLength)
                dataInput.read(mimeBytes)
                val mimeType = String(mimeBytes)

                val descLength = readInt()
                dataInput.skip(descLength.toLong())

                readInt() // width
                readInt() // height
                readInt() // color depth
                readInt() // indexed colors

                val picDataLength = readInt()
                val realImage = ByteArray(picDataLength)
                dataInput.read(realImage)

                saveAlbum(realImage, mimeType) to mimeType
            }
        } else {
            skip(blockLength.toLong())
        }

        if (header and 0x80 != 0) {
            break
        }
    }
    return null
}

@Suppress("CyclomaticComplexMethod")
fun <T> BufferedInputStream.readMp3AlbumFromAudioStream(saveAlbum: (ByteArray, String) -> T): Pair<T, String>? {
    // 读取 ID3 header（10 bytes）
    val header = ByteArray(10)
    if (read(header) != 10 ||
        header[0] != 'I'.code.toByte() ||
        header[1] != 'D'.code.toByte() ||
        header[2] != '3'.code.toByte()
    ) {
        error("不是有效的 ID3v2 标签")
    }

    // 计算标签总长度（同步安全整数：4 x 7bits）
    val tagSize = syncSafeInt(header.copyOfRange(6, 10))
    var totalRead = 0

    while (totalRead < tagSize) {
        val frameHeader = ByteArray(10)
        var countFrame = 0
        while (countFrame < 10) {
            val r = read(frameHeader, countFrame, 10 - countFrame)
            if (r < 0) break
            countFrame += r
        }
        if (countFrame != 10) break

        val frameId = String(frameHeader, 0, 4)
        val frameSize =
            frameHeader[4].toInt() and 0xFF shl 24 or
                (frameHeader[5].toInt() and 0xFF shl 16) or
                (frameHeader[6].toInt() and 0xFF shl 8) or
                (frameHeader[7].toInt() and 0xFF)

        totalRead += 10 + frameSize
        if (frameId != "APIC") {
            skip(frameSize.toLong())
            continue
        }

        val frameData = ByteArray(frameSize)
        var count = 0
        while (count < frameSize) {
            val r = read(frameData, count, frameSize - count)
            if (r < 0) break
            count += r
        }
        if (count != frameSize) return null

        var idx = 1 // skip text encoding byte

        // 读取 MIME 类型字符串
        val mimeStart = idx
        while (idx < frameData.size && frameData[idx] != 0.toByte()) idx++
        val mimeType = String(frameData, mimeStart, idx - mimeStart)
        idx++ // skip null byte

        // 跳过图片类型（1 byte）
        idx++

        // 跳过描述字符串
        while (idx < frameData.size && frameData[idx] != 0.toByte()) idx++
        idx++ // skip null byte

        if (idx >= frameData.size) return null

        val imageData = frameData.copyOfRange(idx, frameData.size)
        return saveAlbum(imageData, mimeType) to mimeType
    }
    return null
}

private fun syncSafeInt(bytes: ByteArray): Int =
    bytes[0].toInt() and 0x7F shl 21 or
        (bytes[1].toInt() and 0x7F shl 14) or
        (bytes[2].toInt() and 0x7F shl 7) or
        (bytes[3].toInt() and 0x7F)
