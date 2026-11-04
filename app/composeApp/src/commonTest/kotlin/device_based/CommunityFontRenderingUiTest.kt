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
import com.storyteller_f.a.app.pages.community.CommunityFontPresentation
import com.storyteller_f.a.app.pages.community.buildCommunityFonts
import com.storyteller_f.a.app.pages.community.decodeCommunityFont
import fonts.CommunityFontTestData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class CommunityFontRenderingUiTest {
    @Test
    fun absentFontsKeepTheApplicationDefaults() =
        runComposeUiTest {
            val fonts = buildCommunityFonts(null, null)
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
                    com.storyteller_f.a.client.compose_core.components.TopicContentFonts(FontFamily.Default),
                    root,
                )
            val before = width(application, "A", root)
            val left = buildCommunityFonts(primary(), null)
            val right = buildCommunityFonts(code(), null)
            assertEquals(30, width(left, "A", root))
            assertEquals(60, width(right, "A", root))
            assertEquals(before, width(application, "A", root))
        }

    @Test
    fun keepsBodyAndCodeFamiliesIndependent() =
        runComposeUiTest {
            val root = rootResolver()
            val fonts = buildCommunityFonts(primary(), code())
            assertEquals(30, width(fonts, "A", root))
            val resolver = fonts.resolver ?: root
            assertEquals(60, measure(resolver, fonts.fonts.code, "A"))
            assertEquals(60, measure(resolver, fonts.fonts.editor, "A"))
        }

    @Test
    fun invalidBodyFontKeepsCodeAndApplicationDefaults() =
        runComposeUiTest {
            val root = rootResolver()
            val fonts = buildCommunityFonts(invalid(), code())
            assertNull(fonts.fonts.content)
            val resolver = fonts.resolver ?: root
            assertEquals(60, measure(resolver, fonts.fonts.code, "A"))
            assertEquals(60, measure(resolver, fonts.fonts.editor, "A"))
        }

    @Test
    fun invalidCodeFontKeepsBodyAndUsesItInTheEditor() =
        runComposeUiTest {
            val root = rootResolver()
            val fonts = buildCommunityFonts(primary(), invalid())
            assertNull(fonts.fonts.code)
            assertEquals(30, width(fonts, "A", root))
            assertEquals(30, measure(fonts.resolver ?: root, fonts.fonts.editor, "A"))
        }

    private fun primary() = decodeCommunityFont("primary", "Regular.ttf", CommunityFontTestData.primary)
    private fun code() = decodeCommunityFont("code", "Regular.ttf", CommunityFontTestData.code)
    private fun invalid() = decodeCommunityFont("invalid", "Regular.ttf", byteArrayOf(1, 2, 3))

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
