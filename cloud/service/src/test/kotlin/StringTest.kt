/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StringTest {
    @Test
    fun testVisibleUnicodeString() {
        assertTrue(isAllVisibleChar("a"))
        assertFalse(isAllVisibleChar("\uFEFF"))
        assertFalse(isAllVisibleChar("\u0301"))
        assertFalse(isAllVisibleChar("\u00A0"))
        assertFalse(isAllVisibleChar("\u202F"))
        assertFalse(isAllVisibleChar("\u3000"))
        assertFalse(isAllVisibleChar("\u200B"))
        assertFalse(isAllVisibleChar("\u202A"))
        // U+1AFF is a non-spacing mark
        assertFalse(isAllVisibleChar("\u1AFF"))
        assertFalse(isAllVisibleChar("\u206F"))
        assertTrue(isAllVisibleChar(" "))
    }
}
