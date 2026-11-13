/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.PlatformHeadlessTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AndroidCommunityFontTest : PlatformHeadlessTest() {
    @Test
    fun localFileFontPreservesNativeGlyphMetrics() =
        runTest {
            val fixture = CommunityFontFixture()
            try {
                val bundle =
                    withContext(Dispatchers.Default) {
                        fixture.testFontBundle("android", "Regular.ttf", CommunityFontTestData.primary)
                    }
                val family = assertNotNull(bundle.loaded.family)
                val resolver = assertNotNull(bundle.loaded.resolver)
                val measured =
                    TextMeasurer(resolver, Density(1f), LayoutDirection.Ltr).measure(
                        AnnotatedString("A"),
                        TextStyle(fontFamily = family, fontSize = 100.sp),
                    )
                assertEquals(30, measured.size.width)
            } finally {
                fixture.close()
            }
        }

    @Test
    fun invalidLocalFileDoesNotPublishAFamily() =
        runTest {
            val fixture = CommunityFontFixture()
            try {
                val bundle = fixture.testFontBundle("invalid", "Regular.ttf", byteArrayOf(1, 2, 3))
                assertNull(bundle.loaded.family)
            } finally {
                fixture.close()
            }
        }

    @Test
    fun duplicateNativeStylesFailBeforePublication() =
        runTest {
            val bytes =
                fontTestArchive(
                    "one-Regular.ttf" to CommunityFontTestData.primary,
                    "two-Regular.ttf" to CommunityFontTestData.primary,
                )
            val failure = assertFailsWith<IllegalStateException> { downloadValidationFont(bytes, "family.zip") }
            assertEquals("Duplicate font weight and style", failure.message)
        }

    @Test
    fun nativeMetadataCannotBeOverriddenByTheFilename() =
        runTest {
            val failure =
                assertFailsWith<IllegalStateException> {
                    downloadValidationFont(CommunityFontTestData.primary, "Bold.ttf")
                }
            assertEquals("Internal font weight or style does not match the filename", failure.message)
        }

    @Test
    fun nativeFacesWithDifferentFamiliesFailTogether() =
        runTest {
            val bytes =
                fontTestArchive(
                    "Regular.ttf" to CommunityFontTestData.code,
                    "Bold.ttf" to CommunityFontTestData.primaryBold,
                )
            val failure = assertFailsWith<IllegalStateException> { downloadValidationFont(bytes, "family.zip") }
            assertEquals("Font faces have different internal families", failure.message)
        }

    @Test
    fun nativeFourStyleFamilyCompletesPreloading() =
        runTest {
            val bytes = fontTestZip("Regular.ttf", "Bold.ttf", "Italic.ttf", "BoldItalic.ttf")
            val bundle = downloadValidationFont(bytes, "family.zip")
            assertEquals(4, bundle.faces.size)
            assertNotNull(bundle.loaded.family)
            assertNotNull(bundle.loaded.resolver)
        }
}
