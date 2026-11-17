/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.shared.utils

import com.ibm.icu.text.UnicodeSet
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.ContentValidationError
import java.text.BreakIterator

actual fun checkContent(text: String): CustomResult<Unit, ContentValidationError> {
    val regexp = """^[\p{L}\p{N}\p{P}\p{Z}\p{S}\n]+$"""
    val emojiSet = UnicodeSet("[[:RGI_Emoji:]]").freeze()
    val nerdFontSet =
        UnicodeSet(
            """[\uE000-\uE0FF \uE5FA-\uE6B7 \uE700-\uE8EF """ +
                """\uEA60-\uEC1E \uED00-\uEFCE \uF000-\uF2FF """ +
                """\uF300-\uF381 \uF400-\uFD46 """ +
                """\U000F0001-\U000F1AF0]""",
        ).freeze()
    val breakIterator =
        BreakIterator.getCharacterInstance().apply {
            setText(text)
        }
    var start = breakIterator.first()
    var end = breakIterator.next()
    while (end != BreakIterator.DONE) {
        val sub = text.substring(start, end)
        val isSupported =
            sub == "\r\n" || regexp.toRegex().matches(sub) || emojiSet.contains(sub) || nerdFontSet.contains(sub)
        if (!isSupported) {
            return CustomResult.Failure(ContentValidationError.UnsupportedCharacters("unsupported $sub"))
        }
        start = end
        end = breakIterator.next()
    }
    return CustomResult.Success(Unit)
}

actual fun safeFirstUnicode(text: String): String? {
    val breakIterator =
        BreakIterator.getCharacterInstance().apply {
            setText(text)
        }
    val first = breakIterator.first()
    val next = breakIterator.next()
    val sub = text.substring(first, next)
    return sub
}

actual fun safeFirstEmoji(text: String): String? {
    val breakIterator =
        BreakIterator.getCharacterInstance().apply {
            setText(text)
        }
    val emojiSet = UnicodeSet("[[:RGI_Emoji:]]").freeze()
    val first = breakIterator.first()
    val next = breakIterator.next()
    val sub = text.substring(first, next)
    if (emojiSet.contains(sub)) {
        return sub
    }
    return null
}
