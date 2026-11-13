/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Build a collection in memory from external inputs, aligning faces and rebasing every table offset. */
internal fun createFontCollection(faces: List<ByteArray>): ByteArray {
    val headerSize = 12 + faces.size * 4
    val sizes = faces.map { it.size + 3 and -4 }
    val offsets = sizes.runningFold(headerSize, Int::plus).dropLast(1)
    val result = ByteBuffer.allocate(headerSize + sizes.sum()).order(ByteOrder.BIG_ENDIAN)
    result.putInt(encodeTag("ttcf")).putInt(0x10000).putInt(faces.size)
    offsets.forEach { result.putInt(it) }
    faces.forEachIndexed { index, bytes ->
        val face = ByteBuffer.wrap(bytes.copyOf()).order(ByteOrder.BIG_ENDIAN)
        repeat(face.getShort(4).toInt() and 0xffff) { table ->
            val record = 12 + table * 16
            val start = face.getInt(record + 8)
            // A TTC does not use head.checkSumAdjustment.
            if (face.getInt(record) == encodeTag("head")) face.putInt(start + 8, 0)
            face.putInt(record + 8, start + offsets[index])
        }
        result.position(offsets[index])
        result.put(face.array())
    }
    return result.array()
}
