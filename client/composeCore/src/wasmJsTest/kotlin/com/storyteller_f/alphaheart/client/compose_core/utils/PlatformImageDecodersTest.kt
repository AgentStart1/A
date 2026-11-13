/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class PlatformImageDecodersTest {
    @Test
    internal fun recognizesAvifMajorBrand() {
        val source = Buffer().write(byteArrayOf(0, 0, 0, 24)).writeUtf8("ftypavifmif1avif")
        assertTrue(source.hasAvifHeader())
    }

    @Test
    internal fun rejectsOtherIsoMediaBrands() {
        val source = Buffer().write(byteArrayOf(0, 0, 0, 24)).writeUtf8("ftypmp42mp42isom")
        assertFalse(source.hasAvifHeader())
    }
}
