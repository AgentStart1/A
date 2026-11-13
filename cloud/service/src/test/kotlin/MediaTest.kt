/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.backend.core.getSvgDimension
import org.apache.tika.mime.MimeTypes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MediaTest {
    @Test
    fun `test svg dimension`() {
        val dimension = getSvgDimension("", "64px" to "64px")
        assertNotNull(dimension)
        assertEquals(64, dimension.width)
        assertEquals(64, dimension.height)
    }

    @Test
    fun `extension from mime type`() {
        listOf(
            "image/png" to ".png",
            "image/jpeg" to ".jpg",
            "image/gif" to ".gif",
            "image/bmp" to ".bmp",
            "image/webp" to ".webp",
            "image/avif" to ".avif",
        ).forEach { (mimeType, extension) ->
            assertEquals(extension, getCoverExtensionFromMimeType(mimeType))
            assertEquals(extension, MimeTypes.getDefaultMimeTypes().forName(mimeType)?.extension)
        }
    }
}
