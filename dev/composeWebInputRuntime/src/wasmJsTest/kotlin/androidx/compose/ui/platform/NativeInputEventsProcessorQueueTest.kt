/*
 * This is a private project. All rights reserved.
 */
package androidx.compose.ui.platform

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.CompositionEventInit
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.KeyboardEvent
import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeInputEventsProcessorQueueTest {
    @Test
    fun selectedDeleteCommitsEmptyModelAndDom() = runTest {
        withEditor(TextFieldValue("ReviewDraft", TextRange(0, 11))) { editor ->
            editor.key("Delete")
            editor.input("deleteContentForward", null, "ReviewDraft", 0, 11)
            editor.strategy.htmlInput.innerHTML = "<br>"
            // The queued Ctrl+A selection must not address this BR using offset 11.
            editor.strategy.updateState(editor.value)
            editor.strategy.restoreSelectionFromState()
            assertEquals("<br>", editor.strategy.htmlInput.innerHTML.lowercase())
            awaitNativeInputFrame()
            editor.assertValue("", 0)
            assertEquals(0, editor.keyboardFallbacks.size)
            assertEquals(1, editor.editBatches)
        }
    }

    @Test
    fun fastNativeTypingPreservesEntirePrefixAndFinalModel() = runTest {
        withEditor(TextFieldValue("")) { editor ->
            var native = ""
            for (letter in "ReviewDraft") {
                editor.key(letter.toString())
                editor.input("insertText", letter.toString(), native, native.length, native.length)
                native += letter
                editor.strategy.htmlInput.textContent = native
            }
            editor.strategy.updateState(TextFieldValue(""))
            assertEquals("ReviewDraft", editor.strategy.htmlInput.textContent)
            awaitNativeInputFrame()
            editor.assertValue("ReviewDraft", 11)
            assertEquals(0, editor.keyboardFallbacks.size)
        }
    }

    @Test
    fun caretDeleteUsesBrowserUnicodeRangeOnce() = runTest {
        withEditor(TextFieldValue("A🚀e\u0301B", TextRange(1))) { editor ->
            editor.key("Delete")
            editor.input("deleteContentForward", null, "A🚀e\u0301B", 1, 3)
            editor.strategy.htmlInput.textContent = "Ae\u0301B"
            awaitNativeInputFrame()
            editor.assertValue("Ae\u0301B", 1)
            assertEquals(1, editor.editBatches)
            assertEquals(0, editor.keyboardFallbacks.size)
        }
    }

    @Test
    fun collapsedForwardDeleteRemovesOneCodePointRatherThanOneUtf16Unit() = runTest {
        withEditor(TextFieldValue("A🚀B", TextRange(1))) { editor ->
            editor.key("Delete")
            editor.input("deleteContentForward", null, "A🚀B", 1, 1)
            editor.strategy.htmlInput.textContent = "AB"
            awaitNativeInputFrame()
            editor.assertValue("AB", 1)
            assertEquals(0, editor.keyboardFallbacks.size)
        }
    }

    @Test
    fun wordDeleteUsesWholeAuthoritativeRange() = runTest {
        withEditor(TextFieldValue("hello world", TextRange.Zero)) { editor ->
            editor.key("Delete", control = true)
            editor.input("deleteWordForward", null, "hello world", 0, 6)
            editor.strategy.htmlInput.textContent = "world"
            awaitNativeInputFrame()
            editor.assertValue("world", 0)
            assertEquals(0, editor.keyboardFallbacks.size)
        }
    }

    @Test
    fun missingWordRangeDelegatesModifiedKeyWithoutGuessingCodePoint() = runTest {
        withEditor(TextFieldValue("hello world", TextRange.Zero)) { editor ->
            editor.key("Delete", control = true)
            editor.input("deleteWordForward", null, "hello world", 0, 0, missingRange = true)
            awaitNativeInputFrame()
            assertEquals(1, editor.keyboardFallbacks.size)
            assertTrue(editor.keyboardFallbacks.single().isCtrlPressed)
            assertEquals(0, editor.editBatches)
            editor.assertValue("hello world", 0)
        }
    }

    @Test
    fun twoForwardDeletesInOneBatchDoNotShareKeyboardOwnership() = runTest {
        withEditor(TextFieldValue("abcd", TextRange(1))) { editor ->
            editor.key("Delete")
            editor.input("deleteContentForward", null, "abcd", 1, 2)
            editor.strategy.htmlInput.textContent = "acd"
            editor.key("Delete")
            editor.input("deleteContentForward", null, "acd", 1, 2)
            editor.strategy.htmlInput.textContent = "ad"
            awaitNativeInputFrame()
            editor.assertValue("ad", 1)
            assertEquals(2, editor.editBatches)
            assertEquals(0, editor.keyboardFallbacks.size)
        }
    }

    @Test
    fun backspaceKeepsExistingNativeRangeSemantics() = runTest {
        withEditor(TextFieldValue("A🚀B", TextRange(3))) { editor ->
            editor.key("Backspace")
            editor.input("deleteContentBackward", null, "A🚀B", 1, 3)
            editor.strategy.htmlInput.textContent = "AB"
            awaitNativeInputFrame()
            editor.assertValue("AB", 1)
            assertEquals(0, editor.keyboardFallbacks.size)
        }
    }

    @Test
    fun elementContainerRangeConvertsChildOffsetsToTextOffsets() = runTest {
        withEditor(TextFieldValue("ReviewDraft", TextRange(0, 11))) { editor ->
            editor.key("Delete")
            editor.input("deleteContentForward", null, "ReviewDraft", 0, 1, elementRange = true)
            editor.strategy.htmlInput.innerHTML = "<br>"
            awaitNativeInputFrame()
            editor.assertValue("", 0)
        }
    }

    @Test
    fun rejectedEditReconcilesFinalAuthoritativeModelAfterWholeBatch() = runTest {
        withEditor(TextFieldValue("A", TextRange(1)), rejectEdits = true) { editor ->
            editor.key("B")
            editor.input("insertText", "B", "A", 1, 1)
            editor.strategy.htmlInput.textContent = "AB"
            awaitNativeInputFrame()
            editor.assertValue("A", 1)
        }
    }

    @Test
    fun compositionBatchCommitsOnceWithoutIntermediateNativeOverwrite() = runTest {
        withEditor(TextFieldValue("")) { editor ->
            editor.strategy.htmlInput.dispatchEvent(CompositionEvent("compositionstart", CompositionEventInit(data = "")))
            editor.input("insertCompositionText", "你", "", 0, 0)
            editor.strategy.htmlInput.textContent = "你"
            editor.strategy.updateState(TextFieldValue(""))
            assertEquals("你", editor.strategy.htmlInput.textContent)
            editor.strategy.htmlInput.dispatchEvent(CompositionEvent("compositionend", CompositionEventInit(data = "你")))
            awaitNativeInputFrame()
            editor.assertValue("你", 1)
            assertEquals(null, editor.value.composition)
        }
    }

    private suspend fun withEditor(
        initial: TextFieldValue,
        rejectEdits: Boolean = false,
        block: suspend (Editor) -> Unit,
    ) {
        val editor = Editor(initial, rejectEdits)
        try {
            block(editor)
        } finally {
            editor.strategy.dispose()
            editor.strategy.htmlInput.remove()
        }
    }

    private class Editor(initial: TextFieldValue, val rejectEdits: Boolean) : ComposeCommandCommunicator {
        private val processor = EditProcessor().apply { reset(initial, null) }
        val strategy = DomInputStrategy(ImeOptions(singleLine = true), this)
        val keyboardFallbacks = mutableListOf<KeyEvent>()
        var editBatches = 0
        val value: TextFieldValue get() = processor.toTextFieldValue()

        init {
            document.body!!.appendChild(strategy.htmlInput)
            strategy.updateState(initial)
        }

        override fun currentTextFieldValue(): TextFieldValue = value
        override fun sendEditCommand(commands: List<EditCommand>) {
            editBatches++
            if (!rejectEdits) processor.apply(commands)
            strategy.updateState(value)
        }
        override fun sendKeyboardEvent(keyboardEvent: KeyEvent): Boolean {
            keyboardFallbacks += keyboardEvent
            return false
        }
        fun key(key: String, control: Boolean = false) {
            strategy.htmlInput.dispatchEvent(nativeKey(key, control))
        }
        fun input(
            type: String,
            data: String?,
            text: String,
            start: Int,
            end: Int,
            missingRange: Boolean = false,
            elementRange: Boolean = false,
        ) {
            strategy.htmlInput.dispatchEvent(nativeInput(type, data, text, start, end, missingRange, elementRange))
        }
        fun assertValue(expected: String, selection: Int) {
            assertEquals(expected, value.text)
            assertEquals(expected, strategy.htmlInput.textContent)
            assertEquals(TextRange(selection), value.selection)
            assertTrue(strategy.htmlInput.isConnected)
            assertEquals(selection, nativeSelectionFocusOffset())
        }
    }
}

