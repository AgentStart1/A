/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.copied
import com.storyteller_f.alphaheart.client.compose_core.copy
import com.storyteller_f.alphaheart.client.compose_core.copy_failed
import com.storyteller_f.alphaheart.client.compose_core.media_options
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import kotlin.js.JsAny
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class BrowserClipboardTest {
    @Test
    fun mountedPlayerCanCopyAfterPermissionIsRestored() =
        runComposeUiTest {
            val restore = installClipboard(true)
            val messages = mutableListOf<String>()
            val toast =
                object : Toast {
                    override fun showMessage(message: String) {
                        messages.add(message)
                    }
                }
            var mounts = 0
            var disposals = 0
            val options = getString(Res.string.media_options)
            val copy = getString(Res.string.copy)
            val failed = getString(Res.string.copy_failed)
            val copied = getString(Res.string.copied)
            val text = "https://example.com/media?q=中文&name=one\ntwo"
            try {
                setContent {
                    CompositionLocalProvider(LocalToaster provides toast) {
                        MediaPlayerSurface(
                            "Track",
                            MediaControlState(),
                            MediaControlActions({}, {}, {}, {}),
                            Modifier.size(360.dp, 203.dp),
                            sourceUrl = text,
                        ) {
                            DisposableEffect(Unit) {
                                mounts++
                                onDispose { disposals++ }
                            }
                        }
                    }
                }
                onNodeWithContentDescription(options).performClick()
                onNodeWithText(copy).performClick()
                waitUntil { clipboardAttempts() == 1 }
                awaitIdle()
                assertEquals(listOf(failed), messages)
                assertEquals(1, clipboardAttempts())
                allowClipboardWrites()
                onNodeWithContentDescription(options).performClick()
                onNodeWithText(copy).performClick()
                waitUntil { clipboardAttempts() == 2 }
                awaitIdle()
                waitUntil { messages.size == 2 }
                assertEquals(text, clipboardText())
                assertEquals(listOf(failed, copied), messages)
                assertEquals(1, mounts)
                assertEquals(0, disposals)
            } finally {
                restoreClipboard(restore)
            }
        }

    @Test
    fun disposingPlayerCancelsPendingCopyWithoutFeedback() =
        runComposeUiTest {
            val restore = installClipboard(false)
            holdClipboardWrites()
            val mounted = mutableStateOf(true)
            val messages = mutableListOf<String>()
            val toast =
                object : Toast {
                    override fun showMessage(message: String) {
                        messages.add(message)
                    }
                }
            val options = getString(Res.string.media_options)
            val copy = getString(Res.string.copy)
            try {
                setContent {
                    CompositionLocalProvider(LocalToaster provides toast) {
                        if (mounted.value) {
                            MediaPlayerSurface(
                                "Track",
                                MediaControlState(),
                                MediaControlActions({}, {}, {}, {}),
                                Modifier.size(360.dp, 203.dp),
                                sourceUrl = "synthetic media URL",
                            ) {}
                        }
                    }
                }
                onNodeWithContentDescription(options).performClick()
                onNodeWithText(copy).performClick()
                waitUntil { clipboardAttempts() == 1 }
                runOnIdle { mounted.value = false }
                awaitIdle()
                rejectPendingClipboardWrite()
                awaitIdle()
                assertEquals(emptyList(), messages)
            } finally {
                restoreClipboard(restore)
            }
        }

    @Test
    fun pendingBrowserWritePreservesCancellation() =
        runTest {
            val restore = installClipboard(false)
            holdClipboardWrites()
            try {
                val copying =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        writeBrowserClipboardText("synthetic media URL")
                    }
                copying.cancel(CancellationException("Copy dismissed"))
                val cancellation = assertFailsWith<CancellationException> { copying.await() }
                assertEquals("Copy dismissed", cancellation.message)
                assertEquals(1, clipboardAttempts())
                rejectPendingClipboardWrite()
            } finally {
                restoreClipboard(restore)
            }
        }

    @Test
    fun writesExactTextThroughBrowserClipboard() =
        runTest {
            val restore = installClipboard(false)
            try {
                val text = "https://example.com/media?q=中文&name=one\ntwo"
                writeBrowserClipboardText(text)
                assertEquals(text, clipboardText())
            } finally {
                restoreClipboard(restore)
            }
        }

    @Test
    fun normalizesSynchronousBrowserFailure() =
        runTest {
            val restore = installClipboard(false)
            throwClipboardWrites()
            try {
                val failure =
                    assertFailsWith<IllegalStateException> {
                        writeBrowserClipboardText("synthetic media URL")
                    }
                assertEquals("Clipboard permission denied", failure.message)
            } finally {
                restoreClipboard(restore)
            }
        }

    @Test
    fun reportsRejectedClipboardWrite() =
        runTest {
            val restore = installClipboard(true)
            try {
                val failure =
                    assertFailsWith<Exception> {
                        writeBrowserClipboardText("synthetic media URL")
                    }
                assertTrue(failure.message.orEmpty().contains("Clipboard permission denied"))
            } finally {
                restoreClipboard(restore)
            }
        }
}

@JsFun(
    """(reject) => {
    const previous = Object.getOwnPropertyDescriptor(navigator, 'clipboard');
    const clipboard = { text: null, reject, attempts: 0, hold: false, writeText(text) {
        this.attempts++;
        if (this.throw) throw new DOMException('Clipboard permission denied', 'NotAllowedError');
        if (this.hold) return new Promise((resolve, reject) => { this.rejectPending = reject; });
        if (this.reject) return Promise.reject(new DOMException('Clipboard permission denied', 'NotAllowedError'));
        return Promise.resolve().then(() => { this.text = text; });
    }};
    Object.defineProperty(navigator, 'clipboard', { configurable: true, value: clipboard });
    return () => {
        if (previous) Object.defineProperty(navigator, 'clipboard', previous);
        else delete navigator.clipboard;
    };
}""",
)
private external fun installClipboard(reject: Boolean): JsAny

@JsFun("() => navigator.clipboard.text")
private external fun clipboardText(): String

@JsFun("(restore) => restore()")
private external fun restoreClipboard(restore: JsAny)

@JsFun("() => navigator.clipboard.attempts")
private external fun clipboardAttempts(): Int

@JsFun("() => { navigator.clipboard.reject = false; }")
private external fun allowClipboardWrites()

@JsFun("() => { navigator.clipboard.hold = true; }")
private external fun holdClipboardWrites()

@JsFun("() => navigator.clipboard.rejectPending(new DOMException('Clipboard permission denied', 'NotAllowedError'))")
private external fun rejectPendingClipboardWrite()

@JsFun("() => { navigator.clipboard.throw = true; }")
private external fun throwClipboardWrites()
