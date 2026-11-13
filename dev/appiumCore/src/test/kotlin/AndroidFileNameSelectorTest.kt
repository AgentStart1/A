/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidFileNameSelectorTest {
    @Test
    fun matchesWholeFilenameThroughUiAutomator() {
        for (name in listOf("sample.pdf", "it's, \"report\".pdf", "file\nname.pdf")) {
            // Match UiExpressionParser.coerceArgToType: strip quotes and unescape quotes only.
            val argument = androidFileNameSelector(name).substringAfter("descriptionMatches(\"").removeSuffix("\")")
            val pattern = Regex(argument.replace("\\\"", "\""))
            assertTrue(pattern.matches(name))
            assertTrue(pattern.matches("$name, 2026/10/30 10:00, 590 B"))
            assertFalse(pattern.matches("$name,backup"))
            assertTrue(pattern.matches("$name\n2026/10/30 10:00\n590 B"))
            assertFalse(pattern.matches("prefix $name"))
            assertFalse(pattern.matches("$name.bak"))
        }
    }
}
