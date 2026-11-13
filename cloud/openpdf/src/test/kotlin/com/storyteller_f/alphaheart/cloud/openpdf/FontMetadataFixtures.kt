/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal fun readFontWeight(bytes: ByteArray): Int {
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    val count = buffer.getShort(4).toInt() and 0xffff
    val record = (0 until count).map { 12 + it * 16 }.single { buffer.getInt(it) == encodeTag("OS/2") }
    return buffer.getShort(buffer.getInt(record + 8) + 4).toInt() and 0xffff
}
