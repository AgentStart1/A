/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.backend.core.service.ProcessedUploadPack
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class FileServiceTest {
    @Test
    fun `cleanup updates upload size`() =
        runBlocking {
            val source = Files.createTempFile("upload-source-", ".image").toFile()
            val cleanedFiles = mutableListOf<File>()
            try {
                source.writeText("cleaned image content")
                val originalSize = source.length() + 100
                val processed =
                    ProcessedUploadPack(
                        UploadPack(source, "image.test", originalSize, "owner/image.test", "old-sha"),
                        "image/x-test",
                    )

                val cleaned = removeExifIfImage(listOf(processed), cleanedFiles).single().pack

                assertEquals(cleaned.file.length(), cleaned.size)
                assertNotEquals(originalSize, cleaned.size)
            } finally {
                source.delete()
                cleanedFiles.forEach(File::delete)
            }
        }
}
