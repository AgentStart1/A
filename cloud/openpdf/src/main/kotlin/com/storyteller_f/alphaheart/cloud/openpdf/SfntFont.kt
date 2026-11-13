/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Bounds-checked SFNT/TTC metadata reader. Native code remains responsible for outline instancing. */
internal class SfntFont(bytes: ByteArray, faceIndex: Int) {
    private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    private val faceOffset = findFace(faceIndex)
    private val tables = readTables()
    val axes: Map<String, FontAxis> = readAxes()

    fun hasTable(tag: String): Boolean = tag in tables

    private fun findFace(faceIndex: Int): Int {
        require(faceIndex >= 0) { "Negative font face index" }
        val signature = readInt(0)
        return if (signature == encodeTag("ttcf")) {
            require(faceIndex < readInt(8)) { "Font face index is outside the collection" }
            readInt(12 + Math.multiplyExact(faceIndex, 4))
        } else {
            require(faceIndex == 0) { "Standalone font only has face zero" }
            0
        }
    }

    private fun readTables(): Map<String, IntRange> {
        val signature = readInt(faceOffset)
        require(signature == 0x00010000 || signature == encodeTag("OTTO")) { "Unsupported SFNT signature" }
        val count = readUShort(faceOffset + 4)
        return (0 until count).associate { index ->
            val record = faceOffset + 12 + index * 16
            val name = ByteArray(4) { byteIndex -> readByte(record + byteIndex) }.toString(Charsets.US_ASCII)
            val offset = readInt(record + 8)
            val length = readInt(record + 12)
            require(offset >= 0 && length >= 0 && offset.toLong() + length <= buffer.limit()) {
                "Invalid font table bounds"
            }
            name to (offset until offset + length)
        }.also { require(it.size == count) { "Duplicate font tables" } }
    }

    private fun readAxes(): Map<String, FontAxis> {
        val table = tables["fvar"] ?: return emptyMap()
        require(table.count() >= 16 && readUShort(table.first) == 1) { "Invalid fvar header" }
        val start = readUShort(table.first + 4)
        val count = readUShort(table.first + 8)
        val size = readUShort(table.first + 10)
        require(start >= 16 && size >= 20 && start.toLong() + count.toLong() * size <= table.count()) {
            "Invalid variation axis bounds"
        }
        return (0 until count).associate { index ->
            val offset = table.first + start + index * size
            val name = ByteArray(4) { byteIndex -> readByte(offset + byteIndex) }.toString(Charsets.US_ASCII)
            val axis = FontAxis(readFixed(offset + 4), readFixed(offset + 8), readFixed(offset + 12))
            require(axis.minimum <= axis.default && axis.default <= axis.maximum) { "Invalid axis range" }
            name to axis
        }.also { require(it.size == count) { "Duplicate variation axes" } }
    }

    private fun readFixed(offset: Int): Float = readInt(offset) / 65_536f
    private fun readUShort(offset: Int): Int {
        require(offset >= 0 && offset.toLong() + 2 <= buffer.limit()) { "Truncated font" }
        return buffer.getShort(offset).toInt() and 0xffff
    }
    private fun readInt(offset: Int): Int {
        require(offset >= 0 && offset.toLong() + 4 <= buffer.limit()) { "Truncated font" }
        return buffer.getInt(offset)
    }
    private fun readByte(offset: Int): Byte {
        require(offset in 0 until buffer.limit()) { "Truncated font" }
        return buffer.get(offset)
    }
}

internal data class FontAxis(val minimum: Float, val default: Float, val maximum: Float)

internal fun encodeTag(tag: String): Int {
    require(tag.length == 4 && tag.all { it.code in 0x20..0x7e }) { "Font axis tags require four ASCII characters" }
    return tag.fold(0) { value, char -> value shl 8 or char.code }
}
