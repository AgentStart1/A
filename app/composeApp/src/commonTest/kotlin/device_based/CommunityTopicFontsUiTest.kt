/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.markdownColor
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontContent
import com.storyteller_f.alphaheart.app.pages.community.fontFileKey
import com.storyteller_f.alphaheart.app.pages.topic.EditTopicPage
import com.storyteller_f.alphaheart.client.compose_core.components.LocalTopicContentFonts
import com.storyteller_f.alphaheart.client.compose_core.components.TopicContentFonts
import com.storyteller_f.alphaheart.client.compose_core.components.customMarkdownTypography
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FontSettings
import com.storyteller_f.shared.model.FontSettingsWithInfo
import fonts.CommunityFontFixture
import fonts.CommunityFontTestData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalTestApi::class)
class CommunityTopicFontsUiTest {
    private val fontFixture = CommunityFontFixture()

    @AfterTest
    fun cleanFontFiles() = runTest { fontFixture.close() }

    @Test
    fun markdownFontsDoNotChangeTheApplicationTheme() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    val original = MaterialTheme.typography
                    CompositionLocalProvider(
                        LocalTopicContentFonts provides TopicContentFonts(FontFamily.Serif, FontFamily.Cursive),
                    ) {
                        val markdown = customMarkdownTypography(markdownColor())
                        val unchanged = MaterialTheme.typography
                        SideEffect {
                            assertSame(original, unchanged)
                            listOf(markdown.text, markdown.h1, markdown.h6, markdown.quote, markdown.table).forEach {
                                assertEquals(FontFamily.Serif, it.fontFamily)
                            }
                            assertEquals(FontFamily.Cursive, markdown.code.fontFamily)
                            assertEquals(FontFamily.Cursive, markdown.inlineCode.fontFamily)
                        }
                    }
                }
            }
            waitForIdle()
        }

    @Test
    fun fontCompletionPreservesTheSourceDraftAndCursor() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val release = CompletableDeferred<Unit>()
                val store =
                    fontFixture.createFontStore(scope) {
                        release.await()
                        CommunityFontTestData.primary
                    }
                val input = mutableStateOf("AAA")
                var family: FontFamily? = null
                setContent {
                    MaterialTheme {
                        CommunityFontContent(settings(1), store) {
                            val current = LocalTopicContentFonts.current.content
                            SideEffect { family = current }
                            EditTopicPage(input.value) { input.value = it }
                        }
                    }
                }
                onNode(hasSetTextAction()).performTextReplacement("AAA draft")
                runOnIdle {
                    assertNull(family)
                    release.complete(Unit)
                }
                waitUntil { family != null }
                onNode(hasSetTextAction()).performTextInput("!")
                runOnIdle { assertEquals("AAA draft!", input.value) }
            } finally {
                scope.cancel()
            }
        }

    @Test
    fun clearsPreviousFontWhileLoadingNewSettings() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val second = CompletableDeferred<Unit>()
                val store =
                    fontFixture.createFontStore(scope) {
                        if (it.id == 1L) {
                            CommunityFontTestData.primary
                        } else {
                            second.await()
                            CommunityFontTestData.code
                        }
                    }
                val selected = mutableStateOf(settings(1))
                val input = mutableStateOf("AAA draft")
                var family: FontFamily? = null
                setContent {
                    MaterialTheme {
                        CommunityFontContent(selected.value, store) {
                            val current = LocalTopicContentFonts.current.content
                            SideEffect { family = current }
                            EditTopicPage(input.value) { input.value = it }
                        }
                    }
                }
                waitUntil { family != null }
                val previous = assertNotNull(family)
                runOnIdle { selected.value = settings(2) }
                waitUntil { family == null }
                runOnIdle { second.complete(Unit) }
                waitUntil { family != null }
                runOnIdle {
                    assertNotSame(previous, family)
                    assertEquals("AAA draft", input.value)
                }
            } finally {
                scope.cancel()
            }
        }

    @Test
    fun visibleFontsShareTheirConstructionFlows() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val reads = MutableStateFlow(emptyMap<Long, Int>())
                val store =
                    fontFixture.createBundleStore(scope) { file ->
                        reads.update { counts -> counts + (file.id to counts.getOrElse(file.id) { 0 } + 1) }
                        val bytes = if (file.id == 1L) CommunityFontTestData.primary else CommunityFontTestData.code
                        fontFixture.testFontBundle(fontFileKey(file), file.name, bytes)
                    }
                val selected = settings(1).copy(settings = FontSettings(1, 2), codeFont = settings(2).contentFont)
                var fonts = TopicContentFonts()
                var resolver: FontFamily.Resolver? = null
                setContent {
                    MaterialTheme {
                        CommunityFontContent(selected, store) {
                            val current = LocalTopicContentFonts.current
                            val currentResolver = LocalFontFamilyResolver.current
                            SideEffect {
                                fonts = current
                                resolver = currentResolver
                            }
                        }
                    }
                }
                waitUntil { fonts.content != null && fonts.code != null }
                waitForIdle()
                runOnIdle {
                    assertEquals(mapOf(1L to 1, 2L to 1), reads.value)
                    assertEquals(30, measure(assertNotNull(resolver), fonts.content))
                    assertEquals(60, measure(assertNotNull(resolver), fonts.code))
                }
            } finally {
                scope.cancel()
            }
        }

    @Test
    fun bodyRetryRefreshesOnlyTheMatchingFont() =
        runComposeUiTest {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val reads = MutableStateFlow(emptyMap<Long, Int>())
                val store =
                    fontFixture.createFontStore(scope) { file ->
                        val counts = reads.updateAndGet { it + (file.id to it.getOrElse(file.id) { 0 } + 1) }
                        if (file.id == 1L && counts[file.id] == 1) error("Synthetic initial font failure")
                        if (file.id == 1L) CommunityFontTestData.primary else CommunityFontTestData.code
                    }
                val selected = settings(1).copy(settings = FontSettings(1, 2), codeFont = settings(2).contentFont)
                var fonts = TopicContentFonts()
                setContent {
                    MaterialTheme {
                        CommunityFontContent(selected, store) {
                            val current = LocalTopicContentFonts.current
                            SideEffect { fonts = current }
                        }
                    }
                }
                waitUntil { reads.value[1] == 1 && fonts.code != null }
                val previousCode = assertNotNull(fonts.code)
                runOnIdle {
                    assertNull(fonts.content)
                    scope.launch { store.retry(assertNotNull(selected.contentFont)) }
                }
                waitUntil { fonts.content != null }
                runOnIdle {
                    assertSame(previousCode, fonts.code)
                    assertEquals(mapOf(1L to 2, 2L to 1), reads.value)
                }
            } finally {
                scope.cancel()
            }
        }

    private fun measure(resolver: FontFamily.Resolver, family: FontFamily?): Int =
        TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr).measure(
            AnnotatedString("A"),
            TextStyle(fontFamily = family, fontSize = 100.sp),
            softWrap = false,
        ).size.width

    private fun settings(id: Long): FontSettingsWithInfo =
        FontSettingsWithInfo(
            settings = FontSettings(contentFontId = id),
            contentFont = FileInfo.EMPTY.copy(id = id, name = "Regular.ttf", url = "https://fonts.invalid/$id"),
        )
}
