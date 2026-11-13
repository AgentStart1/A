/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import com.storyteller_f.alphaheart.app.pages.adaptiveColumnCount
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveHomeLayoutTest {
    @Test
    fun columnsRespectContentWidth() {
        assertEquals(1, adaptiveColumnCount(360f))
        assertEquals(1, adaptiveColumnCount(639f))
        assertEquals(2, adaptiveColumnCount(640f))
        assertEquals(3, adaptiveColumnCount(900f))
        assertEquals(4, adaptiveColumnCount(1280f))
    }
}
