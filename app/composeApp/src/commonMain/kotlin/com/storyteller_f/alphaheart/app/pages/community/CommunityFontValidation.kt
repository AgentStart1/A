/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.community

import androidx.compose.ui.text.font.FontStyle

/** Validates original OpenType tables before any family can be published. Never repairs metadata. */
internal class CommunityFontValidation(faces: List<CommunityFontFace>) {
    private var family: String? = null

    init {
        check(faces.isNotEmpty()) { "Font family is empty" }
        val pairs = mutableSetOf<Pair<Int, FontStyle>>()
        faces.forEach { face ->
            check(pairs.add(face.weight.weight to face.style)) { "Duplicate font weight and style" }
        }
    }

    fun validate(face: CommunityFontFace, names: ByteArray, metrics: ByteArray) {
        val os2 = FontMetadataTable(metrics)
        val weight = os2.readUnsignedShort(4)
        check(weight in 1..1000) { "Invalid internal font weight" }
        val selection = os2.readUnsignedShort(62)
        val style = if (selection and 0x201 != 0) FontStyle.Italic else FontStyle.Normal
        check(face.weight.weight == weight && face.style == style) {
            "Internal font weight or style does not match the filename"
        }
        val actualFamily = readFontFamily(names)
        check(family == null || family == actualFamily) { "Font faces have different internal families" }
        family = actualFamily
    }
}

// OpenType name IDs 16/1 identify families; IDs 17/2 identify styles and must not be compared.
// https://learn.microsoft.com/en-us/typography/opentype/spec/name
private fun readFontFamily(bytes: ByteArray): String {
    val table = FontMetadataTable(bytes)
    check(table.readUnsignedShort(0) in 0..1) { "Unsupported font naming table" }
    val count = table.readUnsignedShort(2)
    val storage = table.readUnsignedShort(4)
    check(storage >= 6 + count * 12) { "Invalid font name storage" }
    val records = (0 until count).map { FontNameRecord(table, 6 + it * 12) }
    val nameId = if (records.any { it.nameId == 16 }) 16 else 1
    val candidates = records.filter { it.nameId == nameId && it.isUnicode }
    val record = checkNotNull(candidates.minByOrNull { it.priority }) { "Missing Unicode font family name" }
    val names = candidates.filter { it.priority == record.priority }.map { it.readName(table, storage) }
    check(names.distinct().size == 1) { "Ambiguous internal font family name" }
    return names.first()
}

private class FontNameRecord(table: FontMetadataTable, offset: Int) {
    private val platform = table.readUnsignedShort(offset)
    private val encoding = table.readUnsignedShort(offset + 2)
    private val language = table.readUnsignedShort(offset + 4)
    val nameId = table.readUnsignedShort(offset + 6)
    private val length = table.readUnsignedShort(offset + 8)
    private val stringOffset = table.readUnsignedShort(offset + 10)
    val isUnicode =
        when (platform) {
            0 -> true
            3 -> encoding in listOf(0, 1, 10)
            else -> false
        }

    // Identical on every platform: prefer Windows English, then language-neutral Unicode,
    // then a deterministic locale/platform. Localized family and subfamily strings may differ.
    val priority: Long =
        when {
            platform == 3 && language == 0x409 -> 0L
            platform == 0 && language == 0 -> 1L
            else -> 2L + language.toLong() * 65_536 + platform
        }

    fun readName(table: FontMetadataTable, storage: Int): String {
        check(length > 0 && length % 2 == 0) { "Invalid Unicode font family name" }
        val name =
            buildString {
                for (index in 0 until this@FontNameRecord.length step 2) {
                    append(table.readUnsignedShort(storage + stringOffset + index).toChar())
                }
            }
        check(name.isNotBlank() && '\u0000' !in name) { "Empty or invalid font family name" }
        return name
    }
}

private class FontMetadataTable(private val bytes: ByteArray) {
    fun readUnsignedShort(offset: Int): Int {
        check(offset >= 0 && offset <= bytes.size - 2) { "Truncated font metadata" }
        return bytes[offset].toInt().and(255).shl(8).or(bytes[offset + 1].toInt().and(255))
    }
}