internal suspend fun awaitNativeInputFrame() = withContext(Dispatchers.Default) {
    suspendCancellableCoroutine { continuation ->
        window.requestAnimationFrame { continuation.resume(Unit) }
    }
}

// Browser-native event/range objects with deterministic captured ranges. This is
// event-queue protocol coverage; trusted OS key/IME behavior is verified separately.
private fun nativeInput(
    type: String, data: String?, text: String, start: Int, end: Int,
    missingRange: Boolean, elementRange: Boolean,
): InputEvent = js("""{
    const event = new InputEvent('beforeinput', {inputType:type, data:data, bubbles:true});
    const node = document.createTextNode(text);
    const element = document.createElement('div');
    element.appendChild(node);
    const container = elementRange ? element : node;
    const range = new StaticRange({startContainer:container, startOffset:start, endContainer:container, endOffset:end});
    Object.defineProperty(event, 'getTargetRanges', {value:() => missingRange ? [] : [range]});
    return event;
}""")

private fun nativeSelectionFocusOffset(): Int = js("window.getSelection().focusOffset")

private fun nativeKey(key: String, control: Boolean): KeyboardEvent = js("""{
    const keyCode = key === 'Delete' ? 46 : key === 'Backspace' ? 8 : key.toUpperCase().charCodeAt(0);
    return new KeyboardEvent('keydown', {key:key, code:key, ctrlKey:control, keyCode:keyCode, which:keyCode});
}""")
