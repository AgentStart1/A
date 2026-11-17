/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared

import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.ContentValidationError
import com.storyteller_f.shared.utils.checkContent
import com.storyteller_f.shared.utils.safeFirstUnicode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentTest : PlatformHeadlessTest() {
    @Test
    fun `test check content`() {
        checkContent("a").getOrThrow()
        checkContent("👌").getOrThrow()
        checkContent("\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66").getOrThrow()
        checkContent("\n").getOrThrow()
        checkContent("\uf092").getOrThrow()
    }

    @Test
    fun `content accepts mixed CRLF and LF without normalizing input`() {
        val input =
            buildString {
                append("first")
                append('\r')
                append('\n')
                append("第二行")
                append('\n')
                append("third")
                append('\r')
                append('\n')
            }
        val original = input.toCharArray().concatToString()

        checkContent(input).getOrThrow()

        assertEquals(original, input)
    }

    @Test
    fun `content still rejects NUL DEL controls and standalone carriage return`() {
        listOf('\u0000', '\u0001', '\u007f', '\r').forEach { control ->
            val input =
                buildString {
                    append("before")
                    append(control)
                    append("after")
                }
            val result = checkContent(input)

            assertTrue(
                result.errorOrNull() is ContentValidationError.UnsupportedCharacters,
                "Expected unsupported-character rejection for U+${control.code.toString(16)}",
            )
        }
    }

    @Test
    fun `first unicode preserves complete code point`() {
        assertEquals("👌", safeFirstUnicode("👌abc"))
    }
}
