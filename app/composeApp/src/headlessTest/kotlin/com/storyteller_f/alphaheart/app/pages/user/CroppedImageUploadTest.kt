/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import com.storyteller_f.alphaheart.app.media.croppedImageUploadName
import com.storyteller_f.alphaheart.app.media.prepareCroppedImageUpload
import com.storyteller_f.shared.utils.sha256
import io.ktor.http.ContentType
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class CroppedImageUploadTest {
    @Test
    fun repeatedConfirmedExportsKeepTheSourceStemButNeverReuseTheOldFileName() =
        runTest {
            val bytes =
                Base64.decode(
                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aDcQAAAAASUVORK5CYII=",
                )
            val first = prepareCroppedImageUpload("avatar.svg", bytes)
            val second = prepareCroppedImageUpload("avatar.svg", bytes)
            val hash = Buffer().also { it.write(bytes) }.use { sha256(it) }
            assertNotEquals("avatar-cropped.png", first.name)
            assertNotEquals(first.name, second.name)
            for (data in listOf(first, second)) {
                assertTrue(data.name.startsWith("avatar-cropped-"))
                assertTrue(data.name.endsWith(".png"))
                Uuid.parse(data.name.removePrefix("avatar-cropped-").removeSuffix(".png"))
                assertEquals(ContentType.Image.PNG, data.contentType)
                assertEquals(hash, data.sha256)
                assertEquals(bytes.size.toLong(), data.size)
                assertContentEquals(bytes, data.block().use { it.readByteArray() })
                // Opening the same request body again retains its export name and bytes; it never renames to retry.
                assertContentEquals(bytes, data.block().use { it.readByteArray() })
            }
        }

    @Test
    fun exportedNamesRespectTheExistingNameColumnLimitWithoutTruncatingIdentity() {
        val firstId = Uuid.parse("00000000-0000-4000-8000-000000000001")
        val secondId = Uuid.parse("00000000-0000-4000-8000-000000000002")
        val source = "a".repeat(200) + ".svg"
        val first = croppedImageUploadName(source, firstId)
        val second = croppedImageUploadName(source, secondId)
        assertEquals(200, first.length)
        assertTrue(first.endsWith("-cropped-$firstId.png"))
        assertNotEquals(first, second)
        assertEquals("image-cropped-$firstId.png", croppedImageUploadName(".svg", firstId))
    }
}
