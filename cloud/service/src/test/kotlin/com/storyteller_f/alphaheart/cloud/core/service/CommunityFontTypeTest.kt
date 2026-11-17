/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import org.apache.tika.Tika
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CommunityFontTypeTest {
    @Test
    fun tikaDetectedOpenTypeAndTrueTypeAreAcceptedByFontSettings() {
        val tika = Tika()
        val openType = tika.detect("OTTO\u0000".encodeToByteArray())
        val trueType = tika.detect(byteArrayOf(0, 1, 0, 0))
        assertEquals("application/x-font-otf", openType)
        assertEquals("application/x-font-ttf", trueType)
        assertTrue(isCommunityFontType(openType))
        assertTrue(isCommunityFontType(trueType))
    }

    @Test
    fun registeredFontTypesAndOpenTypeAliasRemainAccepted() {
        for (type in listOf("font/otf", "font/ttf", "font/woff2", "application/font-woff")) {
            assertTrue(isCommunityFontType(type))
        }
        assertTrue(isCommunityFontType("application/vnd.ms-opentype"))
    }

    @Test
    fun nonFontContentAndUnrelatedLegacyFormatsAreRejected() {
        for (type in listOf("image/png", "application/pdf", "application/octet-stream", "application/x-font-bdf")) {
            assertFalse(isCommunityFontType(type))
        }
    }
}
