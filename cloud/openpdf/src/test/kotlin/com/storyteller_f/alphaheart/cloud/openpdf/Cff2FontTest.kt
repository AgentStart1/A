/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.getOrThrow
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Cff2FontTest {
    @Test
    fun `CFF2 instances contain only static CFF outlines`() {
        val source = fontBytes("cff2-variable.otf")
        val repository = FontInstanceRepository()
        for (weight in listOf(400f, 700f)) {
            val result = repository.getInstance(source, axes = mapOf("wght" to weight)).getOrThrow()
            val metadata = SfntFont(result.copyBytes(), 0)
            assertTrue(metadata.hasTable("CFF "))
            listOf("CFF2", "fvar", "gvar").forEach { assertFalse(metadata.hasTable(it), it) }
        }
    }

    @Test
    fun `discovery selects installed CFF2 OTF sources`() =
        withDirectory { directory ->
            directory.resolve("source.otf").writeBytes(fontBytes("cff2-variable.otf"))
            val family = InstalledPdfFonts.select("serif", InstalledPdfFonts.scan(listOf(directory)))
            val result = family.getInstance(FontInstanceRepository(), 700).getOrThrow()
            assertTrue(SfntFont(result.copyBytes(), 0).hasTable("CFF "))
        }

    @Test
    fun `OTF fallback is read only for missing weight`() =
        withDirectory { directory ->
            val regular = fontBytes("serif-regular.ttf")
            directory.resolve("serif-regular.ttf").writeBytes(regular)
            val family = PdfFontFamily.load("serif", directory)
            directory.resolve("serif-variable.otf").writeBytes(fontBytes("cff2-variable.otf"))
            val repository = FontInstanceRepository()
            assertContentEquals(regular, family.getInstance(repository, 400).getOrThrow().copyBytes())
            val result = family.getInstance(repository, 700).getOrThrow()
            assertTrue(SfntFont(result.copyBytes(), 0).hasTable("CFF "))
        }

    private fun withDirectory(block: (File) -> Unit) {
        val root = Files.createDirectories(Path.of("build/tmp/cff2-discovery")).toRealPath()
        val directory = Files.createTempDirectory(root, "fonts-").toFile()
        try {
            block(directory)
        } finally {
            check(directory.toPath().toRealPath().startsWith(root))
            directory.deleteRecursively()
        }
    }
}
