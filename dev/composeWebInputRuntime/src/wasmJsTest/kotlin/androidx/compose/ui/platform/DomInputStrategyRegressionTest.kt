/*
 * This is a private project. All rights reserved.
 */
package androidx.compose.ui.platform

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.browser.document
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DomInputStrategyRegressionTest {
    @Test
    fun selectedForwardDeleteRestoresModelBeforeSelectionThenCommitsEmptyText() = withStrategy { input ->
        val text = "ReviewDraft"
        input.updateState(TextFieldValue(text, TextRange(text.length)))
        // This is the browser's real empty-contenteditable representation after native Delete.
        input.htmlInput.innerHTML = "<br>"
        // The deferred Compose key handler may first emit selection with unchanged model text.
        input.updateState(TextFieldValue(text, TextRange(0, text.length)))
        assertEquals(text, input.htmlInput.textContent)
        assertEquals(text.length, selectionFocusOffset())
        input.updateState(TextFieldValue("", TextRange.Zero))
        assertEquals("", input.htmlInput.textContent)
        assertEquals(0, selectionFocusOffset())
    }

    @Test
    fun caretForwardDeleteDoesNotLeaveSelectionOutsideShortenedNativeNode() = withStrategy { input ->
        input.updateState(TextFieldValue("abcd", TextRange(4)))
        input.htmlInput.textContent = "abc"
        input.updateState(TextFieldValue("abcd", TextRange(3)))
        assertEquals("abcd", input.htmlInput.textContent)
        assertEquals(3, selectionFocusOffset())
        input.updateState(TextFieldValue("abc", TextRange(3)))
        assertEquals("abc", input.htmlInput.textContent)
    }

    @Test
    fun unicodeSelectionUsesUnchangedUtf16OffsetsWithoutDoubleDeletion() = withStrategy { input ->
        val original = "中🚀e\u0301文"
        val remaining = "中e\u0301文"
        input.updateState(TextFieldValue(original, TextRange(original.length)))
        input.htmlInput.textContent = remaining
        input.updateState(TextFieldValue(original, TextRange(1, 3)))
        assertEquals(original, input.htmlInput.textContent)
        assertEquals(1, selectionAnchorOffset())
        assertEquals(3, selectionFocusOffset())
        input.updateState(TextFieldValue(remaining, TextRange(1)))
        assertEquals(remaining, input.htmlInput.textContent)
        assertEquals(1, selectionFocusOffset())
    }

    @Test
    fun normalBackspaceModelCommitRemainsAuthoritative() = withStrategy { input ->
        input.updateState(TextFieldValue("ab", TextRange(2)))
        input.htmlInput.textContent = "a"
        input.updateState(TextFieldValue("a", TextRange(1)))
        assertEquals("a", input.htmlInput.textContent)
        assertEquals(1, selectionFocusOffset())
    }

    @Test
    fun activeCompositionIsNotOverwrittenByIntermediateModel() = runTest { withStrategy { input ->
        input.updateState(TextFieldValue("n", TextRange(1)))
        input.htmlInput.dispatchEvent(CompositionEvent("compositionstart", CompositionEventInit(data = "")))
        input.htmlInput.textContent = "你"
        input.updateState(TextFieldValue("ni", TextRange(2), TextRange(0, 2)))
        assertEquals("你", input.htmlInput.textContent)
        input.htmlInput.dispatchEvent(CompositionEvent("compositionend", CompositionEventInit(data = "你")))
        input.updateState(TextFieldValue("你", TextRange(1)))
        awaitNativeInputFrame()
        assertEquals("你", input.htmlInput.textContent)
        assertEquals(1, selectionFocusOffset())
    } }

    @Test
    fun fieldOwnerAndGeometryChangesPreserveEditableModel() {
        val firstContainer = document.createElement("div") as HTMLElement
        val secondContainer = document.createElement("div") as HTMLElement
        document.body!!.appendChild(firstContainer)
        document.body!!.appendChild(secondContainer)
        val first = BackingDomInput(firstContainer, ImeOptions(singleLine = true), Commands())
        val second = BackingDomInput(secondContainer, ImeOptions(singleLine = true), Commands())
        try {
            first.register()
            second.register()
            first.updateState(TextFieldValue("ReviewDraft", TextRange(11)))
            second.updateState(TextFieldValue("alias", TextRange(5)))
            first.updateHtmlInputBox(10f, 10f, 350f, 48f)
            first.updateHtmlInputBox(10f, 10f, 900f, 48f)
            first.backingElement.innerHTML = "<br>"
            first.updateState(TextFieldValue("ReviewDraft", TextRange(0, 11)))
            assertEquals("ReviewDraft", first.backingElement.textContent)
            assertEquals("alias", second.backingElement.textContent)
            assertTrue(first.backingElement.isConnected)
            first.updateState(TextFieldValue("", TextRange.Zero))
            assertEquals("", first.backingElement.textContent)
        } finally {
            first.dispose()
            second.dispose()
            firstContainer.remove()
            secondContainer.remove()
        }
    }

    private inline fun withStrategy(block: (DomInputStrategy) -> Unit) {
        val input = DomInputStrategy(ImeOptions(singleLine = true), Commands())
        document.body!!.appendChild(input.htmlInput)
        try {
            block(input)
        } finally {
            input.dispose()
            input.htmlInput.remove()
        }
    }

    private class Commands : ComposeCommandCommunicator {
        override fun sendEditCommand(commands: List<EditCommand>) = Unit
        override fun sendKeyboardEvent(keyboardEvent: KeyEvent): Boolean = false
    }
}

private fun selectionAnchorOffset(): Int = js("window.getSelection().anchorOffset")
private fun selectionFocusOffset(): Int = js("window.getSelection().focusOffset")
