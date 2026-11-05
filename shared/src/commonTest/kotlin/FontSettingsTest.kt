/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared

import com.storyteller_f.shared.model.FontSettings
import com.storyteller_f.shared.model.FontSettingsWithInfo
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FontSettingsTest {
    @Test
    fun legacyFallbackFieldsAreIgnoredAndNotWritten() {
        val legacy =
            """{"settings":{"contentFontId":1,"codeFontId":2,"fallbackFontId":3},"fallbackFont":{"id":3}}"""
        val decoded = commonJson.decodeFromString<FontSettingsWithInfo>(legacy)
        assertEquals(FontSettings(contentFontId = 1, codeFontId = 2), decoded.settings)
        assertNull(decoded.contentFont)
        assertNull(decoded.codeFont)
        val encoded = commonJson.encodeToString(decoded)
        val fields = commonJson.parseToJsonElement(encoded).jsonObject
        assertEquals(setOf("settings"), fields.keys)
        assertEquals(setOf("contentFontId", "codeFontId"), fields.getValue("settings").jsonObject.keys)
        assertEquals(decoded, commonJson.decodeFromString<FontSettingsWithInfo>(encoded))
    }
}
