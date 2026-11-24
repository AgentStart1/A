/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package device_based

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.components.FontView
import com.storyteller_f.alphaheart.app.fonts.CommunityFontStore
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontContent
import com.storyteller_f.alphaheart.app.pages.community.CommunityTopicContent
import com.storyteller_f.alphaheart.client.compose_core.theme.LocalTopicContentFonts
import com.storyteller_f.alphaheart.client.compose_core.theme.TopicContentFonts
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.FontSettings
import com.storyteller_f.shared.model.FontSettingsWithInfo
import fonts.CommunityFontFixture
import fonts.CommunityFontTestData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CommunityFontFallbackBrowserTest {
    private val fontFixture = CommunityFontFixture()

    @AfterTest
    fun cleanFontFiles() = runTest { fontFixture.close() }

    @Test
    fun unavailableCommunityFontsShowFileNameAndKeepDefaultTopicFonts() =
        runAppComposeUiTest {
            val file = FileInfo.EMPTY.copy(id = 1L, name = "Regular.ttf")
            var topicFonts: TopicContentFonts? = null
            setContent {
                MaterialTheme {
                    CommunityTopicContent(communityId = 1L) {
                        val fonts = LocalTopicContentFonts.current
                        SideEffect { topicFonts = fonts }
                        FontView(file)
                    }
                }
            }
            onNodeWithText(file.name).assertExists()
            onNodeWithTag("font-status-slot").assertDoesNotExist()
            runOnIdle { assertEquals(TopicContentFonts(), topicFonts) }
        }

    @Test
    fun communityProviderPreservesTheApplicationCjkFallback() =
        runAppComposeUiTest {
            val fontScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            try {
                val (appResolver, appCjkFamily) = createApplicationCjkResolver()
                val expectedCjkAdvance = measure(appResolver, appCjkFamily, "中")
                assertTrue(expectedCjkAdvance > 0, "Synthetic application CJK font must render its mapped glyph")

                val communityStore = createCommunityFontStore(fontScope)
                val fontFile = FileInfo.EMPTY.copy(id = 1L, name = "Regular.ttf", url = "https://fonts.invalid/1")
                val settings =
                    FontSettingsWithInfo(
                        settings = FontSettings(contentFontId = fontFile.id),
                        contentFont = fontFile,
                    )
                var rootResolver: FontFamily.Resolver? = null
                var topicResolver: FontFamily.Resolver? = null
                var topicFonts = TopicContentFonts()

                setContent {
                    MaterialTheme {
                        CompositionLocalProvider(LocalFontFamilyResolver provides appResolver) {
                            val parentResolver = LocalFontFamilyResolver.current
                            SideEffect { rootResolver = parentResolver }
                            CommunityFontContent(settings, communityStore) {
                                val resolver = LocalFontFamilyResolver.current
                                val fonts = LocalTopicContentFonts.current
                                SideEffect {
                                    topicResolver = resolver
                                    topicFonts = fonts
                                }
                            }
                        }
                    }
                }
                awaitExternalCondition(timeoutMillis = 3_000) { topicFonts.content != null }

                val actualTopicResolver = assertNotNull(topicResolver)
                val contentFont = assertNotNull(topicFonts.content)
                val topicCjkAdvance = measure(actualTopicResolver, contentFont, "中")
                val topicLatinAdvance = measure(actualTopicResolver, contentFont, "A")

                runOnIdle {
                    assertCommunityFontFallback(
                        appResolver = appResolver,
                        rootResolver = rootResolver,
                        topicResolver = actualTopicResolver,
                        expectedCjkAdvance = expectedCjkAdvance,
                        topicCjkAdvance = topicCjkAdvance,
                        topicLatinAdvance = topicLatinAdvance,
                    )
                }
            } finally {
                fontScope.cancel()
            }
        }

    private fun measure(resolver: FontFamily.Resolver, family: FontFamily, text: String): Int =
        TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr).measure(
            AnnotatedString(text),
            style = TextStyle(fontFamily = family, fontSize = 100.sp),
            softWrap = false,
        ).size.width

    private suspend fun createApplicationCjkResolver(): Pair<FontFamily.Resolver, FontFamily> {
        val appCjk = fontFixture.testFontBundle("app-cjk", "Regular.ttf", CJK_FALLBACK_FONT)
        val appResolver = createFontFamilyResolver()
        val appCjkFamily = assertNotNull(appCjk.loaded.family)
        appResolver.preload(appCjkFamily)
        return appResolver to appCjkFamily
    }

    private fun createCommunityFontStore(scope: CoroutineScope): CommunityFontStore =
        fontFixture.createBundleStore(scope) { file ->
            fontFixture.testFontBundle(
                "community-latin:${file.id}",
                file.name,
                CommunityFontTestData.primary,
            )
        }

    private fun assertCommunityFontFallback(
        appResolver: FontFamily.Resolver,
        rootResolver: FontFamily.Resolver?,
        topicResolver: FontFamily.Resolver,
        expectedCjkAdvance: Int,
        topicCjkAdvance: Int,
        topicLatinAdvance: Int,
    ) {
        assertSame(
            appResolver,
            rootResolver,
            "Community font composition must not replace the parent resolver",
        )
        assertEquals(
            expectedCjkAdvance,
            topicCjkAdvance,
            "Community content resolver lost the synthetic application CJK fallback",
        )
        assertEquals(30, topicLatinAdvance, "The selected Latin community font must remain active")
        assertSame(
            appResolver,
            topicResolver,
            "Topic content should inherit the application resolver",
        )
    }

    private companion object {
        // Synthetic test font derived from CommunityFontTestData.primary with its sole glyph mapped to U+4E2D.
        // The asset has valid sfnt checksums and the same normal-weight metadata as the community fixture.
        private const val CJK_FALLBACK_FONT_BASE64 =
            "AAEAAAAKAIAAAwAgT1MvMkUhQ3sAAAEoAAAAYGNtYXAADE6AAAABkAAAADRnbHlmfTC1kwAAAcwAAAA0aGVhZFjv" +
                "pJwAAACsAAAANmhoZWEEigDzAAAA5AAAACRobXR4ArwAAAAAAYgAAAAIbG9jYQAaAA0AAAHEAAAABm1heHAABAAG" +
                "AAABCAAAACBuYW1lGXBdpwAAAgAAAAGAcG9zdDU3A3UAAAOAAAAAKgABAAAAAQAAwuX9sV8PPPUAAQPoAAAAAHwl" +
                "sIAAAAAAfCWwgAAUAAABfAK8AAAAAwACAAAAAAAAAAEAAAMg/zgAAAGQAAAAKAFoAAEAAAAAAAAAAAAAAAAAAAAC" +
                "AAEAAAACAAQAAQAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAwFeAZAABQAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAPz8/PwBAAEEAQQMg/zgAAAMgAMgAAAAAAAAAAAAAAAAAAAAgAAABkAAA" +
                "ASwAAAAAAAIAAAADAAAAFAADAAEAAAAUAAQAIAAAAAQABAABAABOLf//AABOLf//sdQAAQAAAAAAAAANABoAAAAB" +
                "ABQAAAF8ArwAAwAAMyERIRQBaP6YArwAAAEAFAAAARgCvAADAAAzIREhFAEE/vwCvAAAAAAOAK4AAQAAAAAAAQAQ" +
                "AAAAAQAAAAAAAgAHABAAAQAAAAAAAwAXABcAAQAAAAAABAAXABcAAQAAAAAABgAYAC4AAQAAAAAAEAAQAAAAAQAA" +
                "AAAAEQAHABAAAwABBAkAAQAgAEYAAwABBAkAAgAOAGYAAwABBAkAAwAuAHQAAwABBAkABAAuAHQAAwABBAkABgAw" +
                "AKIAAwABBAkAEAAgAEYAAwABBAkAEQAOAGZTeW50aGV0aWNQcmltYXJ5UmVndWxhclN5bnRoZXRpY1ByaW1hcnlS" +
                "ZWd1bGFyU3ludGhldGljUHJpbWFyeS1SZWd1bGFyAFMAeQBuAHQAaABlAHQAaQBjAFAAcgBpAG0AYQByAHkAUgBl" +
                "AGcAdQBsAGEAcgBTAHkAbgB0AGgAZQB0AGkAYwBQAHIAaQBtAGEAcgB5AFIAZQBnAHUAbABhAHIAUwB5AG4AdABo" +
                "AGUAdABpAGMAUAByAGkAbQBhAHIAeQAtAFIAZQBnAHUAbABhAHIAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" +
                "AAAAAAACAAABAgN1NDEAAA=="

        private val CJK_FALLBACK_FONT: ByteArray
            get() = kotlin.io.encoding.Base64.decode(CJK_FALLBACK_FONT_BASE64)
    }
}
