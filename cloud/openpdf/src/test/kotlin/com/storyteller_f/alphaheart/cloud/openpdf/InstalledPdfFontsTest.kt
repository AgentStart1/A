/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.getOrThrow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InstalledPdfFontsTest {
    @Test
    fun `metadata discovers weights and variable status`() =
        withDirectory { directory ->
            val file = directory.resolve("font.ttf")
            file.writeBytes(metadataFont("Noto Serif SC", 700, variable = true, italic = true))
            val face = InstalledFontMetadata.read(file).single()
            assertEquals("Noto Serif SC", face.family)
            assertEquals(700, face.weight)
            assertTrue(face.variable)
            assertTrue(face.italic)
            assertEquals(0, face.source.faceIndex)
        }

    @Test
    fun `collection discovery preserves face indices`() =
        withDirectory { directory ->
            val file = directory.resolve("font.ttc")
            val faces = listOf(metadataFont("Consolas", 400), metadataFont("Consolas", 700))
            val output = ByteBuffer.allocate(20 + faces.sumOf { it.size }).order(ByteOrder.BIG_ENDIAN)
            output.putInt(encodeTag("ttcf")).putInt(0x10000).putInt(2)
                .putInt(20).putInt(20 + faces[0].size)
            faces.forEach { bytes ->
                val offset = output.position()
                val face = ByteBuffer.wrap(bytes.copyOf()).order(ByteOrder.BIG_ENDIAN)
                repeat(face.getShort(4).toInt()) { index ->
                    val position = 12 + index * 16 + 8
                    face.putInt(position, face.getInt(position) + offset)
                }
                output.put(face.array())
            }
            file.writeBytes(output.array())
            val discovered = InstalledPdfFonts.scan(listOf(directory))
            assertEquals(listOf(0, 1), discovered.map { it.source.faceIndex })
            assertEquals(listOf(400, 700), discovered.map { it.weight })
        }

    @Test
    fun `installed static faces never read unused VF`() =
        withDirectory { directory ->
            val regular = directory.resolve("regular.ttf").apply { writeBytes(fontBytes("mono-regular.ttf")) }
            val bold = directory.resolve("bold.ttf").apply { writeBytes(fontBytes("mono-bold.ttf")) }
            val variable =
                directory.resolve("variable.ttf").apply {
                    writeBytes(metadataFont("Noto Sans Mono", 400, true))
                }
            val family = InstalledPdfFonts.select("mono", InstalledPdfFonts.scan(listOf(directory)))
            assertTrue(variable.delete())
            assertTrue(variable.mkdir())
            val repository = FontInstanceRepository()
            assertContentEquals(regular.readBytes(), family.getInstance(repository, 400).getOrThrow().copyBytes())
            assertContentEquals(bold.readBytes(), family.getInstance(repository, 700).getOrThrow().copyBytes())
        }

    @Test
    fun `installed missing weight loads VF on demand`() =
        withDirectory { directory ->
            val regular = directory.resolve("regular.ttf").apply { writeBytes(fontBytes("mono-regular.ttf")) }
            val variable =
                directory.resolve("variable.ttf").apply {
                    writeBytes(metadataFont("Noto Sans Mono", 400, true))
                }
            val family = InstalledPdfFonts.select("mono", InstalledPdfFonts.scan(listOf(directory)))
            variable.writeBytes(fontBytes("variable.ttf"))
            val repository = FontInstanceRepository()
            assertContentEquals(regular.readBytes(), family.getInstance(repository, 400).getOrThrow().copyBytes())
            val bold = family.getInstance(repository, 700).getOrThrow().copyBytes()
            assertTrue(SfntFont(bold, 0).axes.isEmpty())
        }

    @Test
    fun `discovery ignores unrelated malformed fonts`() =
        withDirectory { directory ->
            directory.resolve("bad.ttf").writeBytes(byteArrayOf(1))
            directory.resolve("valid.ttf").writeBytes(metadataFont("Consolas", 400))
            val faces = InstalledPdfFonts.scan(listOf(directory))
            assertEquals(listOf("Consolas"), faces.map { it.family })
            assertFailsWith<IllegalArgumentException> { InstalledPdfFonts.select("mono", faces) }
        }

    private fun metadataFont(
        family: String,
        weight: Int,
        variable: Boolean = false,
        italic: Boolean = false,
    ): ByteArray {
        val text = family.toByteArray(Charsets.UTF_16BE)
        val names = ByteBuffer.allocate(18 + text.size).order(ByteOrder.BIG_ENDIAN)
        names.putShort(0).putShort(1).putShort(18)
        names.putShort(3).putShort(1).putShort(0x409)
            .putShort(16).putShort(text.size.toShort()).putShort(0)
            .put(text)
        val metrics = ByteBuffer.allocate(64).order(ByteOrder.BIG_ENDIAN)
        metrics.putShort(4, weight.toShort()).putShort(62, if (italic) 1 else 0)
        val tables =
            listOf("name" to names.array(), "OS/2" to metrics.array()) +
                if (variable) listOf("fvar" to ByteArray(16)) else emptyList()
        val headerSize = 12 + tables.size * 16
        val output = ByteBuffer.allocate(headerSize + tables.sumOf { it.second.size }).order(ByteOrder.BIG_ENDIAN)
        output.putInt(0x10000).putShort(tables.size.toShort())
            .putShort(0).putShort(0).putShort(0)
        tables.fold(headerSize) { offset, (tag, bytes) ->
            output.putInt(encodeTag(tag)).putInt(0).putInt(offset).putInt(bytes.size)
            offset + bytes.size
        }
        tables.forEach { output.put(it.second) }
        return output.array()
    }

    private fun withDirectory(block: (File) -> Unit) {
        val root = Files.createDirectories(Path.of("build/tmp/installed-font-tests")).toRealPath()
        val directory = Files.createTempDirectory(root, "fonts-").toFile()
        try {
            block(directory)
        } finally {
            check(directory.toPath().toRealPath().startsWith(root))
            directory.deleteRecursively()
        }
    }
}
