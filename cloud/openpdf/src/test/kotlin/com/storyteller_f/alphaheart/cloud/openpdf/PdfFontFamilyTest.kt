/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.getOrThrow
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class PdfFontFamilyTest {
    @Test
    fun `static files ignore an unusable variable path`() =
        withFontDirectory { directory ->
            val regular = fontBytes("mono-regular.ttf")
            val bold = fontBytes("mono-bold.ttf")
            directory.resolve("mono-regular.ttf").writeBytes(regular)
            directory.resolve("mono-bold.ttf").writeBytes(bold)
            directory.resolve("mono-variable.ttf").mkdir()

            val family = PdfFontFamily.load("mono", directory)
            val repository = FontInstanceRepository()

            assertContentEquals(regular, family.getInstance(repository, 400).getOrThrow().copyBytes())
            assertContentEquals(bold, family.getInstance(repository, 700).getOrThrow().copyBytes())
        }

    @Test
    fun `variable IO failure only affects missing weight`() =
        withFontDirectory { directory ->
            val regular = fontBytes("mono-regular.ttf")
            directory.resolve("mono-regular.ttf").writeBytes(regular)
            directory.resolve("mono-variable.ttf").mkdir()

            val family = PdfFontFamily.load("mono", directory)
            val repository = FontInstanceRepository()

            assertContentEquals(regular, family.getInstance(repository, 400).getOrThrow().copyBytes())
            val failure = assertIs<FontError.InstancingFailed>(family.getInstance(repository, 700).errorOrNull())
            assertIs<IOException>(failure.cause)
        }

    @Test
    fun `missing weight reads variable file on demand`() =
        withFontDirectory { directory ->
            val regular = fontBytes("mono-regular.ttf")
            directory.resolve("mono-regular.ttf").writeBytes(regular)
            val family = PdfFontFamily.load("mono", directory)
            directory.resolve("mono-variable.ttf").writeBytes(fontBytes("variable.ttf"))

            val instance = family.getInstance(FontInstanceRepository(), 700).getOrThrow().copyBytes()

            assertFalse(SfntFont(instance, 0).hasTable("fvar"))
        }

    private fun withFontDirectory(block: (File) -> Unit) {
        val root = Files.createDirectories(Path.of("build/tmp/font-family-tests")).toRealPath()
        val directory = Files.createTempDirectory(root, "family-").toFile()
        try {
            block(directory)
        } finally {
            check(directory.toPath().toRealPath().startsWith(root)) { "Font fixture escaped its test directory" }
            directory.deleteRecursively()
        }
    }
}
