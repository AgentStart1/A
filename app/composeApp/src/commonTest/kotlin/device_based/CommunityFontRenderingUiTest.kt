/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontPresentation
import fonts.CommunityFontFixture
import fonts.CommunityFontTestData
import fonts.buildTestCommunityFonts
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class CommunityFontRenderingUiTest {
    private val fontFixture = CommunityFontFixture()

    @AfterTest
    fun cleanFontFiles() = runTest { fontFixture.close() }

    @Test
    fun absentFontsKeepTheApplicationDefaults() =
        runComposeUiTest {
            val fonts = buildTestCommunityFonts(null, null)
            assertNull(fonts.fonts.content)
            assertNull(fonts.fonts.code)
            assertNull(fonts.fonts.editor)
        }

    @Test
    fun isolatesCommunityFontsFromTheApplicationResolver() =
        runComposeUiTest {
            val root = rootResolver()
            val application =
                CommunityFontPresentation(
                    com.storyteller_f.alphaheart.client.compose_core.components.TopicContentFonts(FontFamily.Default),
                    root,
                )
            val before = width(application, "A", root)
            val left = buildTestCommunityFonts(primary(), null)
            val right = buildTestCommunityFonts(code(), null)
            assertEquals(30, width(left, "A", root))
            assertEquals(60, width(right, "A", root))
            assertEquals(before, width(application, "A", root))
        }

    @Test
    fun keepsBodyAndCodeFamiliesIndependent() =
        runComposeUiTest {
            val root = rootResolver()
            val fonts = buildTestCommunityFonts(primary(), code())
            assertEquals(30, width(fonts, "A", root))
            val resolver = fonts.resolver ?: root
            assertEquals(60, measure(resolver, fonts.fonts.code, "A"))
            assertEquals(60, measure(resolver, fonts.fonts.editor, "A"))
        }

    @Test
    fun invalidBodyFontKeepsCodeAndApplicationDefaults() =
        runComposeUiTest {
            val root = rootResolver()
            val fonts = buildTestCommunityFonts(invalid(), code())
            assertNull(fonts.fonts.content)
            val resolver = fonts.resolver ?: root
            assertEquals(60, measure(resolver, fonts.fonts.code, "A"))
            assertEquals(60, measure(resolver, fonts.fonts.editor, "A"))
        }

    @Test
    fun invalidCodeFontKeepsBodyAndUsesItInTheEditor() =
        runComposeUiTest {
            val root = rootResolver()
            val fonts = buildTestCommunityFonts(primary(), invalid())
            assertNull(fonts.fonts.code)
            assertEquals(30, width(fonts, "A", root))
            assertEquals(30, measure(fonts.resolver ?: root, fonts.fonts.editor, "A"))
        }

    private suspend fun primary() = fontFixture.testFontBundle("primary", "Regular.ttf", CommunityFontTestData.primary)
    private suspend fun code() = fontFixture.testFontBundle("code", "Regular.ttf", CommunityFontTestData.code)
    private suspend fun invalid() = fontFixture.testFontBundle("invalid", "Regular.ttf", byteArrayOf(1, 2, 3))

    private fun width(fonts: CommunityFontPresentation, text: String, root: FontFamily.Resolver): Int =
        measure(fonts.resolver ?: root, fonts.fonts.content, text)

    private fun ComposeUiTest.rootResolver(): FontFamily.Resolver {
        var resolver: FontFamily.Resolver? = null
        setContent {
            MaterialTheme {
                val current = LocalFontFamilyResolver.current
                SideEffect { resolver = current }
            }
        }
        waitForIdle()
        return assertNotNull(resolver)
    }

    private fun measure(resolver: FontFamily.Resolver, family: FontFamily?, text: String): Int =
        TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr).measure(
            AnnotatedString(text),
            style = TextStyle(fontFamily = family, fontSize = 100.sp),
            softWrap = false,
        ).size.width
}
