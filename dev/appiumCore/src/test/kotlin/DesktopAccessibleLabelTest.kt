/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopAccessibleLabelTest {
    @Test
    fun matchCompleteLabelWithBadgeSuffix() {
        assertTrue(matchesDesktopLabel("Switch Account, ", "Switch Account"))
        assertTrue(matchesDesktopLabel("Switch Account, 1", "Switch Account"))
        assertTrue(matchesDesktopLabel("Switch Account", "Switch Account"))
    }

    @Test
    fun rejectPartialLabelMatch() {
        assertFalse(matchesDesktopLabel("Switch Account settings, 1", "Switch Account"))
        assertFalse(matchesDesktopLabel("Switch Account, 1", "Account"))
    }
}
