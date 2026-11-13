/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import java.io.RandomAccessFile

/** Read only the original SFNT tables, never properties overridden by a platform Font builder. */
internal fun readAndroidFontMetadata(face: CommunityFontFace, validation: CommunityFontValidation) {
    RandomAccessFile(face.name, "r").use { file ->
        check(file.length() >= 12) { "Truncated font header" }
        val signature = file.readInt()
        val offset =
            if (signature == 0x74746366) {
                file.readInt() // TTC version
                // Each download entry currently represents one face at index zero.
                check(file.readInt() > 0) { "Empty font collection" }
                file.readInt().toLong() and 0xffffffffL
            } else {
                0L
            }
        file.seek(offset)
        check(file.readInt() in listOf(0x00010000, 0x4f54544f, 0x74727565)) { "Unsupported font container" }
        val count = file.readUnsignedShort()
        check(offset + 12L + count * 16L <= file.length()) { "Truncated font table directory" }
        val tables =
            (0 until count).associate { index ->
                file.seek(offset + 12L + index * 16L)
                val tag = file.readInt()
                file.readInt() // Table checksum
                tag to (file.readInt().toLong() and 0xffffffffL to (file.readInt().toLong() and 0xffffffffL))
            }
        validation.validate(
            face,
            file.readMetadataTable(tables[0x6e616d65]),
            file.readMetadataTable(tables[0x4f532f32]),
        )
    }
}

private fun RandomAccessFile.readMetadataTable(location: Pair<Long, Long>?): ByteArray {
    val (offset, length) = checkNotNull(location) { "Missing font metadata table" }
    check(length in 1..MAX_FONT_DECODED_BYTES && offset <= length() - length) { "Invalid font metadata table bounds" }
    seek(offset)
    return ByteArray(length.toInt()).also { readFully(it) }
}
