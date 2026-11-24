/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.copied
import com.storyteller_f.alphaheart.client.compose_core.copy
import com.storyteller_f.alphaheart.client.compose_core.copy_failed
import com.storyteller_f.alphaheart.client.compose_core.file_copy_address
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlActions
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlState
import com.storyteller_f.alphaheart.client.compose_core.media.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.media_options
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.notifications.Toast
import com.storyteller_f.alphaheart.client.compose_core.utils.writeBrowserClipboardText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import org.jetbrains.compose.resources.getString
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingHorizontalPager as HorizontalPager

@OptIn(ExperimentalTestApi::class)
internal class BrowserClipboardTest {
    @Test
    fun detailFieldCopyPreservesTheNarrowPagerPosition() =
        runComposeUiTest {
            val restore = installClipboard(false)
            val text = "synthetic-public-id"
            val copy = getString(Res.string.copy)
            var pager: PagerState? = null
            try {
                setContent {
                    MaterialTheme {
                        Box(Modifier.size(390.dp, 600.dp)) {
                            val state = rememberPagerState { 2 }
                            pager = state
                            HorizontalPager(state, Modifier.fillMaxSize().testTag("detail-pager")) { page ->
                                if (page == 0) {
                                    InfoTable(listOf("ID" to text, "Name" to "Synthetic"))
                                } else {
                                    Text("Other page")
                                }
                            }
                        }
                    }
                }
                onNodeWithText(text).assertIsDisplayed()
                val before = onNodeWithText(text).fetchSemanticsNode().boundsInRoot
                onNodeWithTag("detail-pager").performSemanticsAction(SemanticsActions.ScrollBy) { it(162f, 0f) }
                awaitIdle()
                assertEquals(before, onNodeWithText(text).fetchSemanticsNode().boundsInRoot)
                onAllNodesWithContentDescription(copy)[0].performClick()
                waitUntil { clipboardAttempts() == 1 }
                awaitIdle()
                assertEquals(text, clipboardText())
                assertEquals(before, onNodeWithText(text).fetchSemanticsNode().boundsInRoot)
                onNodeWithText(text).assertIsDisplayed()
                runOnIdle {
                    assertEquals(0, checkNotNull(pager).currentPage)
                    assertEquals(0f, checkNotNull(pager).currentPageOffsetFraction)
                }
            } finally {
                restoreClipboard(restore)
            }
        }

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
                onNodeWithContentDescription(copy).performClick()
                waitUntil { clipboardAttempts() == 1 }
                awaitIdle()
                assertEquals(listOf(failed), messages)
                assertEquals(1, clipboardAttempts())
                allowClipboardWrites()
                onNodeWithContentDescription(options).performClick()
                onNodeWithContentDescription(copy).performClick()
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
    fun filePreviewCopiesTheExactResourceAddress() =
        runComposeUiTest {
            val restore = installClipboard(false)
            holdClipboardWrites()
            val url = "https://files.invalid/document.pdf?download=完整值"
            try {
                setContent {
                    MaterialTheme {
                        ReadingFilePreviewHeading(ResourceDownloadRequest(url, "document.pdf")) {}
                    }
                }
                onNodeWithContentDescription(getString(Res.string.file_copy_address)).performClick()
                waitUntil { clipboardAttempts() == 1 }
                resolvePendingClipboardWrite().await()
                assertEquals(url, clipboardText())
            } finally {
                restoreClipboard(restore)
            }
        }

    @Test
    fun standaloneMediaCopyKeepsMenuUntilTheBrowserWriteCompletes() =
        runComposeUiTest {
            val restore = installClipboard(false)
            holdClipboardWrites()
            val messages = mutableListOf<String>()
            val toast =
                object : Toast {
                    override fun showMessage(message: String) {
                        messages.add(message)
                    }
                }
            val options = getString(Res.string.media_options)
            val copy = getString(Res.string.copy)
            val copied = getString(Res.string.copied)
            val text = "https://example.invalid/clip.mp4?name=完整值😀"
            var stage = "admit browser clipboard write"
            try {
                setContent {
                    CompositionLocalProvider(LocalToaster provides toast) {
                        MediaMenuScope(showDownload = false) {
                            MediaPlayerSurface(
                                "Track",
                                MediaControlState(),
                                MediaControlActions({}, {}, {}, {}),
                                Modifier.size(360.dp, 203.dp),
                                sourceUrl = text,
                            ) {}
                        }
                    }
                }
                onNodeWithContentDescription(options).performClick()
                onNodeWithContentDescription(copy).performClick()
                waitUntil { clipboardAttempts() == 1 }
                awaitIdle()
                onNodeWithContentDescription(copy).assertIsDisplayed()
                assertEquals(emptyList(), messages)
                stage = "resume resolved browser write and publish feedback"
                resolvePendingClipboardWrite().await()
                // Await the actual native write; an already-idle Compose scene need not yield to JS.
                awaitIdle()
                waitUntil { messages.size == 1 }
                assertEquals(text, clipboardText())
                assertEquals(listOf(copied), messages)
                assertEquals(1, clipboardAttempts())
                onNodeWithContentDescription(copy).assertDoesNotExist()
            } catch (failure: ComposeTimeoutException) {
                throw IllegalStateException(
                    "Clipboard stage: $stage; attempts=${clipboardAttempts()}; feedback=$messages",
                    failure,
                )
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
                onNodeWithContentDescription(copy).performClick()
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
        if (this.hold) {
            this.pending = new Promise((resolve, reject) => {
                this.rejectPending = reject;
                this.resolvePending = () => { this.text = text; resolve(); };
            });
            return this.pending;
        }
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

@JsFun("() => { navigator.clipboard.resolvePending(); return navigator.clipboard.pending; }")
private external fun resolvePendingClipboardWrite(): Promise<JsAny?>
