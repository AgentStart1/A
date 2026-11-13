/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads naming and OS/2 metadata only; never loads unselected outlines or variations. */
internal object InstalledFontMetadata {
    fun read(file: File): List<InstalledFontFace> =
        RandomAccessFile(file, "r").use { input ->
            val offsets =
                if (input.readInt() == encodeTag("ttcf")) {
                    input.seek(8)
                    val count = input.readInt()
                    require(count in 1..4096 && 12L + count * 4L <= input.length()) { "Invalid font collection" }
                    List(count) { input.readInt().toLong() }
                } else {
                    listOf(0L)
                }
            offsets.mapIndexed { index, offset -> readFace(input, FontFileSource(file, index), offset) }
        }

    private fun readFace(input: RandomAccessFile, source: FontFileSource, offset: Long): InstalledFontFace {
        input.seek(offset)
        require(input.readInt() in listOf(0x00010000, encodeTag("OTTO"))) { "Unsupported font signature" }
        val count = input.readUnsignedShort()
        input.skipBytes(6)
        val tables =
            (0 until count).associate {
                val tag = input.readInt()
                input.skipBytes(4)
                val start = input.readInt().toLong()
                val size = input.readInt()
                require(start >= 0 && size >= 0 && start + size <= input.length()) { "Invalid installed font table" }
                tag to (start to size)
            }
        val names = readTable(input, tables.getValue(encodeTag("name")))
        val metrics = readTable(input, tables.getValue(encodeTag("OS/2")))
        require(metrics.limit() >= 64) { "Truncated font metrics" }
        return InstalledFontFace(
            source,
            readFamily(names),
            metrics.getShort(4).toInt() and 0xffff,
            encodeTag("fvar") in tables,
            metrics.getShort(62).toInt() and 0x201 != 0,
        )
    }

    private fun readTable(input: RandomAccessFile, table: Pair<Long, Int>): ByteBuffer {
        require(table.second <= 1_048_576) { "Oversized font metadata" }
        input.seek(table.first)
        val bytes = ByteArray(table.second)
        input.readFully(bytes)
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
    }

    private fun readFamily(names: ByteBuffer): String {
        val count = names.getShort(2).toInt() and 0xffff
        val storage = names.getShort(4).toInt() and 0xffff
        require(6L + count * 12L <= names.limit()) { "Invalid font name records" }
        val records =
            (0 until count).mapNotNull { index ->
                val record = 6 + index * 12
                val platform = names.getShort(record).toInt() and 0xffff
                val language = names.getShort(record + 4).toInt() and 0xffff
                val id = names.getShort(record + 6).toInt() and 0xffff
                if (platform !in listOf(0, 3) || id !in listOf(1, 16)) return@mapNotNull null
                val size = names.getShort(record + 8).toInt() and 0xffff
                val start = storage + (names.getShort(record + 10).toInt() and 0xffff)
                require(start + size <= names.limit()) { "Invalid font name string" }
                val value = String(names.array(), start, size, Charsets.UTF_16BE)
                (if (id == 16) 2 else 0) + (if (language == 0x409 || platform == 0) 1 else 0) to value
            }
        return requireNotNull(records.maxByOrNull { it.first }?.second) { "Missing Unicode font family name" }
    }
}
